/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class EiaCategoryTypeHandler extends ExtensibleGroupTypeHandler {



    /** _more_ */
    public static final int IDX_CATEGORY_ID = 0;


    /** _more_ */
    private String apiKey;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public EiaCategoryTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getApiKey() {
        if (apiKey == null) {
            apiKey = getRepository().getProperty(Eia.PROP_API_KEY,
                    (String) null);
        }

        return apiKey;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return getApiKey() != null;
    }

    /**
     * _more_
     *
     * @param mainEntry _more_
     * @param type _more_
     * @param id _more_
     *
     * @return _more_
     */
    public String createSynthId(Entry mainEntry, String type, String id) {
        return getEntryManager().createSynthId(mainEntry, type + ":" + id);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry mainEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {

        if ((parentEntry != null)
                && !parentEntry.getTypeHandler().isType(Eia.TYPE_CATEGORY)) {
            return null;
        }

        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        /*
        System.err.println("EiaTypeHandler.getSynthIds:" + synthId
                           + " parent:" + parentEntry.getName()
                           + " mainEntry: " + mainEntry.getName());
        */
        String categoryId = null;

        if (Utils.stringDefined(synthId)) {
            List<String> toks = StringUtil.split(synthId, ":", true, true);
            if (toks.size() <= 1) {
                System.err.println("EiaTypeHandler.getSynthIds: bad id:"
                                   + synthId);

                return null;
            }
            if ( !toks.get(0).equals(Eia.PREFIX_CATEGORY)) {
                return null;
            }
            categoryId = toks.get(1);
        }


        Object[] categoryValues = null;
        if ((parentEntry != null)
                && parentEntry.getTypeHandler().isType(Eia.TYPE_CATEGORY)) {
            categoryValues =
                parentEntry.getTypeHandler().getEntryValues(mainEntry);
        }
        if (categoryValues == null) {
            categoryValues =
                mainEntry.getTypeHandler().getEntryValues(mainEntry);
        }

        if (categoryId == null) {
            categoryId = ((categoryValues != null)
                          ? (String) categoryValues[IDX_CATEGORY_ID]
                          : (String) null);
        }


        List<String> args = new ArrayList<String>();
        if (categoryId != null) {
            args.add(Eia.ARG_CATEGORY_ID);
            args.add(categoryId);
        }

        Element root     = call(mainEntry, Eia.URL_CATEGORY, args);
        Element catNode  = XmlUtil.findChild(root, Eia.TAG_CATEGORY);
        Element catsNode = XmlUtil.findChild(catNode,
                                             Eia.TAG_CHILDCATEGORIES);
        Element seriesNode = XmlUtil.findChild(catNode, Eia.TAG_CHILDSERIES);
        List<Entry> catEntries    = new ArrayList<Entry>();
        List<Entry> seriesEntries = new ArrayList<Entry>();
        if (catsNode != null) {
            NodeList children = XmlUtil.getElements(catsNode, Eia.TAG_ROW);
            for (int childIdx = 0; childIdx < children.getLength();
                    childIdx++) {
                Element item = (Element) children.item(childIdx);
                String id = XmlUtil.getGrandChildText(item,
                                Eia.TAG_CATEGORY_ID, "");
                String name = XmlUtil.getGrandChildText(item, Eia.TAG_NAME,
                                  "");
                Entry entry = createCategoryEntry(mainEntry, parentEntry, id,
                                  name);
                catEntries.add(entry);
            }
        }

        if (seriesNode != null) {
            NodeList children = XmlUtil.getElements(seriesNode, Eia.TAG_ROW);
            for (int childIdx = 0; childIdx < children.getLength();
                    childIdx++) {
                Element item = (Element) children.item(childIdx);
                String id = XmlUtil.getGrandChildText(item,
                                Eia.TAG_SERIES_ID, "");
                String name = XmlUtil.getGrandChildText(item, Eia.TAG_NAME,
                                  (String) null);
                Entry entry = createSeriesEntry(request,mainEntry, parentEntry, id,
                                  name);
                seriesEntries.add(entry);
            }
        }

        //Sort the entries
        catEntries =
            getEntryManager().getEntryUtil().sortEntriesOnName(catEntries,
                false);
        seriesEntries =
            getEntryManager().getEntryUtil().sortEntriesOnName(seriesEntries,
                false);
        ids = new ArrayList<String>();
        for (Entry child : catEntries) {
            ids.add(child.getId());
        }
        for (Entry child : seriesEntries) {
            ids.add(child.getId());
        }
        parentEntry.setChildIds(ids);

        return ids;

    }


    /** _more_ */
    HashSet<String> seenUrls = new HashSet<String>();

    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Element call(Entry entry,String url, List<String> args) throws Exception {
        long t1 = System.currentTimeMillis();
        url = makeUrl(url, args,"xml");
        if (seenUrls.contains(url)) {
            //            System.err.println("**** Eia URL:" + url);
        } else {
            //            System.err.println("Eia URL:" + url);
            seenUrls.add(url);
        }
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    System.err.println("Error making EIA call:" + entry.getName() +" " + entry.getId());
	    throw new RuntimeException("An error has occurred:" + result.getResult());
	}

        long   t2  = System.currentTimeMillis();
        //        System.err.println("\ttime:" + (t2 - t1));
        Element root = XmlUtil.getRoot(result.getResult());

        return root;
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeUrl(String url, List<String> args, String out) throws Exception {
        List<String> urlArgs = new ArrayList<String>();
        urlArgs.add(Eia.ARG_API_KEY);
        urlArgs.add(getApiKey());
        urlArgs.add(Eia.ARG_OUT);
        urlArgs.add(out);
        for (String arg : args) {
            urlArgs.add(arg);
        }

        return HtmlUtils.url(url, urlArgs);
    }




    /**
     * _more_
     *
     * @param mainEntry _more_
     * @param categoryEntry _more_
     * @param categoryId _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createCategoryEntry(Entry mainEntry, Entry categoryEntry,
                                      String categoryId, String name)
            throws Exception {
        if ( !isEnabled()) {
            return null;
        }
        if (name == null) {
            name = "Category";
        }
        Date   dttm  = new Date();
        String id = createSynthId(mainEntry, Eia.PREFIX_CATEGORY, categoryId);
        Entry  entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }


        String desc = "";
        entry = new Entry(id, this);
        String eiaUrl = "https://www.eia.gov/beta/api/qb.cfm?category="
                        + categoryId;
        Resource resource = new Resource(new URL(eiaUrl));
        Object[] values   = this.makeEntryValues(null);
        values[IDX_CATEGORY_ID] = categoryId;
        entry.initEntry(name, desc, categoryEntry, categoryEntry.getUser(),
                        resource, "", Entry.DEFAULT_ORDER, dttm.getTime(),
                        dttm.getTime(), dttm.getTime(), dttm.getTime(),
                        values);

        getEntryManager().cacheSynthEntry(entry);

        return entry;
    }


    /**
     * _more_
     *
     * @param mainEntry _more_
     * @param parentEntry _more_
     * @param seriesId _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createSeriesEntry(Request request,Entry mainEntry, Entry parentEntry,
                                    String seriesId, String name)
            throws Exception {

        if ( !Utils.stringDefined(name)) {
            name = seriesId;
        }

        String id    = createSynthId(mainEntry, Eia.PREFIX_SERIES, seriesId);
        Entry  entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }
        String desc = "";
        Date   dttm = new Date();
        EiaSeriesTypeHandler seriesTypeHandler =
            (EiaSeriesTypeHandler) getRepository().getTypeHandler(
                Eia.TYPE_SERIES);
        entry = new Entry(id, seriesTypeHandler);
        Object[] values = seriesTypeHandler.makeEntryValues(null);
        values[EiaSeriesTypeHandler.IDX_SERIES_ID] = seriesId;

        entry.initEntry(name, desc, parentEntry, parentEntry.getUser(),
                        new Resource(), "", Entry.DEFAULT_ORDER,
                        dttm.getTime(), dttm.getTime(), dttm.getTime(),
                        dttm.getTime(), values);
        seriesTypeHandler.initializeSeries(request,entry);
        getEntryManager().cacheSynthEntry(entry);

        return entry;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param mainEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry mainEntry, String id)
            throws Exception {
        Entry entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }

        List<String> toks = StringUtil.split(id, ":", true, true);
        if (toks.size() <= 1) {
            return null;
        }
        String type = toks.get(0);
        id = toks.get(1);
        //TODO: get the name
        if (type.equals(Eia.PREFIX_CATEGORY)) {
            return createCategoryEntry(mainEntry, mainEntry, id, null);
        } else {
            return createSeriesEntry(request,mainEntry, mainEntry, id, null);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        return true;
    }



}

/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.biz;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.HtmlUtils;
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
public class FredCategoryTypeHandler extends ExtensibleGroupTypeHandler {



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
    public FredCategoryTypeHandler(Repository repository, Element entryNode)
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
            apiKey = getRepository().getProperty(Fred.PROP_API_KEY,
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
    private String createSynthId(Entry mainEntry, String type, String id) {
        return getEntryManager().createSynthId(mainEntry, type + ":" + id);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        super.initializeEntryFromForm(request, entry, parent, newEntry);

        if ( !getEntryManager().isSynthEntry(entry.getId())) {
            return;
        }

        /*
        Object[] values = entry.getTypeHandler().getEntryValues(entry);
        values[IDX_TEAM_ID] = JsonUtil.readValue(result, "team.id", "");
        String domain = JsonUtil.readValue(result, "team.domain", "");
        values[IDX_TEAM_DOMAIN] = domain;
        entry.setResource(new Resource(Slack.getTeamUrl(domain)));
        */
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
                && !parentEntry.getTypeHandler().isType(Fred.TYPE_CATEGORY)) {
            return null;
        }

        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        //        System.err.println("FredTypeHandler.getSynthIds:" + synthId
        //                           + " parent:" + parentEntry.getName()
        //                           + " mainEntry: " + mainEntry.getName());
        String categoryId = null;

        if (Utils.stringDefined(synthId)) {
            List<String> toks = StringUtil.split(synthId, ":", true, true);
            if (toks.size() <= 1) {
                System.err.println("FredTypeHandler.getSynthIds: bad id:"
                                   + synthId);

                return null;
            }
            if ( !toks.get(0).equals(Fred.PREFIX_CATEGORY)) {
                return null;
            }
            categoryId = toks.get(1);
        }


        Object[] categoryValues = null;
        if ((parentEntry != null)
                && parentEntry.getTypeHandler().isType(Fred.TYPE_CATEGORY)) {
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



        List<Entry>  catEntries    = new ArrayList<Entry>();
        List<Entry>  seriesEntries = new ArrayList<Entry>();

        List<String> args          = new ArrayList<String>();
        if (categoryId != null) {
            args.add(Fred.ARG_CATEGORY_ID);
            args.add(categoryId);
        }

        Element         root = call(Fred.URL_CATEGORY_CHILDREN, args);
        NodeList children    = XmlUtil.getElements(root, Fred.TAG_CATEGORY);
        HashSet<String> seen = new HashSet<String>();
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            Element item = (Element) children.item(childIdx);
            String  id   = XmlUtil.getAttribute(item, ATTR_ID);
            if (seen.contains(id)) {
                continue;
            }
            seen.add(id);
            //            System.err.println("category child id:" + id);
            Entry entry = createCategoryEntry(mainEntry, parentEntry, id);
            catEntries.add(entry);
        }



        args = new ArrayList<String>();
        if (categoryId != null) {
            args.add(Fred.ARG_CATEGORY_ID);
            args.add(categoryId);
        }
        root     = call(Fred.URL_CATEGORY_SERIES, args);
        children = XmlUtil.getElements(root, Fred.TAG_SERIES);
        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
            if (childIdx > 4) {
                break;
            }
            Element item = (Element) children.item(childIdx);
            String  id   = XmlUtil.getAttribute(item, ATTR_ID);
            String  name = XmlUtil.getAttribute(item, ATTR_TITLE);
            //            System.err.println("series child id:" + id);
            Entry entry = createSeriesEntry(request,mainEntry, parentEntry, id, name);
            seriesEntries.add(entry);
        }

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
    public Element call(String url, List<String> args) throws Exception {
        long t1 = System.currentTimeMillis();
        url = makeUrl(url, args);
        if (seenUrls.contains(url)) {
            //            System.err.println("**** Fred URL:" + url);
        } else {
            //            System.err.println("Fred URL:" + url);
            seenUrls.add(url);
        }
        String xml = IOUtil.readContents(new URL(url));
        long   t2  = System.currentTimeMillis();
        //        System.err.println("\ttime:" + (t2 - t1));
        Element root = XmlUtil.getRoot(xml);

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
    public String makeUrl(String url, List<String> args) throws Exception {
        List<String> urlArgs = new ArrayList<String>();
        urlArgs.add(Fred.ARG_API_KEY);
        urlArgs.add(getApiKey());
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createCategoryEntry(Entry mainEntry, Entry categoryEntry,
                                      String categoryId)
            throws Exception {
        if ( !isEnabled()) {
            return null;
        }
        String       name = categoryId;
        Date         dttm = new Date();

        List<String> args = new ArrayList<String>();
        if (categoryId != null) {
            args.add(Fred.ARG_CATEGORY_ID);
            args.add(categoryId);
        }

        String id = createSynthId(mainEntry, Fred.PREFIX_CATEGORY,
                                  categoryId);
        Entry entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }



        String  desc    = "";
        Element root    = call(Fred.URL_CATEGORY, args);
        Element catNode = XmlUtil.findChild(root, Fred.TAG_CATEGORY);

        if (catNode != null) {
            name = XmlUtil.getAttribute(catNode, ATTR_NAME, categoryId);
        }
        entry = new Entry(id, this);
        String fredUrl = "https://research.stlouisfed.org/fred2/categories/"
                         + categoryId;
        Resource resource = new Resource(new URL(fredUrl));
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
        String id    = createSynthId(mainEntry, Fred.PREFIX_SERIES, seriesId);
        Entry  entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }


        if (name == null) {
            name = seriesId;
        }

        String desc = "";
        Date   dttm = new Date();
        FredSeriesTypeHandler seriesTypeHandler =
            (FredSeriesTypeHandler) getRepository().getTypeHandler(
                Fred.TYPE_SERIES);
        entry = new Entry(id, seriesTypeHandler);
        Object[] values = seriesTypeHandler.makeEntryValues(null);
        values[FredSeriesTypeHandler.IDX_SERIES_ID] = seriesId;

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
        if (type.equals(Fred.PREFIX_CATEGORY)) {
            return createCategoryEntry(mainEntry, mainEntry, id);
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



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Entry(id, this, true);
    }



}

/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.socrata;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

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
public class SocrataTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    public static final String PREFIX_CATEGORY = "category";

    /** _more_ */
    public static final String PREFIX_SERIES = "series";

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
    public SocrataTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
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
        return getEntryManager().createSynthId(mainEntry,
                type + TypeHandler.ID_DELIMITER + id);
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
                && !parentEntry.getTypeHandler().isType(
                    SocrataCategoryTypeHandler.TYPE_CATEGORY)) {
            return null;
        }

        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        /*
        System.err.println("SocrataTypeHandler.getSynthIds:" + synthId
                           + " parent:" + parentEntry.getName()
                           + " mainEntry: " + mainEntry.getName());
        */
        String categoryId = null;

        if (Utils.stringDefined(synthId)) {
            List<String> toks = StringUtil.split(synthId,
                                    TypeHandler.ID_DELIMITER, true, true);
            if (toks.size() <= 1) {
                System.err.println("SocrataTypeHandler.getSynthIds: bad id:"
                                   + synthId);

                return null;
            }
            if ( !toks.get(0).equals(PREFIX_CATEGORY)) {
                return null;
            }
            categoryId = toks.get(1);
        }


        Object[] categoryValues = null;
        if ((parentEntry != null)
                && parentEntry.getTypeHandler().isType(
                    SocrataCategoryTypeHandler.TYPE_CATEGORY)) {
            categoryValues =
                parentEntry.getTypeHandler().getEntryValues(mainEntry);
        }
        if (categoryValues == null) {
            categoryValues =
                mainEntry.getTypeHandler().getEntryValues(mainEntry);
        }

        if (categoryId == null) {
            categoryId = ((categoryValues != null)
                          ? (String) categoryValues[SocrataCategoryTypeHandler.IDX_CATEGORY_ID]
                          : (String) null);
        }





        List<String> args    = new ArrayList<String>();
        List<Entry>  entries = new ArrayList<Entry>();

        /*
        if (catsNode != null) {
            NodeList children = XmlUtil.getElements(catsNode, Socrata.TAG_ROW);
            for (int childIdx = 0; childIdx < children.getLength();
                    childIdx++) {
                Element item = (Element) children.item(childIdx);
                String id = XmlUtil.getGrandChildText(item,
                                Socrata.TAG_CATEGORY_ID, "");
                String name = XmlUtil.getGrandChildText(item, Socrata.TAG_NAME,
                                  "");
                Entry entry = createCategoryEntry(mainEntry, parentEntry, id,
                                  name);
                catEntries.add(entry);
            }
        }
        */

        //Sort the entries
        entries = getEntryManager().getEntryUtil().sortEntriesOnName(entries,
                false);
        ids = new ArrayList<String>();
        for (Entry child : entries) {
            ids.add(child.getId());
        }
        parentEntry.setChildIds(ids);

        return ids;
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
        Date   dttm  = new Date();
        String id    = createSynthId(mainEntry, PREFIX_CATEGORY, categoryId);
        Entry  entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }


        String desc = "";
        entry = new Entry(id, this);
        String socrataUrl =
            "http://www.socrata.gov/beta/api/qb.cfm?category=" + categoryId;
        Resource resource = new Resource(new URL(socrataUrl));

        Object[] values   = this.makeEntryValues(null);
        values[SocrataCategoryTypeHandler.IDX_CATEGORY_ID] = categoryId;
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
    private Entry createSeriesEntry(Entry mainEntry, Entry parentEntry,
                                    String seriesId, String name)
            throws Exception {

        if ( !Utils.stringDefined(name)) {
            name = seriesId;
        }

        String id    = createSynthId(mainEntry, PREFIX_SERIES, seriesId);
        Entry  entry = getEntryManager().getEntryFromCache(id);
        if (entry != null) {
            return entry;
        }
        String desc = "";
        Date   dttm = new Date();
        SocrataSeriesTypeHandler seriesTypeHandler =
            (SocrataSeriesTypeHandler) getRepository().getTypeHandler(
                SocrataSeriesTypeHandler.TYPE_SERIES);
        entry = new Entry(id, seriesTypeHandler);
        Object[] values = seriesTypeHandler.makeEntryValues(null);
        values[SocrataSeriesTypeHandler.IDX_SERIES_ID] = seriesId;

        entry.initEntry(name, desc, parentEntry, parentEntry.getUser(),
                        new Resource(), "", Entry.DEFAULT_ORDER,
                        dttm.getTime(), dttm.getTime(), dttm.getTime(),
                        dttm.getTime(), values);
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

        List<String> toks = StringUtil.split(id, TypeHandler.ID_DELIMITER,
                                             true, true);
        if (toks.size() <= 1) {
            return null;
        }
        String type = toks.get(0);
        id = toks.get(1);
        //TODO: get the name
        if (type.equals(PREFIX_CATEGORY)) {
            return createCategoryEntry(mainEntry, mainEntry, id, null);
        } else {
            return createSeriesEntry(mainEntry, mainEntry, id, null);
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

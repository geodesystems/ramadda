/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.media;


import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.repository.type.*;
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
public class YouTubeChannelTypeHandler extends ExtensibleGroupTypeHandler {


    /** _more_ */
    public static final String ARG_API_KEY = "key";

    /** _more_ */
    public static final String ARG_PART = "part";

    /** _more_ */
    public static final String ARG_PLAYLIST_ID = "playlistId";

    /** _more_ */
    public static final String ARG_CHANNEL_ID = "channelId";


    /** _more_ */
    public static final String URL_BASE =
        "https://www.googleapis.com/youtube/v3/playlistItems";

    /** _more_ */
    public static final String URL_PLAYLIST = URL_BASE + "/playlistItems";


    /** _more_ */
    public static final int IDX_CHANNEL_ID = 0;


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
    public YouTubeChannelTypeHandler(Repository repository, Element entryNode)
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
            apiKey = getRepository().getProperty("youtube.api.key",
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
        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        if (Utils.stringDefined(synthId)) {}

        //        String json = 
        ids = new ArrayList<String>();
        //        for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
        //            Entry entry = createVideoEntry(mainEntry, parentEntry, id);
        //            ids.add(entry.getId());
        //        }

        parentEntry.setChildIds(ids);

        return ids;
    }


    /**
     * _more_
     * @param args _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeUrl(List<String> args) throws Exception {
        List<String> urlArgs = new ArrayList<String>();
        urlArgs.add(ARG_API_KEY);
        urlArgs.add(getApiKey());
        for (String arg : args) {
            urlArgs.add(arg);
        }

        return HtmlUtils.url(URL_BASE, urlArgs);
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
    private Entry createChannelEntry(Entry mainEntry, Entry categoryEntry,
                                     String categoryId)
            throws Exception {
        if ( !isEnabled()) {
            return null;
        }
        String       name = categoryId;
        Date         dttm = new Date();

        List<String> args = new ArrayList<String>();
        if (categoryId != null) {
            args.add(ARG_CHANNEL_ID);
            args.add(categoryId);
        }

        return null;
        /*
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
                        resource, "", dttm.getTime(), dttm.getTime(),
                        dttm.getTime(), dttm.getTime(), values);

        getEntryManager().cacheSynthEntry(entry);

        return entry;
        */
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

        return createChannelEntry(mainEntry, mainEntry, id);
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

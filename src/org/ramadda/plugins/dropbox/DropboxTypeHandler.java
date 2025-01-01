/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.dropbox;


import com.dropbox.core.*;

import org.json.*;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;



import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.ArrayList;
import java.util.Date;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.Locale;


/**
 */
public class DropboxTypeHandler extends ExtensibleGroupTypeHandler {

    /** _more_ */
    public static final int IDX_ACCESS_TOKEN = 0;

    /** _more_ */
    private SimpleDateFormat displaySdf =
        new SimpleDateFormat("MMMMM dd - HH:mm");

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DropboxTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param column _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     * @param formInfo _more_
     * @param baseHandler _more_
     *
     * @throws Exception _more_
     */
    public void addColumnToEntryForm(Request request, Column column,
                                     Appendable formBuffer, Entry entry,
                                     Object[] values, Hashtable state,
                                     FormInfo formInfo,
                                     TypeHandler baseHandler)
            throws Exception {
        super.addColumnToEntryForm(request, column, formBuffer, entry,
                                   values, state, formInfo, baseHandler);
        if (column.getOffset() != IDX_ACCESS_TOKEN) {
            return;
        }
        boolean hasValue = column.getString(values) != null;
        if ( !hasValue || hasValue) {
            StringBuilder tmpSb = new StringBuilder();
            String        url   = getAuthenticationUrl();
            tmpSb.append(HtmlUtils.href(url, url,
                                        HtmlUtils.attr("target",
                                            "_dropbox")));
            formBuffer.append(HtmlUtils.formEntry("Authentication URL:",
                    tmpSb.toString()));
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getAuthenticationUrl() throws Exception {
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(getConfig(),
                                           getAppInfo());
        String authorizeUrl = webAuth.start();

        return authorizeUrl;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DbxRequestConfig getConfig() throws Exception {
        return new DbxRequestConfig("ramadda",
                                    Locale.getDefault().toString());
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DbxAppInfo getAppInfo() throws Exception {
        String ramaddaAppKey = getRepository().getProperty("dropbox.app.key",
                                   "");
        String ramaddaAppSecret =
            getRepository().getProperty("dropbox.app.secret", "");

        return new DbxAppInfo(ramaddaAppKey, ramaddaAppSecret);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DbxClient getClient(Request request, Entry entry)
            throws Exception {
        String accessToken = (String) entry.getValue(IDX_ACCESS_TOKEN);
        if ( !Utils.stringDefined(accessToken)) {
            return null;
        }
        DbxClient client = new DbxClient(getConfig(), accessToken);

        return client;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param dropboxEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select, Entry dropboxEntry,
                                    Entry parentEntry, String synthId)
            throws Exception {

        System.err.println("DropboxTypeHandler.getSynthId:" + synthId
                           + " parent:" + parentEntry.getName()
                           + " dropbox: " + dropboxEntry.getName());
        List<String> ids = parentEntry.getChildIds();
        if (ids != null) {
            return ids;
        }
        ids = new ArrayList<String>();

        if (synthId == null) {
            synthId = "/";
        }

        //            for (int i = 0; i < channels.length(); i++) {
        //                Entry channelEntry = createDropboxEntry(dropboxEntry, channel,   channelsToShow);
        //getEntryManager().cacheSynthEntry(channelEntry);
        //ids.add(channelEntry.getId());
        //            }


        DbxClient client = getClient(request, dropboxEntry);
        if (client == null) {
            return ids;
        }
        System.err.println(
            "PROPERTY:" + System.getProperty("java.protocol.handler.pkgs"));

        System.out.println("Linked account: "
                           + client.getAccountInfo().displayName);
        DbxEntry.WithChildren listing = client.getMetadataWithChildren("/");
        System.out.println("Files in the root path:");
        for (DbxEntry child : listing.children) {
            if (child.isFolder()) {
                System.out.println("folder:" + child.path);
                DbxEntry.WithChildren children =
                    client.getMetadataWithChildren(child.path);
                for (DbxEntry grandchild : children.children) {
                    System.out.println("\t:" + grandchild.name);
                }
            }
            //            System.out.println("" + child.name + ": " + child.toString());
        }



        return ids;
    }



    /**
     * _more_
     *
     *
     * @param dropboxEntry _more_
     * @param channel _more_
     * @param channelsToShow _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createDropboxEntry(Entry dropboxEntry, JSONObject channel,
                                     HashSet<String> channelsToShow)
            throws Exception {
        String channelId = JsonUtil.readValue(channel, "id", "");
        String name      = JsonUtil.readValue(channel, "name", "");
        if (channelsToShow != null) {
            if ( !(channelsToShow.contains(channelId)
                    || channelsToShow.contains(name))) {
                return null;
            }
        }
        Date   dttm = new Date();
        String id = getEntryManager().createSynthId(dropboxEntry, channelId);

        String topic = JsonUtil.readValue(channel, "topic.value", "");
        String purpose = JsonUtil.readValue(channel, "purpose.value", "");
        TypeHandler dropboxTypeHandler =
            getRepository().getTypeHandler("dropbox_file");
        Entry newEntry = new Entry(id, dropboxTypeHandler);
        //        https://geodesystems.slack.com/messages/general/

        String   desc     = "";
        Resource resource = null;
        Object[] values   = dropboxTypeHandler.makeEntryValues(null);
        newEntry.initEntry(name, desc, dropboxEntry, dropboxEntry.getUser(),
                           resource, "", dttm.getTime(), dttm.getTime(),
                           dttm.getTime(), dttm.getTime(), values);

        newEntry.setMasterTypeHandler(this);

        return newEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param dropboxEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry dropboxEntry,
                                String id)
            throws Exception {
        //        System.err.println("Dropbox.makeSynthEntry id = " + id +" dropbox:" + dropboxEntry.getName());
        //        String token = getToken(request, dropboxEntry);
        //        if ( !Utils.stringDefined(token)) {
        //            return null;
        //        }
        List<String> toks = StringUtil.split(id, ":", true, true);

        return null;
        //        return createMessageEntry(request, dropboxEntry, channelId, message);

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
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        System.err.println(
            "PROPERTY:" + System.getProperty("java.protocol.handler.pkgs"));
        String ramaddaAppKey    = "y0ghyhzkx8ltdoz";
        String ramaddaAppSecret = "dummy";
        DbxAppInfo appInfo = new DbxAppInfo(ramaddaAppKey, ramaddaAppSecret);

        DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0",
                                      Locale.getDefault().toString());
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config,
                                           appInfo);


        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to: " + authorizeUrl);
        String        code = "oUzX9lO9Yk0AAAAAAAACcDH8rs00l5Pb82B8SUqFM6I";

        DbxAuthFinish authFinish  = webAuth.finish(code);
        String        accessToken = authFinish.accessToken;
        DbxClient     client      = new DbxClient(config, accessToken);
        System.err.println(
            "PROPERTY:" + System.getProperty("java.protocol.handler.pkgs"));


        System.out.println("Linked account: "
                           + client.getAccountInfo().displayName);
    }



}

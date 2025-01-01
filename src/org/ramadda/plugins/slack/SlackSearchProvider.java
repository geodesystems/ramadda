/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.slack;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;

import org.ramadda.repository.util.ServerInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sat, Nov 8, '14
 * @author         Enter your name here...
 */
public class SlackSearchProvider extends SearchProvider {



    /** _more_ */
    private String externalUrl;

    /** _more_ */
    private String name;

    /** _more_ */
    private String token;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public SlackSearchProvider(Repository repository) {
        super(repository, "slack");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public SlackSearchProvider(Repository repository, List<String> args) {
        super(repository);
        if (args != null) {
            if (args.size() > 0) {
                token = args.get(0);
            }
            if (args.size() > 1) {
                name = args.get(1);
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/favicon.png";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (name != null) {
            return name;
        }

        return "SlackSearchProvider";
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
    public List<Entry> getEntries(Request request, org.ramadda.repository.util.SelectInfo searchInfo)
            throws Exception {
        List<Entry> results = new ArrayList<Entry>();
        if ( !Utils.stringDefined(token)) {
            return results;
        }
        String url = Slack.URL_SEARCH;
        url += "?";
        url += HtmlUtils.arg(Slack.ARG_QUERY,
                             request.getString(ARG_TEXT, ""));
        url += "&";
        url += HtmlUtils.arg(Slack.ARG_TOKEN, token);
        System.err.println("url:" + url);
        String json = IOUtil.readContents(url);
        System.err.println("Json:" + json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
        if ( !obj.has("ok")) {
            System.err.println("SlackSearchProvider: no ok field in json:"
                               + json);

            return results;

        }

        if ( !obj.has("ok")) {
            System.err.println("SlackSearchProvider: no ok field in json:"
                               + json);

            return results;

        }

        if ( !obj.getBoolean("ok")) {
            System.err.println("SlackSearchProvider: ok=false:" + json);

            return results;
        }

        JSONObject messages = obj.getJSONObject("messages");
        JSONObject files    = obj.getJSONObject("files");

        if (messages != null) {
            JSONArray matches = messages.getJSONArray("matches");
            for (int i = 0; i < matches.length(); i++) {
                JSONObject message = matches.getJSONObject(i);
                String     text    = message.getString("text");
                String     user    = message.getString("user");
                String     link    = message.getString("permalink");

                /*
                Entry        newEntry = new Entry(albumEntryId, this, true);
            addMetadata(newEntry, album, desc);
            entries.add(newEntry);
            newEntry.setIcon("/gdata/picasa.png");
            Date dttm = album.getDate();
            Date now  = new Date();
            newEntry.initEntry(title, desc.toString(), entry,
                               getUserManager().getLocalFileUser(),
                               new Resource(), "", dttm.getTime(),
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);

                */
            }

        }


        return results;
    }



}

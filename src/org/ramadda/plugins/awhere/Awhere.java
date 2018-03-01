/*
* Copyright (c) 2008-2018 Geode Systems LLC
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

package org.ramadda.plugins.awhere;


import org.json.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Json;
import org.ramadda.util.Oauth;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;


import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 */
public class Awhere {

    /** _more_ */
    private static TTLCache<String, Oauth> auths = new TTLCache<String,
                                                       Oauth>(60 * 60 * 1000);


    /** _more_ */
    public static final String URL_BASE = "https://api.awhere.com";

    /** _more_ */
    public static final String URL_OAUTH = URL_BASE + "/oauth/token";

    /** _more_ */
    public static final String URL_FIELDS = URL_BASE + "/v2/fields";

    /** _more_ */
    public static final String URL_OBSERVATIONS =
        URL_BASE + "/v2/weather/fields/{fieldId}/observations";

    /** _more_ */
    private static final String URL_FORECASTS =
        URL_BASE + "/v2/weather/fields/{fieldId}/forecasts";


    /** _more_ */
    private static final String URL_FORECASTS_GEO =
        URL_BASE + "/v2/weather/locations/{lat},{lon}/forecasts";



    /** _more_ */
    public static final String PROP_KEY = "awhere.api.key";

    /** _more_ */
    public static final String PROP_SECRET = "awhere.api.secret";

    /**
     * _more_
     *
     * @param fieldId _more_
     *
     * @return _more_
     */
    public static final String getObservationsUrl(String fieldId) {
        return URL_OBSERVATIONS.replace("{fieldId}", fieldId);
    }

    /**
     * _more_
     *
     * @param fieldId _more_
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public static final String getForecastsUrl(String fieldId, double lat,
            double lon) {
        return URL_FORECASTS_GEO.replace("{fieldId}",
                                         fieldId).replace("{lat}",
                                             "" + lat).replace("{lon}",
                                                 "" + lon);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doGet(Entry entry, URL url) throws Exception {
        try {
            String token = Awhere.getToken(entry, false);
            String res   = null;
            try {
                res = Utils.doGet(url, "Authorization", "Bearer " + token);
            } catch (Exception exc) {
                token = Awhere.getToken(entry, true);
                res   = Utils.doGet(url, "Authorization", "Bearer " + token);
            }

            return res;
        } catch (Exception exc) {
            System.err.println("Error reading:" + url);

            throw exc;
        }

    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param force _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getToken(Entry entry, boolean force)
            throws Exception {
        try {
            Oauth oauth = getOauth(entry, force);
            if (oauth != null) {
                return oauth.getToken(false);
            }

            return null;
        } catch (Exception exc) {
            System.err.println("Awhere.getToken Error:" + exc);

            return null;
        }
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public static String getId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]+", "_");
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param force _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Oauth getOauth(Entry entry, boolean force)
            throws Exception {
        Repository repository = entry.getTypeHandler().getRepository();
        String     cacheKey   = entry.getId() + "_" + entry.getChangeDate();

        Oauth      oauth      = null;
        if ( !force) {
            oauth = auths.get(cacheKey);
            if (oauth != null) {
                return oauth;
            }
        }
        String key       = null;
        String secret    = null;

        Entry  farmEntry = getFarmEntry(entry);
        if (farmEntry != null) {
            key = farmEntry.getValue(AwhereFarmTypeHandler.IDX_KEY, null);
            secret = farmEntry.getValue(AwhereFarmTypeHandler.IDX_SECRET,
                                        null);
        }

        if ( !Utils.stringDefined(key) || !Utils.stringDefined(secret)) {
            key    = repository.getProperty(PROP_KEY, (String) null);
            secret = repository.getProperty(PROP_SECRET, (String) null);
        }

        if ( !Utils.stringDefined(key) || !Utils.stringDefined(secret)) {
            return null;
        }
        oauth = new Oauth(URL_OAUTH, key.trim(), secret.trim());
        oauth.getToken(true);
        auths.put(cacheKey, oauth);

        return oauth;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Entry getFarmEntry(Entry entry) throws Exception {
        if (entry == null) {
            return null;
        }
        if (entry.getTypeHandler().isType("type_awhere_farm")) {
            return entry;
        }

        return getFarmEntry(entry.getParentEntry());
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param url _more_
     * @param json _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doPost(Entry entry, String url, String json)
            throws Exception {
        String token = Awhere.getToken(entry, false);
        if (token == null) {
            return null;
        }
        try {
            return Utils.doPost(new URL(url), json, "Content-Type",
                                "application/json", "Authorization",
                                "Bearer " + token);
        } catch (Exception exc) {
            token = Awhere.getToken(entry, true);
            if (token == null) {
                return null;
            }
            try {
                return Utils.doPost(new URL(url), json, "Content-Type",
                                    "application/json", "Authorization",
                                    "Bearer " + token);
            } catch (Exception exc2) {
                return null;
            }
        }

    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param url _more_
     * @param json _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doGet(Entry entry, String url, String json)
            throws Exception {
        String token = Awhere.getToken(entry, false);
        if (token == null) {
            return null;
        }
        try {
            return Utils.doGet(new URL(url), "Authorization",
                               "Bearer " + token);
        } catch (Exception exc) {
            token = Awhere.getToken(entry, true);
            if (token == null) {
                return null;
            }
            try {
                return Utils.doGet(new URL(url), "Authorization",
                                   "Bearer " + token);
            } catch (Exception exc2) {
                return null;
            }
        }

    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doDelete(Entry entry, URL url) throws Exception {
        String token = Awhere.getToken(entry, false);
        String res   = null;
        try {
            return Utils.doHttpRequest("DELETE", url, null, "Authorization",
                                       "Bearer " + token);
        } catch (Exception exc) {
            token = Awhere.getToken(entry, true);

            return Utils.doHttpRequest("DELETE", url, null, "Authorization",
                                       "Bearer " + token);
        }

    }

}

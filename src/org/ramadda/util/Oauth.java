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

package org.ramadda.util;


import org.json.*;


import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.*;




/**
 */

public class Oauth {

    /** _more_ */
    public static final String CONTENT_TYPE =
        "application/x-www-form-urlencoded";

    /** _more_ */
    public static String AUTH_PREFIX = "Basic ";

    /** _more_ */
    public static final String AUTH_BODY = "grant_type=client_credentials";


    /** _more_ */
    private String url;

    /** _more_ */
    private String key;

    /** _more_ */
    private String secret;

    /** _more_ */
    private String token;

    /**
     * _more_
     *
     * @param url _more_
     * @param key _more_
     * @param secret _more_
     */
    public Oauth(String url, String key, String secret) {
        this.url    = url;
        this.key    = key;
        this.secret = secret;
    }


    /**
     * _more_
     *
     * @param force _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getToken(boolean force) throws Exception {
        if ((this.token == null) || force) {
            String json = doPost(this.url, this.key, this.secret);
            //            System.err.println("Oauth.getToken:" + json);
            JSONObject js = new JSONObject(new JSONTokener(json));
            this.token = js.optString("access_token");
        }

        return this.token;
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param secret _more_
     *
     * @return _more_
     */
    public static String makeHash(String key, String secret) {
        String s = key + ":" + secret;

        return AUTH_PREFIX + Utils.encodeBase64(s.getBytes());
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param key _more_
     * @param secret _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String doPost(String url, String key, String secret)
            throws Exception {
        return Utils.doPost(new URL(url), AUTH_BODY, "Content-Type",
                            CONTENT_TYPE, "Authorization",
                            makeHash(key, secret));
    }





    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //        System.err.println(doPost("https://api.awhere.com/oauth/token", args[0], args[1]));
    }



}

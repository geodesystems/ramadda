/*
* Copyright (c) 2008-2021 Geode Systems LLC
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

import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;



/**
 * Class description
 * https://oembed.com
 *
 * @version        $version$, Fri, Feb 26, '21
 * @author         Enter your name here...
 */
public class Oembed {

    /** _more_ */
    private static List<Oembed> oembeds;

    /** _more_ */
    String name;

    /** _more_ */
    List<String> schemes = new ArrayList<String>();

    /** _more_ */
    String url;

    /**
     * _more_
     *
     * @param obj _more_
     *
     * @throws Exception _more_
     */
    public Oembed(JSONObject obj) throws Exception {
        name = obj.getString("provider_name");
        JSONObject endPoint = obj.getJSONArray("endpoints").getJSONObject(0);
        url = endPoint.getString("url");
        url = url.replace("{format}", "json");
        if ( !endPoint.has("schemes")) {
            addScheme(url);

            return;
        }
        JSONArray schemes = endPoint.getJSONArray("schemes");
        for (int i = 0; i < schemes.length(); i++) {
            addScheme(schemes.getString(i));
        }
        //          System.err.println(name +" " + url +" " + this.schemes);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static List<Oembed> getOembeds() throws Exception {
        if (oembeds == null) {
            List<Oembed> tmp = new ArrayList<Oembed>();
            JSONArray array = new JSONArray(
                                  IO.readContents(
                                      "/org/ramadda/util/oembed.json"));
            //          JSONArray array = new JSONArray(IO.readContents("/org/ramadda/util/test.json"));                
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                tmp.add(new Oembed(obj));
            }
            oembeds = tmp;
        }

        return oembeds;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + " " + url + " " + schemes;
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Oembed find(String url) throws Exception {
        for (Oembed oembed : getOembeds()) {
            if (oembed.match(url)) {
                return oembed;
            }
        }

        return null;
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param width _more_
     * @param height _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Response get(String url, String width, String height)
            throws Exception {
        Oembed oembed = find(url);
        if (oembed == null) {
            //      System.err.println("no embed found");
            return null;
        }
        try {
            String eurl = HtmlUtils.urlEncode(url);
            String rurl = HtmlUtils.url(oembed.url, "url", url, "maxwidth",
                                        width, "maxheight", height);
            URL    req  = new URL(rurl);
            String json = IO.readUrl(req);
            if (json.startsWith("<")) {
                return oembed.getFromXml(url, json);
            } else {
                return oembed.getFromJson(url, json);
            }
        } catch (Exception exc) {
            System.err.println("Error fetching embed for:" + url + " " + exc);

            return null;
        }
    }



    /**
     * _more_
     *
     * @param url _more_
     * @param xml _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Response getFromXml(String url, String xml) throws Exception {
        Element root = XmlUtil.getRoot(xml);

        return new Response(
            url, XmlUtil.getGrandChildText(root, "type", null),
            XmlUtil.getGrandChildText(root, "title", null),
            XmlUtil.getGrandChildText(root, "author_name", null),
            XmlUtil.getGrandChildText(root, "width", null),
            XmlUtil.getGrandChildText(root, "height", null),
            XmlUtil.getGrandChildText(root, "url", null),
            XmlUtil.getGrandChildText(root, "html", null));
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param json _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Response getFromJson(String url, String json) throws Exception {
        JSONObject root = new JSONObject(json);

        return new Response(url, root.optString("type"),
                            root.optString("title"),
                            root.optString("author_name"),
                            root.optString("width"),
                            root.optString("height"), root.optString("url"),
                            root.optString("html"));
    }

    /**
     * _more_
     *
     * @param url _more_
     */
    private void addScheme(String url) {
        url = url.replaceAll("\\.", "\\\\.");
        url = url.replaceAll("\\*", ".*");
        url = url.replaceAll("\\{", "\\\\{");
        url = url.replaceAll("\\}", "\\\\}");
        schemes.add(url);
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    private boolean match(String url) {
        for (String s : schemes) {
            if (url.matches(s)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Fri, Feb 26, '21
     * @author         Enter your name here...
     */
    public static class Response {

        /** _more_ */
        public String originalUrl;

        /** _more_ */
        public String type;

        /** _more_ */
        public String title;

        /** _more_ */
        public String author;

        /** _more_ */
        public String width;

        /** _more_ */
        public String height;

        /** _more_ */
        public String url;

        /** _more_ */
        public String html;

        /**
         * _more_
         *
         * @param originalUrl _more_
         * @param type _more_
         * @param title _more_
         * @param author _more_
         * @param width _more_
         * @param height _more_
         * @param url _more_
         * @param html _more_
         */
        public Response(String originalUrl, String type, String title,
                        String author, String width, String height,
                        String url, String html) {
            this.originalUrl = originalUrl;
            this.type        = type;
            this.title       = title;
            this.author      = author;
            this.width       = height;
            this.height      = height;
            this.url         = url;
            this.html        = html;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return type + " author:" + author + " title:" + title + " w:"
                   + width;
        }




        /**
         * _more_
         *
         * @return _more_
         */
        public String getHtml() {
            if (type.equals("photo")) {
                return HtmlUtils.href(originalUrl, HtmlUtils.image(url));
            }
            if (type.equals("video")) {
                return html;
            }
            if (type.equals("link")) {
                System.err.println("LINk:" + title + " " + author);

                return HtmlUtils.href(originalUrl, (title != null)
                        ? title
                        : (author != null)
                          ? author
                          : originalUrl);
            }

            //rich type
            return html;
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String[] urls = new String[] {
            "https://vimeo.com/515404225",
            "https://www.youtube.com/watch?v=EL2Y1XHd70c",
            "http://www.iamcal.com/linklog/1206113631/",
            "http://www.flickr.com/photos/bees/2341623661/",
            "https://www.facebook.com/don.murray.121/posts/10219628323639292",
            "https://thebeeswaggle.wordpress.com/2018/04/23/pollinators-support-biodiversity/",
            "https://www.ted.com/talks/marla_spivak_why_bees_are_disappearing?language=en",
            "https://youneedone2.tumblr.com/post/644196242666192896/lotus-by-chishou-nakada",
            "https://www.reddit.com/r/bees/comments/lsyykm/save_the_bees_themed_botanical_bread_floral/"
        };
        for (String url : urls) {
            System.err.println(url + " response:"
                               + Oembed.get(url, "500", "300"));
        }





    }


}

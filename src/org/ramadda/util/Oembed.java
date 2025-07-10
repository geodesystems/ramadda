/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.json.*;

import org.w3c.dom.*;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;



/**
 * Class description
 * https://oembed.com
 *
 * @version        $version$, Fri, Feb 26, '21
 * @author         Enter your name here...
 */
public class Oembed {

    /**  */
    private Response fixed;

    /** _more_ */
    private static List<Oembed> oembeds;

    /** _more_ */
    String name;

    /** _more_ */
    List<String> schemes = new ArrayList<String>();

    /** _more_ */
    String url;

    /**
     *
     *
     * @param fixed _more_
     */
    public Oembed(Response fixed) {
        this.fixed = fixed;
    }


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
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String findHtml(String url) throws Exception {
        String id;
        id = StringUtil.findPattern(url,
                                    "https://gitlab.com/.*/snippets/(.*)");
        if (id != null) {
            return "\n<script src='https://gitlab.com/-/snippets/" + id
                   + ".js'></script>\n";
        }

        id = StringUtil.findPattern(url,
                                    "https://gist.github.com/(.*/\\d+$)");
        if (id != null) {
            return "\n<script src='https://gist.github.com/" + id
                   + ".js'></script>\n";
        }

	//https://jsfiddle.net/afabbro/vrVAP/
	//<script async src="//jsfiddle.net/afabbro/vrVAP/embed/"></script>
        id = StringUtil.findPattern(
				    url, "https://jsfiddle.net/(.+/[^/]+)/?");

        if (id != null) {
	    if(true)
		return  "<iframe width='100%' height='300' src='//jsfiddle.net/" + id+"/embedded/' allowfullscreen='allowfullscreen' allowpaymentrequest frameborder='0'></iframe>";

            return "\n<script async src='https://jsfiddle.net/" + id
                   + "/embed'></script>\n";
        }

        return null;

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
        String fixed = findHtml(url);
        if (fixed != null) {
            return new Oembed(new Response(fixed));
        }

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
            return null;
        }
        Response fixed = oembed.fixed;
        if (fixed != null) {
            return fixed;
        }
	String eurl = HtmlUtils.urlEncode(url);
	if(width.endsWith("%")) {
	    width = "640";
	}
	if(width.endsWith("px")) {
	    width = width.replace("px","");
	}
	String rurl = HtmlUtils.url(oembed.url, new String[] {
                "url", eurl, "format", "json", "maxwidth", width, "maxheight",
                height
            }, false);
        try {
            URL    req  = new URL(rurl);
	    IO.Result result = IO.doGetResult(req);
	    if(result.getError()) {
		return null;
	    }
            String json = result.getResult();
            if (json.startsWith("<")) {
                return oembed.getFromXml(url, json);
            } else {
		//		System.err.println("url:" +url + " " + json+"\n");
                return oembed.getFromJson(url, json);
            }
        } catch (Exception exc) {
            System.err.println("Error fetching embed for:" + url + " request url:" + rurl+" exception:" +exc);
	    exc.printStackTrace();
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

        /**  */
        String fixedHtml;

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
         *
         *
         * @param fixedHtml _more_
         */
        public Response(String fixedHtml) {
            this.fixedHtml = fixedHtml;
        }


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
            this.width       = width;
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
                   + width + " " + fixedHtml;
        }


        //<script src="https://gist.github.com/dustinmartin/364172.js"></script>

        /**
         * _more_
         *
         * @return _more_
         */
        public String getHtml() {
            if (fixedHtml != null) {
                return fixedHtml;
            }
            if (type.equals("photo")) {
                return HtmlUtils.href(originalUrl, HtmlUtils.image(url));
            }
            if (type.equals("video")) {
                return html;
            }
            if (type.equals("link")) {
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
            "https://jsfiddle.net/macloo/bvwvd0ao/", null,
            "https://gist.github.com/dustinmartin/364172", null,
            "https://gitlab.com/-/snippets/2085394", null,
            "https://codepen.io/Coderesting/pen/yLyaJMz",
            "https://vimeo.com/515404225",
            "https://www.youtube.com/watch?v=EL2Y1XHd70c",
            "http://www.flickr.com/photos/bees/2341623661/",
            "https://www.ted.com/talks/marla_spivak_why_bees_are_disappearing?language=en",
            "https://youneedone2.tumblr.com/post/644196242666192896/lotus-by-chishou-nakada",
            "https://www.reddit.com/r/bees/comments/lsyykm/save_the_bees_themed_botanical_bread_floral/"
        };
        for (String url : urls) {
            if (url == null) {
                break;
            }
            Response response = Oembed.get(url, "500", "300");
            if (response == null) {
                System.err.println("ERROR:" + url);
            } else {
                System.err.println("OK:" + url + " " + response);
            }
        }





    }


}

/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import org.apache.commons.lang3.text.StrTokenizer;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import java.awt.Toolkit;
import java.awt.image.*;
import java.awt.image.BufferedImage;

import java.io.*;

import java.lang.reflect.Constructor;

import java.net.*;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.*;




/**
 * A collection of utilities
 *
 * @author Jeff McWhirter
 */

@SuppressWarnings("unchecked")
public class WebHarvester {

    /**  */
    private URL url;

    /**  */
    private List<Replace> replaceList = new ArrayList<Replace>();

    /**  */
    private List<Replace> imageReplace = new ArrayList<Replace>();

    /**  */
    private String title;

    /**  */
    private String body;

    /**  */
    private Hashtable<String, Page> seen = new Hashtable<String, Page>();

    /**  */
    private HashSet seenImage = new HashSet();

    /**  */
    private Page page;

    /**  */
    private int cnt = 0;

    /**  */
    private int maxCnt = -1;

    /**  */
    private int maxDepth = -1;

    /**  */
    private List<String> images = new ArrayList<String>();

    /**  */
    private boolean wrapWithSection = true;

    /**  */
    private static SimpleDateFormat mmmyyyysdf =
        new SimpleDateFormat("MMMMM yyyy");

    /**  */
    private static SimpleDateFormat mmmyyyysdf2 =
        new SimpleDateFormat("yyyy-MM");

    /**
     *
     */
    public WebHarvester() {}


    /**
     *
     *
     * @param url _more_
     *
     * @throws Exception _more_
     */
    public WebHarvester(String url) throws Exception {
        this.url = new URL(url);
    }

    /**
     *
     * @param d _more_
     */
    public void setMaxDepth(int d) {
        maxDepth = d;
    }

    /**
     *
     * @param url _more_
     *
     * @throws Exception _more_
     */
    public void setUrl(String url) throws Exception {
        this.url = new URL(url);
    }



    /**
     *  Get the Title property.
     *  @return The Title
     */
    public String getTitle() {
        return title;
    }


    /**
     *  Get the Body property.
     *
     *  @return The Body
     */
    public String getBody() {
        return body;
    }

    /**
     *
     * @param pattern _more_
     * @param with _more_
     */
    public void addReplace(String pattern, String with) {
        replaceList.add(new Replace(pattern, with));
    }

    /**
     *
     * @param pattern _more_
     * @param with _more_
     */
    public void addImageReplace(String pattern, String with) {
        imageReplace.add(new Replace(pattern, with));
    }

    /**
     *
     * @param recurse _more_
     * @param pattern _more_
     * @param notpattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Page harvest(boolean recurse, String pattern, String notpattern)
            throws Exception {
        page = harvest(null, url, null, null, recurse, pattern, notpattern,
                       "", 0);

        return page;
    }

    /**
     *
     * @param parent _more_
     * @param url _more_
     * @param label _more_
     * @param link _more_
     * @param recurse _more_
     * @param linkPattern _more_
     * @param notpattern _more_
     * @param indent _more_
     * @param depth _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Page harvest(Page parent, URL url, String label, String link,
                        boolean recurse, String linkPattern,
                        String notpattern, String indent, int depth)
            throws Exception {

        cnt++;
        if ((maxCnt >= 0) && (cnt > maxCnt)) {
            return null;
        }
        Page page;
        if ((page = seen.get(url.toString())) != null) {
            //      System.out.println("SEEN:" + url);
            return page;
        }
        //      System.out.println("NOT SEEN:" + url);
        if ((maxDepth >= 0) && (depth > maxDepth)) {
            //      System.err.println(indent+"MAX DEPTH");
            return null;
        }

        String surl = url.toString();
        if (ImageUtils.isImage(surl) || surl.toLowerCase().endsWith("pdf")) {
            System.err.println(indent + "RESOURCE:" + url);
            page = new Page(url);
            if (parent != null) {
                parent.addChild(page);
            }
            seen.put(url.toString(), page);

            return page;
        }

        String html = "";
        try {
            html = IO.readContents(url.toString());
        } catch (Exception exc) {
            System.err.println(indent + "BAD URL:" + url + " " + exc);

            return null;
        }
        title = StringUtil.findPattern(html,
                                       "(?s)(?i)<title[^>]*>(.*?)</title>");
        if (title == null) {
            title = IOUtil.stripExtension(IOUtil.getFileTail(url.toString()));
        }
        //A hack for School of Mines harvest
        if (label != null) {
            if (title.equals("T")) {
                title = label;
            }
            if (title.equals("Date")) {
                title = label;
            }
        }

        title = title.replaceAll("–", "-");


        body = StringUtil.findPattern(html,
                                      "(?s)(?i)<body[^>]*>(.*?)</body>");
        if (body == null) {
            body = html;
        }
        for (Replace replace : replaceList) {
            body = body.replaceAll("(?s)(?i)" + replace.pattern,
                                   replace.with);
            //      if(replace.pattern.startsWith("<p>")) {
            //          int index = body.indexOf("<p>&nbsp;</p>");
            //          System.err.println(replace.pattern + " " + index);
            //          System.err.println(body.substring(0,500));
            //      }
        }

        /*
          <p><a href="f800track.gif">Flight track&nbsp; <br>
          <a href="800summary.htm">T
        */

        body = fixUnclosedHrefs(body);
        int maxLength = 256000;

        if (body.length() > maxLength) {
            body = body.replaceAll(">\n+", ">");
        }
        if (body.length() > maxLength) {
            body = body.replaceAll("<tr[^>]+>", "<tr>");
        }
        if (body.length() > maxLength) {
            System.err.println("***** bad length: " + url + " "
                               + body.length());
            body = body.replaceAll(">\n+", ">");
            //      body = body.substring(0,29000);
        }
        System.err.println(indent + "URL:" + url + " " + label);  // +" label:"  + label+" " +body.length());
        //      System.err.println(indent +"URL:" + url +" link:" + link);
        if ( !Utils.stringDefined(title)) {
            title = StringUtil.findPattern(body, "(?s)(?i)<h1.*?>(.*?)<.h1>");
            if (title == null) {
                title = StringUtil.findPattern(body,
                        "(?s)(?i)<h2.*?>(.*?)<.h2>");
            }
            if (title == null) {
                title = "Title";
            }
            title = StringUtil.stripTags(title).trim();
        }
        page           = new Page(url, title, body);
        page.href      = link;
        page.hrefLabel = label;
        seen.put(url.toString(), page);

        if (parent != null) {
            parent.addChild(page);
        }

        for (HtmlUtils.Link childLink :
                HtmlUtils.extractLinks(url, body, ".*", true)) {
            String l = childLink.getLink().replace("\\", "\\\\");
            //      System.err.println("IMG:" + l);
            String fullPath    = childLink.getUrl().toString();
            String replacePath = fullPath;
            for (Replace replace : imageReplace) {
                replacePath = replacePath.replaceAll("(?s)(?i)"
                        + replace.pattern, replace.with);
            }
            page.body = page.body.replaceAll(l, replacePath);
            if ( !seenImage.contains(fullPath)) {
                seenImage.add(fullPath);
                images.add(fullPath);
            }
        }

        if (recurse) {
            for (HtmlUtils.Link childLink :
                    HtmlUtils.extractLinks(url, page.body, null)) {
                if (Utils.stringDefined(notpattern)
                        && childLink.matches(notpattern)) {
                    System.err.println("not:" + childLink);
                    continue;
                }

                if (childLink.matches(linkPattern)) {
                    Page child = harvest(page, childLink.getUrl(),
                                         childLink.getLabel(),
                                         childLink.getLink(), recurse,
                                         linkPattern, notpattern,
                                         indent + "  ", depth + 1);
                    if (child == null) {
                        //                      System.err.println(indent +" " +"NO CHILD:"  + childLink.getUrl());
                        continue;
                    }
                    String l = childLink.getLink().replace("\\", "\\\\");
                    //              System.out.println("Link:" + l);
                    page.body = page.body.replaceAll("(?i)<a +href=\"" + l
                            + "\" *>([^<>]*?)</a>", "[[" + child.id
                                + "|$1]]");
                    page.body = page.body.replaceAll("(?i)href=\"" + l
                            + "\"", "href=\"/repository/entry/show?entryid="
                                    + child.id + "\"");
                    page.body = page.body.replaceAll("(?i)href=\'" + l
                            + "\'", "href=\"/repository/entry/show?entryid="
                                    + child.id + "\"");
                    page.body = page.body.replaceAll("(?i)href=" + l,
                            "href=\"/repository/entry/show?entryid="
                            + child.id + "\"");
                    //              System.out.println(page.body);
                    //              Utils.exitTest(0);
                } else {

                    String l = childLink.getLink().replace("\\", "\\\\");
                    String fullPath = "href=\""
                                      + childLink.getUrl().toString() + "\"";
                    page.body = page.body.replaceAll("href=\"" + l + "\"",
                            fullPath);
                    page.body = page.body.replaceAll("href=\'" + l + "\'",
                            fullPath);
                    page.body = page.body.replaceAll("href=" + l, fullPath);
                }

            }
        }

        return page;

    }

    /**
     *
     * @throws Exception _more_
     */
    public void fetchImages() throws Exception {
        for (String path : images) {
            String replacePath = path;
            for (Replace replace : imageReplace) {
                replacePath = replacePath.replaceAll("(?s)(?i)"
                        + replace.pattern, replace.with);
            }
            replacePath = "." + replacePath;
            File f      = new File(replacePath);
            File parent = f.getParentFile();
            if ( !f.exists()) {
                parent.mkdirs();
                try {
                    //A hack to clean up the path
                    path = path.replace("/..", "");
                    InputStream      is  = IO.getInputStream(path);
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtil.writeTo(is, fos);
                    IOUtil.close(is);
                    IOUtil.close(fos);
                    System.err.println("Fetched image:" + path);
                } catch (Exception exc) {
                    System.err.println("ERROR fetching image:" + path + " "
                                       + exc);
                }
                //              f.mkdirs();
            }

        }
    }


    /**
     *
     * @return _more_
     */
    public Page getPage() {
        return page;
    }

    /**
     */
    public void addReplaceMS() {
        //Non ascii
        addReplace("<strong>([^<]+)</a>", "$1</a>");
        addReplace(" ", " ");
        addReplace("[^\\x00-\\x7F]", "");
        addReplace("style *= *'.*?'", "");
        addReplace("class=MsoNormal", "");
        addReplace("class=SpellE", "");
        addReplace("class=GramE", "");
        addReplace("lang=DE", "");
        addReplace("<!--\\[if.*?\\]>", "");
        addReplace("<!\\[if.*?\\]>", "");
        addReplace("<!\\[endif\\]-->", "");
        addReplace("<!\\[endif\\]>", "");
        addReplace("<!--msnavigation-->", "");
        addReplace("<o\\:p>", "");
        addReplace("</o\\:p>", "");
        addReplace("<span\\s+>", "<span>");
        addReplace("<span>\\s*</span>", "");
        addReplace("<span>\\s*</span>", "");
        addReplace("<span>(.*?)</span>", "$1");
        addReplace("<tr\\s+>", "<tr>");
        addReplace("<p\\s+>", "<p>");
        addReplace("<v:shape.*?</v:shape>", "");
        addReplace("<i *>\\s*</i>", "");
        addReplace("<i +>", "<i>");
        addReplace("<td.*?>(.*?)</td>", "<td>$1</td>");
        addReplace("([^\\s]+)<h", "$1\n<h");
        addReplace(">\\n", ">");
        addReplace("<table", "\n<table");
        addReplace("<h", "\n<h");
        addReplace("<tr", "\n<tr");
        addReplace("/tr>", "/tr>\n");
        addReplace("/div>", "/div>\n");
        addReplace("<td><p>(.*?)</p></td>", "<td>$1</td>");
        addReplace("<font.*?>", "");
        addReplace("</font>", "");
        addReplace("<p> *&nbsp; *</p>", "");
        addReplace("–", "-");
        addReplace("\\r", "\n");
        addReplace("\\s*\\n\\s*", "\n");
        addReplace("\\n\\n+", "\n");
        addReplace("(<a[^>]*>)\\n", "$1");
    }

    /**  */
    private static int idCnt = 0;


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Page {

        /**  */
        boolean isResource = false;

        /**  */
        Page parent = null;

        /**  */
        boolean processed = false;

        /**  */
        String id = "page_" + (idCnt++);

        /**  */
        String href;

        /**  */
        String hrefLabel;

        /**  */
        URL url;

        /**  */
        String title;

        /**  */
        String body;

        /**  */
        List<Page> children = new ArrayList<Page>();

        /**
         *
         *
         * @param url _more_
         */
        public Page(URL url) {
            isResource = true;
            this.url   = url;
            this.title =
                IOUtil.stripExtension(IOUtil.getFileTail(url.toString()));
        }


        /**
         *
         *
         * @param url _more_
         * @param title _more_
         * @param body _more_
         */
        public Page(URL url, String title, String body) {
            this.url   = url;
            this.title = title;
            this.body  = body;
        }

        /**
         *
         * @param child _more_
         */
        public void addChild(Page child) {
            if (child.parent != null) {
                return;
            }
            children.add(child);
            child.parent = this;
        }

        /**
         *
         * @return _more_
         */
        public URL getUrl() {
            return url;
        }

        /**
         *
         * @return _more_
         */
        public boolean isResource() {
            return isResource;
        }

        /**
         *
         * @return _more_
         */
        public String getHref() {
            return href;
        }

        /**
         *
         * @return _more_
         */
        public String getTitle() {
            return title;
        }

        /**
         *
         * @return _more_
         */
        public String getBody() {
            return body;
        }

        /**
         *
         * @return _more_
         */
        public List<Page> getChildren() {
            return children;
        }

        /**
         *
         * @return _more_
         */
        public String toString() {
            return title + " " + url;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Nov 4, '21
     * @author         Enter your name here...
     */
    public static class Replace {

        /**  */
        String pattern;

        /**  */
        String with;

        /**
         *
         *
         * @param pattern _more_
         * @param with _more_
         */
        public Replace(String pattern, String with) {
            this.pattern = pattern;
            this.with    = with;
        }
    }

    /**
     *
     * @param page _more_
     * @param parent _more_
     *
     * @throws Exception _more_
     */
    private void writeEntryXml(Page page, Page parent) throws Exception {

        //      System.out.println("TITLE:" + page.title  +" " + page.href +" " + page.url);
        StringBuilder sb    = new StringBuilder();
        String        type  = "wikipage";
        String        attrs = "";
        if (page.isResource) {
            String surl = page.url.toString().toLowerCase();
            if (surl.endsWith("pdf")) {
                type = "type_document_pdf";
            } else {
                type = "type_image";
            }
            attrs += XmlUtil.attr("filename", IOUtil.getFileTail(surl));
            String filename = Utils.makeID(page.url.getPath());
            attrs += XmlUtil.attr("file", filename);
            File fileDir = new File("imports");
            fileDir.mkdirs();
            File f = new File("imports/" + filename);
            if ( !f.exists()) {
                try {
                    InputStream is = IO.getInputStream(page.url.toString());
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtil.writeTo(is, fos);
                    IOUtil.close(is);
                    IOUtil.close(fos);
                    System.err.println("Fetched resource:" + page.url
                                       + " as:" + f);
                } catch (Exception exc) {
                    System.err.println("ERROR: fetching resource:" + page.url
                                       + " " + exc + " parent:"
                                       + page.parent.url);

                    return;
                }
            }
        }
        attrs += XmlUtil.attr("type", type) + XmlUtil.attr("id", page.id)
                 + ((parent == null)
                    ? ""
                    : XmlUtil.attr("parent", parent.id));
        String date = null;

        for (String title : new String[] { page.title, page.hrefLabel }) {
            //      System.err.println("title:" + title);
            if (title == null) {
                continue;
            }
            String[] mmmyyyy =
                Utils.findPatterns(
                    title,
                    "(?i)(january|february|march|april|may|june|july|august|september|october|november|december)\\s*(\\d\\d\\d\\d)");
            if (mmmyyyy != null) {
                Date dttm = mmmyyyysdf.parse(mmmyyyy[0] + " " + mmmyyyy[1]);
                date = mmmyyyysdf2.format(dttm);
            }


            if (date == null) {
                String[] mmddyy =
                    Utils.findPatterns(
                        title, "[^\\d]+(\\d\\d)/(\\d\\d)/(\\d\\d)[^\\d]+");
                if (mmddyy != null) {
                    date = "20" + mmddyy[2] + "-" + mmddyy[0] + "-"
                           + mmddyy[1];
                }
            }
            if (date == null) {
                String year = StringUtil.findPattern(title, "\\d\\d\\d\\d");
                if (year != null) {
                    date = year;
                }
            }
            //      System.err.println(title+" date:" + date);
            if (date != null) {
                attrs += XmlUtil.attr("fromdate", date)
                         + XmlUtil.attr("todate", date);

                break;
            }
        }
        sb.append(XmlUtil.openTag("entry", attrs));
        sb.append("\n");

        sb.append(XmlUtil.tag("name", "", XmlUtil.getCdata(page.title)));
        sb.append("\n");
        if ( !page.isResource) {
            //      String content = HtmlUtils.href(page.url.toString(),"Source") +"<br>" + page.body;
            String content = wrapWithSection
                             ? ("+section title={{name}}\n" + page.body
                                + "\n-section\n")
                             : page.body;
            //      if(page.children.size()>0)
            //          content+="\n----\n:heading Links\n{{tree details=false}}";
            //If we are creating something other than a wikipage then set the description
            //sb.append(XmlUtil.tag("description","", XmlUtil.getCdata(content)));
            sb.append(XmlUtil.tag("wikitext", "", XmlUtil.getCdata(content)));
        }
        sb.append("</entry>");
        System.out.println(sb);
        for (Page child : page.children) {
            writeEntryXml(child, page);
        }

    }

    /**
     *
     * @param msg _more_
     */
    public static void usage(String msg) {
        System.err.println(msg);
        System.err.println("usage:");
        System.err.println("\t-maxdepth <depth>");
        System.err.println(
            "\t-pattern <pattern> (pattern to match links to follow)");
        System.err.println(
            "\t-replacems (clean up the extra cruft MS Word generates)");
        System.err.println("\t-doimages (download images)");
        System.err.println(
            "\t-imagereplace <url pattern> <with> (convert image links)");
        System.err.println("\t-entries (generate the RAMADDA entry xml)");
        System.err.println("\t<url> (url to start)");
        Utils.exitTest(0);
    }

    /**
     *
     * @param html _more_
     *
     * @return _more_
     */
    public static String fixUnclosedHrefs(String html) {
        //This is a hack to replace '</a' with a single character that probably won't show up in the html
        //That way we can use a negate pattern with the single replace chart [^c]
        //since I can't figure out how to do negative lookaheads in regexps
        String c = "\uabcd";
        html = html.replace("</a", c);
        html = html.replaceAll("<a([^" + c + "]+)<a", "<a$1</a><a");
        html = html.replace(c, "</a");

        return html;
    }

    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        WebHarvester harvester  = new WebHarvester();
        String       pattern    = null;
        String       notpattern = null;
        boolean      doImages   = false;
        boolean      doEntries  = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("#")) {
                continue;
            }
            if (arg.equals("-nosection")) {
                harvester.wrapWithSection = false;
                continue;
            }
            if (arg.equals("-maxdepth")) {
                harvester.setMaxDepth(Integer.parseInt(args[++i]));
                continue;
            }
            if (arg.equals("-max")) {
                harvester.maxCnt = Integer.parseInt(args[++i]);
                continue;
            }
            if (arg.equals("-pattern")) {
                pattern = args[++i];
                continue;
            }
            if (arg.equals("-notpattern")) {
                notpattern = args[++i];
                continue;
            }
            if (arg.equals("-doimages")) {
                doImages = true;
                continue;
            }
            if (arg.equals("-entries")) {
                doEntries = true;
                continue;
            }
            if (arg.equals("-replacems")) {
                harvester.addReplaceMS();
                continue;
            }
            if (arg.equals("-imagereplace")) {
                harvester.addImageReplace(args[++i], args[++i]);
                harvester.addReplaceMS();
                continue;
            }
            if (arg.startsWith("-")) {
                usage("Unknown argument:" + arg);
            }
            harvester.setUrl(arg);
        }


        Page page = harvester.harvest(true, pattern, notpattern);

        if (doEntries) {
            System.out.println("<entries>");
            harvester.writeEntryXml(page, null);
            System.out.println("</entries>");
        }
        if (doImages) {
            harvester.fetchImages();
        }
    }
}

/*
 * {{tree entries.filter="name:Flight.*" details=false
 * treePrefix="<h3>Flights</h3>" message="" nameTemplate="${name} - ${date}" sort="date" sortdir=up}}
 */





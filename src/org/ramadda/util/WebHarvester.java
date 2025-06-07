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
    private URL url;
    private List<Replace> replaceList = new ArrayList<Replace>();
    private List<Replace> imageReplace = new ArrayList<Replace>();
    private String title;
    private String body;
    private Hashtable<String, Page> seen = new Hashtable<String, Page>();
    private HashSet seenImage = new HashSet();
    private Page page;
    private int cnt = 0;
    private int goodCnt=0;
    private List<String[]> badUrls = new ArrayList<String[]>();

    private int maxCnt = -1;
    private int maxDepth = -1;
    private List<String> images = new ArrayList<String>();
    private boolean addOriginalUrl =false;
    private boolean wrapWithSection = true;
    private boolean writeFiles = true;
    private boolean addTable = false;
    private static SimpleDateFormat mmmyyyysdf =      new SimpleDateFormat("MMMMM yyyy");
    private static SimpleDateFormat mmmyyyysdf2 =    new SimpleDateFormat("yyyy-MM");
    private PrintWriter log;

    public WebHarvester(PrintWriter log) throws Exception {
	this.log = log;
    }

    public WebHarvester(String url) throws Exception {
        this.url = new URL(url);
    }


    public void log(String msg) {
	log.println(msg);
	System.err.println(msg);
    }

    public void setMaxDepth(int d) {
        maxDepth = d;
    }

    public void setUrl(String url) throws Exception {
        this.url = new URL(url);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public void addReplace(String pattern, String with) {
        replaceList.add(new Replace(pattern, with));
    }

    public void addImageReplace(String pattern, String with) {
        imageReplace.add(new Replace(pattern, with));
    }

    public Page harvest(boolean recurse, String pattern, String notpattern)
            throws Exception {
        page = harvest(null, url, null, null, recurse, pattern, notpattern,
                       "", 0);

        return page;
    }

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
            log(indent + "RESOURCE:" + url);
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
	    log(indent + "BAD URL:" + url + " " + exc +" PARENT:" + parent);
	    badUrls.add(new String[]{url.toString(),parent.url.toString()});
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

        body = StringUtil.findPattern(html,  "(?s)(?i)<body[^>]*>(.*?)</body>");
        if (body == null) {
            body = html;
        }

	String ogBody = body;
	//	System.out.println("******** OGBODY:" + title +"\n"+body+"\n\n");
	body = Replace.replace(replaceList, body);
	//	System.out.println("******** BODY:" + title +"\n"+body+"\n\n");
        body = fixUnclosedHrefs(body);
        int maxLength = 256000;
        if (body.length() > maxLength) {
            body = body.replaceAll(">\n+", ">");
        }
        if (body.length() > maxLength) {
            body = body.replaceAll("<tr[^>]+>", "<tr>");
        }
        if (body.length() > maxLength) {
	    //Try to clean up the really long styles
	    for(int l:new int[]{500,400,300,200,100,50,25}) {
		String regex = "(?s)(?i)style=['\"]([^'\"]{" + l+",}?)['\"]";
		body = body.replaceAll(regex," ");
		if(body.length()<maxLength) break;
	    }
	}

        if (body.length() > maxLength) {
            log("***** bad length: " + url + " "               + body.length());
            body = body.replaceAll(">\n+", ">");
	    FileOutputStream fos = new FileOutputStream("badtext.html");
	    PrintWriter          writer    = new PrintWriter(fos);
	    writer.print(body);
	    writer.close();

	    throw new IllegalArgumentException("bad length:" + body.length() +" url:" + url);
            //      body = body.substring(0,29000);
        }
	log(indent + "URL:" + url + " " + label);  // +" label:"  + label+" " +body.length());
	goodCnt++;
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
	//	boolean debug = title.indexOf("Research Projects")>=0;
	boolean debug = false;
	if(debug) {
	    System.out.println("TITLE:" + title);
	    //	    System.out.println("*******\n" + ogBody +"\n*********");
	}

        page           = new Page(url, title, body);
        page.href      = link;
        page.hrefLabel = label;
        seen.put(url.toString(), page);





        if (parent != null) {
            parent.addChild(page);
        }

	
	body = Replace.replace(imageReplace,body);


        for (HtmlUtils.Link childLink :
                HtmlUtils.extractLinks(url, body, ".*", true)) {
            String l = childLink.getLink().replace("\\", "\\\\");
	    //	    System.out.println("IMG:" + l);
            String fullPath    = childLink.getUrl().toString();
            String replacePath = fullPath;
	    replacePath = Replace.replace(imageReplace,replacePath);
            page.body = page.body.replaceAll(l, replacePath);
            if ( !seenImage.contains(fullPath)) {
                seenImage.add(fullPath);
                images.add(fullPath);
            }
        }

	//	page.body = Replace.replace(imageReplace,page.body);
	//	System.out.println("title:" + title);

        if (recurse) {
            for (HtmlUtils.Link childLink :
                    HtmlUtils.extractLinks(url, page.body, null)) {
                if (Utils.stringDefined(notpattern)
                        && childLink.matches(notpattern)) {
                    System.err.println("not:" + childLink);
                    continue;
                }

		if(debug) System.out.println("link:" + childLink);
                if (childLink.matches(linkPattern)) {
                    Page child = harvest(page, childLink.getUrl(),
                                         childLink.getLabel(),
                                         childLink.getLink(), recurse,
                                         linkPattern, notpattern,
                                         indent + "  ", depth + 1);
                    if (child == null) {
			if(debug) System.out.println(indent +" " +"NO CHILD:"  + childLink.getUrl());
                        continue;
                    }
                    String l = childLink.getLink().replace("\\", "\\\\");
		    String linkUrl = childLink.getUrl().toString();
		    //		    System.out.println(title + " Link:" + l);
		    if(childLink.toString().indexOf("steps.htm")>=0) {
			System.out.println("replace:" + childLink);
			System.out.println("replace URL:" + linkUrl);			
		    }
		    String p =  "(?i)<a\\s+href=\"?" + linkUrl + "\"?[^>]*>(.*?)</a>";
		    page.body = page.body.replaceAll(p, "[[" + child.id+ "|$1]]");		    
		    //		    p = "(?i)<a\\s+href=\"?" + l + "\"? *>((?!</a>\\s*</a>).*)";
		    p = "(?i)<a\\s+href=\"?" + l + "\"?[^>]*>(.*?)</a>";
                    page.body = page.body.replaceAll(p, "[[" + child.id+ "|$1]]");
                    page.body = page.body.replaceAll("(?i)href=\"" + l
                            + "\"", "href=\"/repository/entry/show?entryid="
                                    + child.id + "\"");
                    page.body = page.body.replaceAll("(?i)href=\'" +linkUrl
                            + "\'", "href=\"/repository/entry/show?entryid="
                                    + child.id + "\"");
                    page.body = page.body.replaceAll("(?i)href=" + l,
                            "href=\"/repository/entry/show?entryid="
                            + child.id + "\"");
                    page.body = page.body.replaceAll("(?i)href=" + linkUrl,
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

	if(addOriginalUrl)
	    page.body = HtmlUtils.href(url.toString(), "ORIGINAL URL") +"\n<br>" + page.body;
        return page;

    }


    public void fetchImages() throws Exception {
        for (String path : images) {
            String replacePath = path;
	    replacePath = Replace.replace(imageReplace, replacePath);
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
                    log("Fetched image:" + path);
                } catch (Exception exc) {
                    log("ERROR fetching image:" + path + " "  + exc);
                }
                //              f.mkdirs();
            }

        }
    }


    public Page getPage() {
        return page;
    }

    public void addReplaceMS() {
        //Non ascii
        addReplace(" ", " ");
	addReplace("[^\\x00-\\x7F]", "");
	addReplace("<msnavigation[^>]*?>"," ");
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
        addReplace("<v:shape.*?</v:shape>", "");
        addReplace("<font.*?>", "");
        addReplace("</font>", "");
	addReplace("<span\\s+>", "<span>");
	addReplace("<span>\\s*</span>", "");
	addReplace("<span>\\s*</span>", "");
	addReplace("<span>(.*?)</span>", "$1");
	addReplace("<tr\\s+>", "<tr>");
	addReplace("<p\\s+>", "<p>");
	addReplace("<i *>\\s*</i>", "");
	addReplace("<i +>", "<i>");
	addReplace("<!--\\[if gte mso 9\\]>.*?<!\\[endif\\]-->","");
	//        addReplace("<td.*?>(.*?)</td>", "<td>$1</td>");

	//        addReplace("([^\\s]+)<h", "$1\n<h");
	//        addReplace(">\\n", ">");
	//        addReplace("<table", "\n<table");
	//        addReplace("<h", "\n<h");
	//        addReplace("<tr", "\n<tr");
	//        addReplace("/tr>", "/tr>\n");
	//        addReplace("/div>", "/div>\n");
	//        addReplace("<td><p>(.*?)</p></td>", "<td>$1</td>");
	//        addReplace("<p> *&nbsp; *</p>", "");

	addReplace("–", "-");
        addReplace("\\r\\n", "\n");
        addReplace("\\r", "\n");	
	//	addReplace("\\s*\\n\\s*", "\n");
	addReplace("[\\s&&[^\\n]]*\\n[\\s&&[^\\n]]*", "\n");
	//Not sure why there are two new lines but it seems to work
	addReplace("\\n\\n+", "\n");	
	addReplace("\\n{2,}", "\n");
	addReplace("\\n","\n");
    }

    private static int idCnt = 0;

    public static class Page {
        boolean isResource = false;
        Page parent = null;
        boolean processed = false;
        String id = "webharvesterpage_" + (idCnt++);
        String href;
        String hrefLabel;
        URL url;
        String title;
        String body;
        List<Page> children = new ArrayList<Page>();

        public Page(URL url) {
            isResource = true;
            this.url   = url;
            this.title =
                IOUtil.stripExtension(IOUtil.getFileTail(url.toString()));
        }

        public Page(URL url, String title, String body) {
            this.url   = url;
            this.title = title;
            this.body  = body;
        }

        public void addChild(Page child) {
            if (child.parent != null) {
                return;
            }
            children.add(child);
            child.parent = this;
        }

        public URL getUrl() {
            return url;
        }

        public boolean isResource() {
            return isResource;
        }

        public String getHref() {
            return href;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public List<Page> getChildren() {
            return children;
        }


        public String toString() {
            return title + " " + url;
        }
    }

    public static class Replace {
        String pattern;
        String with;

        public Replace(String pattern, String with) {
            this.pattern = pattern;
            this.with    = with;
        }
	public static String replace(List<Replace> list,String s) {
            for (Replace replace : list) {
		//		System.out.println("PATTERN:" + replace.pattern +" WITH:" + replace.with);
                s = s.replaceAll("(?s)(?i)"   + replace.pattern, replace.with);
            }
	    return s;
	}
    }



    private void writeEntryXml(PrintWriter          writer,Page page, Page parent) throws Exception {

        //      System.out.println("TITLE:" + page.title  +" " + page.href +" " + page.url);
        StringBuilder sb    = new StringBuilder();
        String        type  = "wikipage";
        String        attrs = "";
        if (page.isResource) {
	    if(!writeFiles) return;
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
                    log("Fetched resource:" + page.url  + " as:" + f);
                } catch (Exception exc) {
                    log("ERROR: fetching resource:" + page.url
                                       + " " + exc + " parent:"
                                       + page.parent.url);

                    return;
                }
            }
        }
        attrs += XmlUtil.attr("type", type) + XmlUtil.attr("id", page.id)
	    +XmlUtil.attr("isnew","true") 
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
	//	log("TITLE:" +page.title +" URL:"  + page.url);
        sb.append(XmlUtil.tag("name", "", XmlUtil.getCdata(page.title)));
        sb.append("\n");
        if ( !page.isResource) {
            //      String content = HtmlUtils.href(page.url.toString(),"Source") +"<br>" + page.body;
	    String contents = page.body;
	    if(addTable) {
		contents = contents +"\n\n{{tabletree  message=\"\"   prefix=\":heading Contents\" }} \n";
	    }
            String content = wrapWithSection
		? ("+section title={{name}}\n" + contents
		   + "\n-section\n")
		: contents;
            //      if(page.children.size()>0)
            //          content+="\n----\n:heading Links\n{{tree details=false}}";
            //If we are creating something other than a wikipage then set the description
            //sb.append(XmlUtil.tag("description","", XmlUtil.getCdata(content)));
            sb.append(XmlUtil.tag("wikitext", "", XmlUtil.getCdata(content)));
        }
        sb.append("</entry>");
        writer.println(sb);
        for (Page child : page.children) {
            writeEntryXml(writer,child, page);
        }

    }

    public static void usage(String msg) {
        System.err.println(msg);
        System.err.println("usage:");
        System.err.println("\t-maxdepth <depth>");
        System.err.println(
            "\t-pattern <pattern> (pattern to match links to follow)");
        System.err.println(
            "\t-replacems (clean up the extra cruft MS Word generates)");
        System.err.println("\t-doimages (download images)");
        System.err.println("\t-addtable (add entry table wiki tag)");	
        System.err.println("\t-addurl (add link to original page)");	
        System.err.println(
            "\t-replace <url pattern> <with> (convert text)");
        System.err.println(
            "\t-imagereplace <url pattern> <with> (convert image links)");
        System.err.println("\t-entries (generate the RAMADDA entry xml)");
        System.err.println("\t<url> (url to start)");
        Utils.exitTest(0);
    }

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

    public static void main(String[] args) throws Exception {
	/*
	String s = "<a href=texarc.htm <strong>TEXARC</a> - ";
	String  p ="(<a\\s+[^<>]+)\\s*<strong>";
	System.err.println("S:" + s);
	System.err.println("D:" + s.replaceAll(p,"$1>"));
	if(true) return;
	*/

	FileOutputStream logfos = new FileOutputStream("log.txt");
	PrintWriter log = new PrintWriter(logfos);
        WebHarvester harvester  = new WebHarvester(log);
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
            if (arg.equals("-nofiles")) {
		harvester.writeFiles = false;
		continue;
	    }

	    if(arg.equals("-addurl")) {
		harvester.addOriginalUrl = true;
		continue;
	    }
	    if(arg.equals("-addtable")) {
		harvester.addTable = true;
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
            if (arg.equals("-replace")) {
		harvester.addReplace(args[++i], args[++i]);
                continue;
            }
            if (arg.startsWith("-")) {
                usage("Unknown argument:" + arg);
            }
            harvester.setUrl(arg);
        }

        Page page = harvester.harvest(true, pattern, notpattern);

        if (doEntries) {
	    FileOutputStream fos = new FileOutputStream("entries.xml");
	    PrintWriter          writer    = new PrintWriter(fos);
            writer.println("<entries>");
            harvester.writeEntryXml(writer,page, null);
            writer.println("</entries>");
	    writer.close();
	    fos.close();
	}
        if (doImages) {
            harvester.fetchImages();
        }
	if(doEntries) {
	    System.err.println("RAMADDA entries.xml written");
	}
	System.err.println("#pages harvested:" + harvester.goodCnt);
	if(harvester.badUrls.size()>0) {
	    PrintWriter          w2    = new PrintWriter(new FileOutputStream("failed.txt"));
	    for(String []tuple: harvester.badUrls) {
		String url = tuple[0];
		String parent = tuple[1];
		w2.println("  from:" + parent);
		w2.println("failed:" + url);
		w2.println("");
	    }
	    w2.close();
	    System.err.println("#pages failed:" + harvester.badUrls.size() +" urls written to failed.txt");
	}

	log.close();



    }
}

/*
 * {{tree entries.filter="name:Flight.*" details=false
 * treePrefix="<h3>Flights</h3>" message="" nameTemplate="${name} - ${date}" sort="date" sortdir=up}}
 */


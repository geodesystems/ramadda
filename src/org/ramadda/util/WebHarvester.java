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

public class WebHarvester {
    private URL url;
    private List<Replace> replaces = new ArrayList<Replace>();
    private String title;
    private String body;    
    private HashSet seen = new HashSet();

    private Page page;

    private int cnt=0;

    public WebHarvester(String url) throws Exception {
	this.url = new URL(url);
    }
    

    /**
       Get the Title property.
       @return The Title
    **/
    public String getTitle () {
	return title;
    }


    /**
       Get the Body property.

       @return The Body
    **/
    public String getBody () {
	return body;
    }

    


    public void addReplace(String pattern, String with) {
	replaces.add(new Replace(pattern, with));
    }

    public Page harvest(boolean recurse, String pattern) throws Exception {
	page = harvest(url, null, null, recurse, pattern,"",0);
	return page;
    }

    public Page harvest(URL url, String label, String link,boolean recurse, String linkPattern,String indent, int depth) throws Exception {	
	//	if(depth>1) return null;
	//	if(cnt++>2) return null;
	if(seen.contains(url)) return null;
	seen.add(url);
	String surl = url.toString();
	if(Utils.isImage(surl) || surl.toLowerCase().endsWith("pdf")) {
	    System.err.println(indent+"IMAGE:" + url);
	    return new Page(url);
	}

	String html="";
	try {
	    html = IO.readContents(url.toString());
	} catch(Exception exc) {
	    System.err.println(indent+"BAD URL:"+ url +" " + label);
	    return null;
	}
	title = StringUtil.findPattern(html,"(?s)(?i)<title[^>]*>(.*?)</title>");
	if(title==null) title=IOUtil.stripExtension(IOUtil.getFileTail(url.toString()));
	//A hack for School of Mines harvest
	if(label!=null) {
	    if(title.equals("T")) title = label;
	    if(title.equals("Date")) title = label;
	}

	body = StringUtil.findPattern(html,"(?s)(?i)<body[^>]*>(.*?)</body>");
	if(body==null) body = html;
	for(Replace replace: replaces) {
	    body = body.replaceAll("(?s)(?i)" + replace.pattern,replace.with);
	}
	if(body.length()>28000) 
	    System.err.print("***** bad length ");
	//	System.err.println(indent +"URL:" + url +" title:" + title+" label:"  + label+" " +body.length());
	System.err.println(indent +"URL:" + url +" link:" + link);
	Page page = new Page(url, title,body);
	for(HtmlUtils.Link childLink: HtmlUtils.extractLinks(url, body, ".*",true)) {
	    String l = childLink.getLink().replace("\\","\\\\");
	    //	    System.err.println("IMG:" + l);
	    page.body = page.body.replaceAll(l,childLink.getUrl().toString());
	}
	//	if(true) return page;

	if(recurse) {
	    for(HtmlUtils.Link childLink: HtmlUtils.extractLinks(url, body, linkPattern)) {
		Page child = harvest(childLink.getUrl(), childLink.getLabel(),childLink.getLink(), recurse, linkPattern,indent+"   ",depth+1);
		if(child==null) continue;
		page.href=childLink.getLink();
		page.addChild(child);
		String l = childLink.getLink().replace("\\","\\\\");
		page.body = page.body.replaceAll("href=\"" + l +"\"","href=\"/repository/entry/show?entryid=" + child.id+"\"");
		page.body = page.body.replaceAll("href=\'" + l +"\'","href=\"/repository/entry/show?entryid=" + child.id+"\"");
		page.body = page.body.replaceAll("href=" + l,"href=\"/repository/entry/show?entryid=" + child.id+"\"");
	    }
	}

	return page;
    }

    public Page getPage() {
	return page;
    }

    public void addReplaceMS() {
	//Non ascii
	addReplace("[^\\x00-\\x7F]","");
	addReplace("style *= *'.*?'","");
	addReplace("class=MsoNormal","");
	addReplace("class=SpellE","");
	addReplace("class=GramE","");	
	addReplace("lang=DE","");		
	addReplace("<!--\\[if.*?\\]>","");
	addReplace("<!\\[if.*?\\]>","");
	addReplace("<!\\[endif\\]-->","");
	addReplace("<!\\[endif\\]>","");
	addReplace("<o\\:p>","");
	addReplace("</o\\:p>","");
	addReplace("<span\\s+>","<span>");
	addReplace("<span>\\s*</span>","");
	addReplace("<span>\\s*</span>","");
	addReplace("<span>(.*?)</span>","$1");
	addReplace("<tr\\s+>","<tr>");
	addReplace("<p\\s+>","<p>");
	addReplace("<v:shape.*?</v:shape>","");
	addReplace("<i *>\\s*</i>","");
	addReplace("<i +>","<i>");
	addReplace("<td.*?>(.*?)</td>","<td>$1</td>");			
	addReplace("\\r\\n","\n");
	addReplace("\\n\\s+","\n");
	addReplace(">\\n",">");
	addReplace("<td><p>(.*?)</p></td>","<td>$1</td>");
	addReplace("<table","\n<table");
	addReplace("/tr>","/tr>\n");
	addReplace("/div>","/div>\n");			
    }

    private static int idCnt=0;


    public static class Page {
	String id = "page_" + (idCnt++);
	String href;
	URL url;
	String title;
	String body;
	List<Page> children = new ArrayList<Page>();
	boolean isImage = false;
	public Page(URL url) {
	    isImage = true;
	    this.url = url;
	}


	public Page(URL url, String title, String body) {
	    this.url = url;
	    this.title = title;
	    this.body = body;
	}
	public void addChild(Page child) {
	    children.add(child);
	}
	public URL getUrl() {
	    return url;
	}
	public boolean isImage() {
	    return isImage;
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

	public 	List<Page> getChildren() {
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
	    this.with = with;
	}
    }

    private static void writeEntryXml(Page page, Page parent) throws Exception {
	if(page.isImage) return;
	//	System.out.println("TITLE:" + page.title  +" " + page.href +" " + page.url);
	StringBuilder sb = new StringBuilder();
	String content = page.body;
	if(page.children.size()>0)
	    content+="\n----\n:heading Links\n{{tree details=false}}";
	sb.append(XmlUtil.openTag("entry",XmlUtil.attr("type","group") + XmlUtil.attr("id",page.id) + (parent==null?"":XmlUtil.attr("parent",parent.id))));
	sb.append("\n");
	sb.append(XmlUtil.tag("name","", XmlUtil.getCdata(page.title)));
	sb.append("\n");
	sb.append(XmlUtil.tag("description","", XmlUtil.getCdata("<wiki>\n+section title={{name}}\n"+content+"\n-section\n")));	
	sb.append("</entry>");	
	System.out.println(sb);
	for(Page child: page.children) {
	    if(page.isImage) continue;
	    writeEntryXml(child, page);
	}
    }

    public static void main(String[]args) throws Exception {
	WebHarvester harvester = new WebHarvester(args[0]);
	harvester.addReplaceMS();
	Page page = harvester.harvest(true,".*/t28/.*");
	//	System.out.println(page.body);
	//	if(true) return;
	System.out.println("<entries>");
	writeEntryXml(page,null);
	System.out.println("</entries>");

    }

}

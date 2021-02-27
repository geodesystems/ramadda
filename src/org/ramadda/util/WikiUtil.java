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


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import org.json.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.*;
import java.util.regex.*;


/**
 */
public class WikiUtil {


    private static final HtmlUtils HU = null;

    /** _more_ */
    public static final String ATTR_OPEN = "open";

    /** _more_ */
    public static final String ATTR_VAR = "var";

    /** _more_ */
    public static final String ATTR_DECORATE = "decorate";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_SHOW = "show";

    /** _more_ */
    public static final String PROP_NOHEADING = "noheading";

    /** _more_ */
    public static final String PROP_NOP = "nop";

    /** _more_ */
    public static final String PROP_DOP = "dop";

    /** _more_ */
    public static final String PROP_HEADING = "heading";

    /** _more_ */
    public static final String TAG_PREFIX = "{{";

    /** _more_ */
    public static final String TAG_SUFFIX = "}}";


    /** _more_ */
    private static Hashtable globalProperties;

    /** _more_ */
    private Hashtable properties;

    /** _more_ */
    private Hashtable wikiProperties = new Hashtable();

    /** _more_ */
    private Hashtable wikiAttributes = new Hashtable();

    /** _more_ */
    private StringBuilder js = new StringBuilder();

    /** _more_ */
    private Hashtable<String, String> myVars = new Hashtable<String,
	String>();

    /** _more_ */
    private boolean hasSet = false;

    /** _more_ */
    private List categoryLinks = new ArrayList();

    /** _more_ */
    private List floatBoxes = new ArrayList();

    /** _more_ */
    private boolean makeHeadings = false;

    /** _more_ */
    private boolean replaceNewlineWithP = true;


    /** _more_ */
    private boolean mobile = false;

    /** _more_ */
    private String user;

    /** _more_ */
    private String[] notTags;


    private WikiPageHandler handler;


    /**
     * _more_
     */
    public WikiUtil() {}

    /**
     * _more_
     *
     * @param that _more_
     */
    public WikiUtil(WikiUtil that) {
        this.properties = that.properties;
    }


    /**
     * _more_
     *
     * @param properties _more_
     */
    public WikiUtil(Hashtable properties) {
        this.properties = properties;
    }

    /**
     * _more_
     *
     * @param code _more_
     */
    public void appendJavascript(String code) {
        if ((code == null) || (code.trim().length() == 0)) {
            return;
        }
        js.append(code);
        js.append("\n");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getJavascript() {
        return js.toString();
    }

    /**
     * Set the GlobalProperties property.
     *
     * @param value The new value for GlobalProperties
     */
    public static void setGlobalProperties(Hashtable value) {
        globalProperties = value;
    }

    /**
     * Get the GlobalProperties property.
     *
     * @return The GlobalProperties
     */
    public static Hashtable getGlobalProperties() {
        return globalProperties;
    }



    /**
     * _more_
     *
     * @param key _more_
     */
    public void removeProperty(Object key) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.remove(key);

    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putWikiProperty(Object key, Object value) {
	if(value==null)
	    wikiProperties.remove(key);
	else
	    wikiProperties.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getWikiProperty(Object key) {
        return wikiProperties.get(key);
    }


    public Object getWikiProperty(Hashtable props,Object...args) {
	Object dflt = args[args.length-1];
	for(int i=0;i<args.length-1;i++) {
	    Object key = args[i];
	    Object v = props.get(key);
	    if(v==null) v = getWikiProperty(key);
	    if(v!=null) return v;
	}
	return dflt;
    }


    /**
     * _more_
     *
     * @param key _more_
     */
    public void removeWikiProperty(Object key) {
        wikiProperties.remove(key);
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public void putWikiAttribute(Object key, Object value) {
        wikiAttributes.put(key, value);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Hashtable getWikiAttributes() {
        return wikiAttributes;
    }

    /**
     * _more_
     */
    public void clearWikiAttributes() {
        wikiAttributes = new Hashtable();
    }


    /**
     * _more_
     *
     * @param l _more_
     *
     * @throws Exception _more_
     */
    public void addWikiAttributes(List l) throws Exception {
        for (Enumeration keys =
		 wikiAttributes.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) wikiAttributes.get(key);
            l.add(key);
            l.add(Json.quote(value));
        }
    }

    /**
     * _more_
     *
     * @param link _more_
     */
    public void addCategoryLink(String link) {
        categoryLinks.add(link);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public Object getProperty(Object key) {
        if (properties == null) {
            return null;
        }

        return properties.get(key);
    }

    /**
     * _more_
     *
     * @param property _more_
     *
     * @return _more_
     */
    public String getInfoBox(String property) {
        StringBuffer sb = new StringBuffer();
        List<String> toks = (List<String>) StringUtil.split(property, "\n",
							    true, true);
        String firstLine = toks.get(0);
        toks.remove(0);
        /*
          {{Infobox file format
          | name = Network Common Data Form
          | icon =
          | extension = .nc<br/>.cdf
          | mime = application/netcdf<br/>application/x-netcdf
          | owner = [[University Corporation for Atmospheric Research|UCAR]]
          | typecode =
          | magic = CDF\001
          | genre = scientific binary data
          | containerfor =
          | containedby =
          | extendedfrom = [[Common Data Format|CDF]]
          | extendedto =
          | standard =
          }}
        */
        sb.append(HU.open(HU.TAG_TABLE));
        String title = "";
        for (String line : toks) {
            String[] toks2 = StringUtil.split(line, "=", 2);
            if (toks2 == null) {
                continue;
            }
            String name = toks2[0].trim();
            if (name.startsWith("|")) {
                name = name.substring(1).trim();
            }
            if (name.equals("name")) {
                title = toks2[1].trim();
            } else if (toks2[1].trim().length() > 0) {
                sb.append(
			  HU.rowTop(
				    HU.col(
					   name,
					   HU.cssClass(
						       "wiki-infobox-entry-title")) + HU.col(
											     toks2[1],
											     HU.cssClass(
													 "wiki-infobox-entry"))));

            }
        }
        sb.append(HU.close(HU.TAG_TABLE));
        String div = HU.makeShowHideBlock(title, sb.toString(), true,
					  HU.cssClass("wiki-infobox-title"),
					  HU.cssClass("wiki-infobox"));
        floatBoxes.add(wikify(div, null));
        return "";
    }

    /**
     * _more_
     *
     * @param property _more_
     *
     * @return _more_
     */
    public String getPropertyValue(String property) {
        if (property.startsWith("Infobox")) {
            return getInfoBox(property);
        }

        return null;
    }



    /**
     * _more_
     *
     *
     * @param text _more_
     * @param handler _more_
     *
     * @return _more_
     */
    public String wikify(String text, WikiPageHandler handler) {
	this.handler = handler;
        return wikify(text, handler, null);
    }


    
    /**
     * _more_
     *
     * @param text _more_
     * @param handler _more_
     * @param notTags _more_
     *
     * @return _more_
     */
    public String wikify(String text, WikiPageHandler handler,
                         String[] notTags) {
        try {
            this.notTags = notTags;
            StringBuffer mainBuffer = new StringBuffer();
	    this.handler = handler;
            wikify(mainBuffer, text, handler, notTags);

            return mainBuffer.toString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public WikiPageHandler getHandler() {
	return handler;
    }


    /**
     * _more_
     *
     * @param mainBuffer _more_
     * @param text _more_
     * @param handler _more_
     *
     * @throws IOException _more_
     */
    public void wikify(Appendable mainBuffer, String text,
                       WikiPageHandler handler)
	throws IOException {
        wikify(mainBuffer, text, handler, new String[] {});
    }


    /**
     * _more_
     *
     * @param mainBuffer _more_
     * @param text _more_
     * @param handler _more_
     * @param notTags _more_
     *
     * @throws IOException _more_
     */
    public void wikify(Appendable mainBuffer, String text,
                       WikiPageHandler handler, String[] notTags)
	throws IOException {
        if (text.startsWith("<wiki>")) {
            text = text.substring("<wiki>".length());
        }
        List<Chunk> chunks = Chunk.splitText(text);
        String      s      = wikifyInner(chunks, handler, notTags);
        mainBuffer.append(s);
    }




    /**
     * _more_
     *
     * @param handler _more_
     * @param headings _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    private String applyPatterns(WikiPageHandler handler, List headings,
                                 String s)
	throws IOException {
        if (getReplaceNewlineWithP()) {
            s = s.replaceAll("\r\n\r\n", "\n<p></p>\n");
            s = s.replaceAll("\r\r", "\n<p></p>\n");
        }

        s = s.replace("\\\\[", "_BRACKETOPEN_");
        s = s.replaceAll("''''(.*?)''''", "<b><i>$1</i></b>");
        s = s.replaceAll("'''(.*?)'''", "<b>$1</b>");
        s = s.replaceAll("''(.*?)''", "<i>$1</i>");
        Pattern pattern1 =
            Pattern.compile("\\[\\[([^\\]|]+)\\|?([^\\]]*)\\]\\]");
        Matcher matcher1 = pattern1.matcher(s);
        while (matcher1.find()) {
            String name  = matcher1.group(1);
            String label = matcher1.group(2);
            int    start = matcher1.start(0);
            int    end   = matcher1.end(0);
            String link;
            if (handler == null) {
                if (label.trim().length() == 0) {
                    label = name;
                }
                link = "<a href=\"" + name + "\">" + label + "</a>";
            } else {
                link = handler.getWikiLink(this, name, label);
            }
            s        = s.substring(0, start) + link + s.substring(end);
            matcher1 = pattern1.matcher(s);
        }

        int     cnt      = 0;
        Pattern pattern2 = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher matcher2 = pattern2.matcher(s);
        while (matcher2.find()) {
            String name  = matcher2.group(1).trim();
            int    idx   = name.indexOf(" ");
            int    start = matcher2.start(0);
            int    end   = matcher2.end(0);
            if (idx > 0) {
                String label = name.substring(idx);
                name = name.substring(0, idx);
                String ahref =
                    "<a title='" + name
                    + "' class='wiki-link-external' target='_blank' href='"
                    + name + "'>";
                s = s.substring(0, start) + ahref + label + "</a>"
                    + s.substring(end);
            } else {
                cnt++;
                String ahref =
                    "<a title='" + name
                    + "' class='wiki-link-external' target='_blank' href='"
                    + name + "'>";
                s = s.substring(0, start) + ahref + "_BRACKETOPEN_" + cnt
                    + "_BRACKETCLOSE_</a>" + s.substring(end);
            }
            matcher2 = pattern2.matcher(s);
        }


        Pattern pattern3 = Pattern.compile("(?m)^\\s*(==+)([^=]+)(==+)\\s*$");
        Matcher matcher3 = pattern3.matcher(s);
        while (matcher3.find()) {
            String prefix = matcher3.group(1).trim();
            String label  = matcher3.group(2).trim();
            //            System.err.println("MATCH " + prefix + ":" + label);
            int    start = matcher3.start(0);
            int    end   = matcher3.end(0);
            int    level = prefix.length();
            String value;

            if (label.startsWith(TAG_PREFIX)) {
                value = "<div class=\"wiki-h" + level + "\">" + label
		    + "</div>";
            } else {
                value = "<a name=\"" + label + "\"></a><div class=\"wiki-h"
		    + level + "\">" + label + "</div>";
                //            if(level==1)
                //                value = value+"<hr class=\"wiki-hr\">";
                headings.add(new Object[] { new Integer(level), label });
            }
            s        = s.substring(0, start) + value + s.substring(end);
            matcher3 = pattern3.matcher(s);
        }

        s = s.replaceAll("\r", "");

        return s;
    }

public interface TriConsumer<T, U, V> {
     void accept(T t, U u, V v);
}


    /**
     * _more_
     *
     * @param s _more_
     *
     * @param chunks _more_
     * @param handler _more_
     * @param notTags _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    private String wikifyInner(List<Chunk> chunks, WikiPageHandler handler,
                               String[] notTags)
	throws IOException {

        List                 headings        = new ArrayList();
        List                 headings2        = new ArrayList();
        String headingsNav = null;
	Hashtable headingsProps=null;

	TriConsumer<StringBuffer,String,Integer > defineHeading = (sb,label,level) ->{
	    String id = Utils.makeID(label);
	    //	    String id = "heading_" + HU.blockCnt++;
	    headings2.add(new Object[]{id, label,level});
	    sb.append("<a class=ramadda-nav-anchor name='" + id +"'></a>");
	};
	



	
        boolean              closeTheTag     = false;
        int                  ulCnt           = 0;
        int                  olCnt           = 0;
        StringBuffer         buff            = new StringBuffer();
        StringBuilder        js              = new StringBuilder("\n");
        List<TabState>       allTabStates    = new ArrayList<TabState>();
        List<TabState>       tabStates       = new ArrayList<TabState>();
        List<RowState>       rowStates       = new ArrayList<RowState>();
        List<AccordionState> accordionStates =
            new ArrayList<AccordionState>();

        List<TableState> tableStates       = new ArrayList<TableState>();
        String           currentVar        = null;
        StringBuilder    currentVarValue   = null;
        boolean          inScroll          = false;
        String           afterId           = null;
        String           afterPause        = null;
        String           afterFade         = null;
        boolean          inPropertyTag     = false;
        String           dragId            = null;
        boolean          dragToggle        = false;
        boolean          dragToggleVisible = false;

        for (Chunk chunk : chunks) {
            if (chunk.type == chunk.TYPE_CODE) {
                handleCode(buff, chunk, handler);
                continue;
            }
            if (chunk.type == chunk.TYPE_NOWIKI) {
                buff.append(chunk.chunk);
                continue;
            }
            if (chunk.type == chunk.TYPE_CSS) {
                buff.append("<style type='text/css'>\n");
                buff.append(chunk.chunk);
                buff.append("</style>\n");
                continue;
            }
            if (chunk.type == chunk.TYPE_JS) {
                buff.append("\n<script type='text/JavaScript'>\n");
                buff.append(chunk.chunk);
                buff.append("\n</script>\n");
                continue;
            }
            if (chunk.type == chunk.TYPE_PRE) {
                buff.append("\n<pre>");
		String s = chunk.chunk.toString().replaceAll("\\{\\{","{<noop>{");
                buff.append(s);
                buff.append("\n</pre>\n");
                continue;
            }
            String text = chunk.chunk.toString();
            text = applyPatterns(handler, headings, text);

            for (String line : text.split("\n")) {
                if ((line.indexOf("${") >= 0)
		    && (hasSet || (globalProperties != null))) {
                    if (myVars != null) {
                        for (java.util.Enumeration keys = myVars.keys();
			     keys.hasMoreElements(); ) {
                            Object key   = keys.nextElement();
                            Object value = myVars.get(key);
                            line = line.replace("${" + key + "}",
						value.toString());
                        }
                    }
                    if (globalProperties != null) {
                        for (java.util.Enumeration keys =
				 globalProperties.keys();
			     keys.hasMoreElements(); ) {
                            Object key   = keys.nextElement();
                            Object value = globalProperties.get(key);
                            line = line.replace("${" + key + "}",
						value.toString());
                        }
                    }
                }

                String tline = line.trim();

                if (tline.startsWith("{{")) {
                    buff.append(tline);
                    buff.append("\n");
                    if (tline.indexOf("}}") < 0) {
                        inPropertyTag = true;
                    }
                    continue;
                }

                if (inPropertyTag && tline.endsWith("}}")) {
                    buff.append(tline);
                    buff.append("\n");
                    inPropertyTag = false;
                    continue;
                }

                if (inPropertyTag) {
                    buff.append(tline);
                    buff.append("\n");
                    continue;
                }

                if (tline.startsWith("@(")) {
                    handleEmbed(buff, tline);
                    continue;
                }

                if (tline.startsWith(":property")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 3);
		    if(toks.size()>2)
			putWikiProperty(toks.get(1),toks.get(2));
		    else
			putWikiProperty(toks.get(1),null);
			
		    continue;
		}


                if (tline.startsWith(":macro")) {
                    hasSet = true;
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 3);
                    String       var   = ((toks.size() > 1)
                                          ? toks.get(1)
                                          : "");
                    String       value = ((toks.size() > 2)
                                          ? toks.get(2)
                                          : "");
                    myVars.put(var.trim(), value.trim());
                    continue;
                }

                if (tline.startsWith("+macro")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 3);
                    currentVar      = ((toks.size() > 1)
                                       ? toks.get(1)
                                       : "");
                    currentVarValue = new StringBuilder();
                    continue;
                }

                if (tline.startsWith("-macro")) {
                    myVars.put(currentVar, currentVarValue.toString());
                    currentVar      = null;
                    currentVarValue = null;
                    continue;
                }


                if (currentVar != null) {
                    currentVarValue.append(tline);
                    continue;
                }

                if (tline.startsWith("+table")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    String       width     = "100%";
                    String       height    = null;
                    String       ordering  = null;
                    String       paging    = null;
                    String       xclazz    = null;
                    String       searching = "false";
                    String       clazz     = "ramadda-table";
                    if (toks.size() == 2) {
                        Hashtable props =
                            HU.parseHtmlProperties(toks.get(1));
                        width  = Utils.getProperty(props, "width", width);
                        height = Utils.getProperty(props, "height", height);
                        ordering = Utils.getProperty(props, "ordering",
						     ordering);
                        paging = Utils.getProperty(props, "paging", paging);
                        searching = Utils.getProperty(props, "searching",
						      height);

                        if (Misc.equals(Utils.getProperty(props, "rowborder",
							  null), "true")) {
                            clazz = "row-border " + clazz;
                        }
                        if (Misc.equals(Utils.getProperty(props,
							  "cellborder", null), "true")) {
                            clazz = "cell-border " + clazz;
                        }
                        if (Misc.equals(Utils.getProperty(props, "stripe",
							  null), "true")) {
                            clazz = "stripe " + clazz;
                        }
                        if (Misc.equals(Utils.getProperty(props, "hover",
							  null), "true")) {
                            clazz = "hover " + clazz;
                        }
                    }

                    buff.append("<table class='" + clazz + "' width=" + width
                                + " table-searching=" + searching + " "
                                + ((height != null)
                                   ? " table-height=" + height
                                   : "") + ((ordering != null)
                                            ? " table-ordering=" + ordering
                                            : "") + ((paging != null)
						     ? " table-paging=" + paging
						     : "") + "><thead>");
                    tableStates.add(new TableState());
                    continue;
                }
                if (tline.equals("-table")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
                        buff.append("Not in a table");
                        continue;
                    }
                    if (state.inTd) {
                        buff.append("</td>");
                    }
                    if (state.inTr) {
                        buff.append("</tr>");
                    }
                    if (state.inHead) {
                        buff.append("</thead>");
                    }
                    if (state.inBody) {
                        buff.append("</tbody>");
                    }
                    buff.append("</table>");
                    tableStates.remove(tableStates.size() - 1);
                    continue;
                }



                if (tline.startsWith(":tr")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
                        buff.append("Not in a table");
                        continue;
                    }
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    buff.append("<tr valign=top>");
                    if (toks.size() == 2) {
                        for (String td :
				 Utils.parseCommandLine(toks.get(1))) {
                            if (state.inHead) {
                                buff.append(HU.th(td));
                            } else {
                                buff.append(HU.td(td));
                            }
                        }
                    }
                    if (state.inHead) {
                        buff.append("</thead>");
                        buff.append("<tbody>");
                        state.inHead = false;
                        state.inBody = true;
                    }
                    continue;
                }
                if (tline.startsWith("+tr")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
                        buff.append("Not in a table");
                        continue;
                    }
                    buff.append("<tr valign=top>");
                    continue;
                }
                if (tline.startsWith("-tr")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
			buff.append("Not in a table");
                        continue;
                    }
                    buff.append("</tr>");
                    if (state.inHead) {
                        buff.append("</thead>");
                        buff.append("<tbody>");
                        state.inHead = false;
                        state.inBody = true;
                    }
                    continue;
                }

                if (tline.startsWith("+td")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
                        buff.append("Not in a table");
                        continue;
                    }
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       width = null;
                    if (toks.size() == 2) {
                        Hashtable props =
                            HU.parseHtmlProperties(toks.get(1));
                        width = Utils.getProperty(props, "width", width);
                    }

                    if (state.inHead) {
                        buff.append("<th " + ((width != null)
					      ? " width=" + width
					      : "") + ">");
                    } else {
                        buff.append("<td valign=top " + ((width != null)
							 ? " width=" + width
							 : "") + ">");
                    }
                    continue;
                }
                if (tline.startsWith("-td")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
                        buff.append("Not in a table");
                        continue;
                    }
                    if (state.inHead) {
                        buff.append("</th>");
                    } else {
                        buff.append("</td>");
                    }
                    continue;
                }
                if (tline.startsWith(":td")) {
                    TableState state = (tableStates.size() > 0)
			? tableStates.get(tableStates.size()
					  - 1)
			: null;
                    if (state == null) {
                        buff.append("Not in a table");
                        continue;
                    }
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    String       td   = (toks.size() == 2)
			? toks.get(1)
			: "";
                    if (state.inHead) {
                        buff.append(HU.th(td));
                    } else {
                        buff.append(HU.td(td, "valign=top"));
                    }
                    continue;
                }


                if (tline.startsWith("+tabs")) {
                    TabState     tabInfo  = new TabState();
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    String       divClass = "";
                    if (toks.size() == 2) {
                        Hashtable props =
                            HU.parseHtmlProperties(toks.get(1));
                        if (props.get("min") != null) {
                            divClass = "ramadda-tabs-min";
                        } else if (props.get("center") != null) {
                            divClass = "ramadda-tabs-center";
                        } else if (props.get("minarrow") != null) {
                            divClass =
                                "ramadda-tabs-min ramadda-tabs-minarrow";
                        }
                        if (props.get("transparent") != null) {
                            divClass += " ramadda-tabs-transparent ";
                        }

                        tabInfo.minHeight = (String) props.get("minHeight");
                        if (tabInfo.minHeight != null) {
                            tabInfo.minHeight = getSize(tabInfo.minHeight);
                        }
                    }
                    tabStates.add(tabInfo);
                    allTabStates.add(tabInfo);
                    buff.append("\n");
                    HU.open(buff, HU.TAG_DIV, "class",
			    divClass);
                    HU.open(buff, HU.TAG_DIV, "id", tabInfo.id,
			    "class", "ui-tabs");
                    buff.append("\n");
                    HU.open(tabInfo.title, HU.TAG_UL);
                    tabInfo.title.append("\n");
                    buff.append("\n");
                    buff.append("${" + tabInfo.id + "}");
                    buff.append("\n");
                    continue;
                }
                if (tline.equals("+tab") || tline.startsWith("+tab ")) {
                    if (tabStates.size() == 0) {
                        buff.append("No +tabs tag");
                        continue;
                    }
                    List<String> toks    = StringUtil.splitUpTo(tline, " ",
								2);
                    String       title   = (toks.size() > 1)
			? toks.get(1)
			: "";
                    TabState     tabInfo = tabStates.get(tabStates.size()
							 - 1);
                    tabInfo.cnt++;
                    tabInfo.title.append("<li><a href=\"#" + tabInfo.id + "-"
                                         + (tabInfo.cnt) + "\">" + title
                                         + "</a></li>\n");
                    String style = "";
                    if (tabInfo.minHeight != null) {
                        style = " style=min-height:" + tabInfo.minHeight
			    + ";";
                    }
                    buff.append(HU.open("div",
					style
					+ HU.id(tabInfo.id + "-" + (tabInfo.cnt))
					+ HU.cssClass("ui-tabs-hide")));
                    buff.append("\n");
                    continue;
                }
                if (tabStates.size() > 0) {
                    if (tline.equals("-tab")) {
                        TabState tabInfo = tabStates.get(tabStates.size()
							 - 1);
                        buff.append(HU.close("div"));
                        buff.append("\n");
                        js.append(
				  "jQuery(function(){\njQuery('#" + tabInfo.id
				  + "').tabs({activate: HtmlUtil.tabLoaded})});\n");
                        continue;
                    }
                    if (tline.equals("-tabs")) {
                        TabState tabInfo = tabStates.get(tabStates.size()
							 - 1);
                        tabInfo.title.append("\n");
                        tabInfo.title.append("</ul>");
                        tabInfo.title.append("\n");
                        tabStates.remove(tabStates.size() - 1);
                        buff.append(HU.close("div"));
                        buff.append(HU.close("div"));
                        continue;
                    }


                }

                if (tline.startsWith("+accordian")
		    || tline.startsWith("+accordion")) {
                    AccordionState accordionState = new AccordionState();
                    accordionStates.add(accordionState);
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    String       divClass = "";
                    if (toks.size() == 2) {
                        Hashtable props =
                            HU.parseHtmlProperties(toks.get(1));
                        accordionState.activeSegment =
                            Misc.getProperty(props, "activeSegment", 0);
                        accordionState.animate = Misc.getProperty(props,
								  "animate", accordionState.animate);
                        accordionState.heightStyle = Misc.getProperty(props,
								      "heightStyle", accordionState.heightStyle);
                        accordionState.collapsible = Misc.getProperty(props,
								      "collapsible", accordionState.collapsible);
                        accordionState.decorate = Misc.getProperty(props,
								   "decorate", accordionState.decorate);
                    }

                    buff.append("\n");
                    buff.append(HU.open(HU.TAG_DIV,
					HU.cssClass(" ui-accordion ui-widget ui-helper-reset")
					+ HU.id(accordionState.id)));
                    buff.append("\n");
                    continue;
                }

                if (tline.startsWith("-accordian")
		    || tline.startsWith("-accordion")) {
                    if (accordionStates.size() == 0) {
                        buff.append("No open accordion tag");
                        continue;
                    }
                    buff.append("\n");
                    buff.append("</div>");
                    buff.append("\n");
                    AccordionState accordionState =
                        accordionStates.get(accordionStates.size() - 1);
                    accordionStates.remove(accordionStates.size() - 1);
                    String args = "{heightStyle: \""
			+ accordionState.heightStyle + "\""
			+ ", collapsible: "
			+ accordionState.collapsible + ", active: "
			+ accordionState.activeSegment
			+ ", decorate: " + accordionState.decorate
			+ ", animate:" + accordionState.animate
			+ "}";
                    js.append("HtmlUtil.makeAccordion(\"#"
                              + accordionState.id + "\" " + "," + args
                              + ");\n");
                    buff.append("\n");

                    continue;
                }

                if (tline.startsWith("+segment")) {
                    if (accordionStates.size() == 0) {
                        buff.append("No open accordion tag");

                        continue;
                    }
                    AccordionState accordionState =
                        accordionStates.get(accordionStates.size() - 1);
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       title = (toks.size() > 1)
			? toks.get(1)
			: "";
                    buff.append("\n");
                    buff.append(HU.open(HU.TAG_H3,
					HU.cssClass(" ui-accordion-header ui-helper-reset ui-corner-top")
					+ (accordionState.decorate
					   ? ""
					   : " style=\"border:0px;background:none;\" ")));
                    buff.append("\n");
                    buff.append("<a href=\"#\">");
                    buff.append(title);
                    buff.append("</a></h3>");
                    buff.append("\n");
                    String contentsId =
                        HU.getUniqueId("accordion_contents_");
                    buff.append(
				HU.open(
					"div",
					HU.id(contentsId)
					+ HU.cssClass(
						      "ramadda-accordion-contents")));
                    buff.append("\n");
                    accordionState.segmentId++;

                    continue;
                }

                if (tline.startsWith("-segment")) {
                    buff.append("\n");
                    buff.append("</div>");
                    buff.append("\n");

                    continue;
                }

                if (tline.equals("-div")) {
                    buff.append("</div>");

                    continue;
                }
                if (tline.startsWith("+pagehead")) {
                    String weight = StringUtil.findPattern(tline,
							   "-([0-9]+)");
                    if (weight == null) {
                        weight = "8";
                    }
                    buff.append("<div class=\"row\">");
                    buff.append("<div class=\"col-md-" + weight
                                + "  ramadda-col\">");
                    buff.append("<div class=\"jumbotron\">");

                    continue;
                }
                if (tline.startsWith("-pagehead")) {
                    buff.append("</div></div></div>");

                    continue;
                }
                if (tline.equals("+jumbo")) {
                    buff.append("<div class=\"jumbotron\">");

                    continue;
                }
                if (tline.equals("-jumbo")) {
                    buff.append("</div>");

                    continue;
                }

                if (tline.startsWith("+inset")) {
                    List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                    StringBuilder extra = new StringBuilder();
                    if (toks.size() > 1) {
                        StringBuilder styles = new StringBuilder();
                        for (String side : new String[] { "top", "left",
							  "bottom", "right" }) {
                            String v = getAttribute(tline, side);
                            if (v != null) {
                                v = getSize(v);
                                styles.append("margin-" + side + ":" + v
					      + ";");
                            }
                        }

                        if (styles.length() > 0) {
                            extra.append(HU.style(styles.toString()));
                        }
                    }

                    buff.append(HU.open("div",
					HU.cssClass("inset") + extra));

                    continue;
                }
                if (tline.equals("-inset")) {
                    buff.append("</div>");

                    continue;
                }

                if (tline.startsWith("+div")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       style = "";
                    String       clazz = "";
                    if (toks.size() == 2) {
                        Hashtable props =
                            HU.parseHtmlProperties(toks.get(1));
                        String tmp = (String) props.get("class");
                        if (tmp != null) {
                            clazz = tmp;
                        }
                        style = (String) props.get("style");
                        if (style == null) {
                            style = "";
                        }
                        String image = (String) props.get("image");
                        if (image != null) {
                            String attach = (String) props.get("attach");
                            if (attach == null) {
                                attach = "fixed";
                            }
                            tmp = handler.getWikiImageUrl(this, image, props);
                            if (tmp != null) {
                                image = tmp;
                            }
                            style +=
                                " background-repeat: repeat-y;background-attachment:"
                                + attach
                                + ";background-size:100% auto; background-image: url('"
                                + image + "'); ";
                        }
                        String bg = (String) props.get("background");
                        if (bg != null) {
                            style += " background: " + bg + "; ";
                        }
                    }
                    buff.append(HU.open(HU.TAG_DIV,
					HU.cssClass(clazz)
					+ HU.style(style)));

                    continue;
                }
                if (tline.startsWith("-div")) {
                    buff.append(HU.close(HU.TAG_DIV));

                    continue;
                }

                if (tline.startsWith("+gridboxes")) {
                    tline = tline.substring(1);
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    List<String> toks2 = StringUtil.splitUpTo(toks.get(0),
							      "-", 2);
                    String clazz = "";
                    if (toks2.size() > 1) {
                        clazz = "ramadda-gridboxes-" + toks2.get(1);
                    }
                    buff.append(HU.open(HU.TAG_DIV,
					HU.cssClass("ramadda-gridboxes "
						    + clazz)));

                    continue;
                }


                if (tline.startsWith("-gridboxes")) {
                    buff.append(HU.close(HU.TAG_DIV));

                    continue;
                }

                if (tline.startsWith("+gridbox")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    buff.append(HU.open(HU.TAG_DIV,
					HU.cssClass("ramadda-gridbox")));
                    if (toks.size() > 1) {
                        buff.append(HU.tag(HU.TAG_DIV,
					   HU.cssClass("ramadda-gridbox-header"),
					   toks.get(1)));
                    }
                    buff.append(HU.open(HU.TAG_DIV,
					HU.cssClass("ramadda-gridbox-contents")));

                    continue;
                }


                if (tline.startsWith("-gridbox")) {
                    buff.append(HU.close(HU.TAG_DIV));
                    buff.append(HU.close(HU.TAG_DIV));

                    continue;
                }

                if (tline.startsWith("+after")) {
                    afterId = HU.getUniqueId("after");
                    Hashtable props = lineToProps(tline);
                    afterPause = (String) props.get("pause");
                    if (afterPause == null) {
                        afterPause = "0";
                    }
                    afterFade = (String) props.get("afterFade");
                    if (afterFade == null) {
                        afterPause = "3000";
                    }
                    buff.append(HU.open(HU.TAG_DIV,
					HU.style("opacity:0;")
					+ HU.id(afterId)));

                    continue;
                }

                if (tline.startsWith("-after")) {
                    HU.close(buff, HU.TAG_DIV);
                    HU.script(buff,
			      "HU.callWhenScrolled('" + afterId
			      + "',()=>{$('#" + afterId + "').fadeTo("
			      + afterFade + ",1.0);}," + afterPause
			      + ");");

                    continue;
                }


                if (tline.startsWith("+draggable")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                    dragId = HU.getUniqueId("draggable");

                    dragToggle = Utils.getProperty(props, "toggle", false);
                    dragToggleVisible = Utils.getProperty(props,
							  "toggleVisible", true);
                    String style  = (String) props.get("style");
                    String header = (String) props.get("header");
                    String clazz  = "ramadda-draggable";
                    if (Misc.equals("true", props.get("framed"))) {
                        clazz = "ramadda-draggable-frame";
                    }
                    if (style == null) {
                        style = "";
                    }
                    HU.open(buff, "div", "id", dragId, "style",
			    "display:inline-block;z-index:1000;"
			    + style);
                    if (header != null) {
                        if (dragToggle) {
                            header = HU.image("", "id",
					      dragId + "_img") + " " + header;
                        }
                        HU.div(buff, header,
			       HU.attrs("class",
					"ramadda-draggable-header"));
                    }
                    HU.open(buff, "div", "class", clazz, "id",
			    dragId + "_frame");
                    if (dragToggle) {
                        HU.script(buff,
				  "HU.makeToggle('" + dragId
				  + "_img','" + dragId + "_frame',"
				  + dragToggleVisible + ");");
                    }

                    continue;
                }

                if (tline.startsWith("-draggable")) {
                    if (dragId != null) {
                        HU.close(buff, "div");
                        HU.close(buff, "div");
                        //              HU.script(buff, "$('#" + dragId +"').draggable();\n");
                        HU.script(buff,
				  "HU.makeDraggable('#"
				  + dragId + "');\n");
                    }

                    continue;
                }

                if (tline.startsWith("+expandable")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                    dragId = HU.getUniqueId("expandable");
                    String  header = (String) props.get("header");
                    String  clazz  = "ramadda-expandable";
                    String  clazz2 = "";
                    boolean expand = Misc.equals(props.get("expand"), "true");
                    if (expand) {
                        clazz2 += " ramadda-expand-now";
                    }
                    if (Misc.equals("true", props.get("framed"))) {
                        clazz = "ramadda-expandable-frame";
                    }
                    HU.open(buff, "div", "id", dragId, "style",
			    "position:relative;", "class", clazz2);
                    if (header != null) {
                        HU.div(buff, header,
			       HU.attrs("class",
					"ramadda-expandable-header"));
                    }
                    HU.open(buff, "div", "class", clazz);

                    continue;
                }

                if (tline.startsWith("-expandable")) {
                    if (dragId != null) {
                        HU.close(buff, "div");
                        HU.close(buff, "div");
                        //              HU.script(buff, "$('#" + dragId +"').expandable();\n");
                        HU.script(buff,
				  "HU.makeExpandable('#"
				  + dragId + "');\n");
                    }

                    continue;
                }

                if (tline.startsWith("+section")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");

                    String       tag       = toks.get(0).substring(1);
                    List<String> toks2     = StringUtil.splitUpTo(tag, "-",
								  2);
                    String       remainder = ((toks2.size() > 1)
					      ? toks2.get(1)
					      : "");

                    String       baseClass = "ramadda-section";
                    if (remainder.length() > 0) {
                        baseClass = baseClass + "-" + remainder;
                    }

                    String  label       = (String) props.get("label");
                    String  heading     = (String) props.get("heading");
                    String  title       = (String) props.get("title");
                    String  subTitle    = (String) props.get("subTitle");
                    String  classArg    = (String) props.get("class");
                    String  style       = (String) props.get("style");
                    String  titleStyle  = (String) props.get("titleStyle");
                    String  headerStyle = (String) props.get("headerStyle");
                    boolean doBorderTop = tline.indexOf("----") >= 0;
                    boolean doEvenOdd   = tline.indexOf("#") >= 0;
                    String  extraClass  = "";
                    if (doBorderTop) {
                        if (style == null) {
                            style =
                                "border-top:1px rgb(224, 224, 224) solid;";
                        } else {
                            style +=
                                "border-top:1px rgb(224, 224, 224) solid;";
                        }
                    }
                    String extraAttr = ((style == null)
                                        ? ""
                                        : " style=\"" + style + "\" ");

                    if (doEvenOdd) {
                        Integer scnt  = (Integer) getProperty("section-cnt");
                        boolean first = false;
                        if (scnt == null) {
                            scnt  = new Integer(-1);
                            first = true;
                        }
                        int newCnt = scnt.intValue() + 1;
                        if (((float) newCnt / 2.0)
			    == (int) ((float) newCnt / 2.0)) {
                            extraClass = "ramadda-section-even";
                        } else {
                            extraClass = "ramadda-section-odd";
                        }
                        if (first) {
                            extraClass = "ramadda-section-first";
                        }
                        putProperty("section-cnt", new Integer(newCnt));
                    }
                    if (classArg != null) {
                        extraClass += " " + classArg + " ";
                    }

                    String full = (String) props.get("full");
                    if ((full != null) && ( !full.equals("false"))) {
                        extraClass += " ramadda-section-full ";
                    }
                    String clazz = baseClass + " " + extraClass;
                    buff.append("<div class=\"");
                    buff.append(clazz);
                    buff.append("\"   " + extraAttr + ">");
                    //                System.err.println("s:" + clazz +" " + extraAttr);
                    if (label == null) {
                        label = heading;
                    }
                    if (label != null) {
                        buff.append(HU.open(HU.TAG_DIV,
					    HU.cssClass("ramadda-heading-outer")));
                        buff.append(HU.div(label,
					   HU.cssClass("ramadda-heading")));
                        buff.append(HU.close(HU.TAG_DIV));
                    }
                    if (title != null) {
                        String sub = "";
                        if (subTitle != null) {
                            sub = HU.div(subTitle,
					 HU.clazz("ramadda-page-subtitle"));
                        }
                        buff.append(
				    HU.div(
					   getTitle(title, titleStyle) + sub,
					   HU.cssClass("ramadda-page-title")
					   + ((headerStyle == null)
					      ? ""
					      : HU.style(headerStyle))));
                    }

                    continue;
                }
                if (tline.startsWith("-section")) {
                    buff.append("</div>");

                    continue;
                }

                if (tline.startsWith("+scroll")) {
                    buff.append("\n");
                    HU.cssLink(
			       buff,
			       handler.getHtdocsUrl("/lib/scrollify/scrollify.css"));
                    HU.importJS(
				buff,
				handler.getHtdocsUrl(
						     "/lib/scrollify/jquery.scrollify.js"));

                    continue;
                }

                if (tline.startsWith("-scroll")) {
                    buff.append("\n");
                    inScroll = false;
                    HU.importJS(
				buff,
				handler.getHtdocsUrl("/lib/scrollify/template.js"));

                    continue;
                }

                if (tline.startsWith(":wikip")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 3);
                    String       page  = toks.get(1).trim();
                    String       label = (toks.size() > 2)
			? toks.get(2).trim()
			: Utils.makeLabel(page);
                    HU.href(buff,
			    "https://en.wikipedia.org/wiki/" + page,
			    label);

                    continue;
                }

                if (tline.startsWith(":reload")) {
                    String       id   = HU.getUniqueId("reload");
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                    String time = Utils.getProperty(props, "seconds", "60");
                    boolean showCbx = Utils.getProperty(props,
							"showCheckbox", true);
                    boolean showLabel = Utils.getProperty(props, "showLabel",
							  true);
                    if (showCbx) {
                        HU.checkbox(buff, "", "true", true,
				    HU.id(id));
                        buff.append(" ");
                    }
                    HU.span(buff, showLabel
			    ? ""
			    : "Reload", HU.id(id
					      + "_label"));
                    //                if (showLabel) {
                    //                    buff.append(" ");
                    //                    HU.span(buff, "", HU.id(id + "_label"));
                    //                }
                    buff.append(HU.script("Utils.initPageReload("
					  + time + ",'" + id + "'," + showLabel + ");"));

                    continue;
                }

                if (tline.startsWith(":script")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    HU.importJS(buff, toks.get(1));

                    continue;
                }

                if (tline.startsWith("+panel")) {
                    buff.append("\n");
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                    String name  = (String) props.get("name");
                    String style = (String) props.get("style");
                    String color = (String) props.get("color");
                    String extra = "";
                    String clazz = "panel ";
                    if (name != null) {
                        extra += " data-section-name=\"" + name + "\"  ";
                    }
                    if (style != null) {
                        extra += HU.style(style);
                    }
                    if ( !inScroll) {
                        clazz += " panel-first ";
                    }
                    if (color != null) {
                        clazz += " panel-" + color + " ";
                    }
                    buff.append("<section class=\"" + clazz + "\" " + extra
                                + ">\n");
                    buff.append("<div class=\"panel-inner\">");
                    inScroll = true;

                    continue;
                }

                if (tline.startsWith("-panel")) {
                    buff.append("\n");
                    buff.append("</div></section>");

                    continue;
                }

                if (tline.equals("+info-text")) {
                    buff.append("<div class=\"info-text\">");

                    continue;
                }
                if (tline.equals("-info-text")) {
                    buff.append("</div>");

                    continue;
                }

                if (tline.startsWith(":title")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    if (toks.size() > 1) {
			String label = toks.get(1);
			if(label.indexOf("{{")>=0) {
			    label = wikify(label, handler);
			    label = label.replaceAll(".*<.*?>(.*)</.*>.*","$1");
			}
			defineHeading.accept(buff,label,0);
                        buff.append(HU.div(getTitle(toks.get(1)),
					   HU.cssClass("ramadda-page-title")));
                    }
                    continue;
                }


                if (tline.startsWith("+frame")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                    String outerClazz = "ramadda-frame-outer";
                    String innerStyle = (String) props.get("innerStyle");
                    if (innerStyle == null) {
                        innerStyle = "";
                    }
                    String frameStyle = "";
                    if (props.get("shadow") != null) {
                        outerClazz += " ramadda-frame-shadow ";
                    }
                    String frameSize = (String) props.get("frameSize");
                    if (frameSize != null) {
                        frameStyle += " padding:" + getSize(frameSize) + ";";
                    }
                    String frameColor = (String) props.get("frameColor");
                    if (frameColor != null) {
                        frameStyle += " background:" + frameColor + ";";
                    }

                    String background = (String) props.get("background");
                    if (background != null) {
                        innerStyle += " background:" + background + ";";
                    }
                    String title = (String) props.get("title");
                    HU.open(buff, "div",
			    HU.cssClass(outerClazz)
			    + HU.style(frameStyle));
                    if (title != null) {
                        String titleBackground =
                            (String) props.get("titleBackground");
                        String titleColor = (String) props.get("titleColor");
                        String titleStyle = Misc.getProperty(props,
							     "titleStyle", "");
                        if (titleBackground != null) {
                            titleStyle += "background:" + titleBackground
				+ ";";
                        }
                        if (titleColor != null) {
                            titleStyle += "color:" + titleColor + ";";
                        }
                        //              String url = getTitleUrl(false);
                        //              if(url!=null) 
                        //                  title = HU.href(url, title);
                        HU.div(buff, title,
			       HU.clazz("ramadda-frame-title")
			       + HU.style(titleStyle));
                    }
                    HU.open(buff, "div",
			    HU.cssClass("ramadda-frame-inner")
			    + HU.style(innerStyle));

                    continue;
                }


                if (tline.startsWith("-frame")) {
                    HU.close(buff, "div");
                    HU.close(buff, "div");

                    continue;
                }

                if (tline.startsWith("+title")) {
                    StringBuilder extra = new StringBuilder();
                    List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                    HU.open(buff, "div",
			    HU.cssClass("ramadda-page-title"));
                    String url = getTitleUrl(true);
                    if (url != null) {
                        closeTheTag = true;
                        HU.open(buff, "a", "href=\"" + url + "\"");
                    }

                    continue;
                }

                if (tline.startsWith("-title")) {
                    if (closeTheTag) {
                        buff.append("</a>");
                        closeTheTag = false;
                    }
                    buff.append("</div>");

                    continue;
                }



                if (tline.startsWith(":button")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 3);
                    String       tag   = toks.get(0).substring(1);
                    String       url   = ((toks.size() > 1)
                                          ? toks.get(1)
                                          : "");
                    String       label = ((toks.size() > 2)
                                          ? toks.get(2)
                                          : url);
                    List<String> toks2 = StringUtil.splitUpTo(tag, "-", 2);
                    String       clazz = ((toks2.size() > 1)
                                          ? toks2.get(1)
                                          : "");
                    if (clazz.length() > 0) {
                        clazz = "ramadda-button-" + clazz;
                    }
                    HU.href(buff, url, label,
			    " class='ramadda-button " + clazz
			    + "' role='button' ");

                    continue;
                }

                if (tline.startsWith("+vertical-center")) {
                    buff.append("\n<div class=\"vertical-center\">\n");

                    continue;
                }

                if (tline.startsWith("-vertical-center")) {
                    buff.append("\n</div>\n");

                    continue;
                }

                if (tline.startsWith("+absolute")) {
                    Hashtable props = lineToProps(tline);
                    String    style = (String) props.get("style");
                    if (style == null) {
                        style = "";
                    }
                    style += "position:absolute;";
                    for (String side : new String[] { "top", "left", "bottom",
						      "right" }) {
                        String sv = (String) props.get(side);
                        if (sv != null) {
                            sv = getSize(sv);
                            if ( !sv.endsWith(";")) {
                                sv += ";";
                            }
                            style += side + ":" + sv;
                        }
                    }
                    HU.open(buff, "div", HU.style(style));

                    continue;
                }
                if (tline.equals("-absolute")) {
                    buff.append("</div>\n");

                    continue;
                }

                if (tline.equals("+relative")) {
                    buff.append("<div style=\"position:relative;\">\n");

                    continue;
                }
                if (tline.equals("-relative")) {
                    buff.append("</div>\n");

                    continue;
                }

                if (tline.equals("+centerdiv")) {
                    buff.append(HU.open(HU.TAG_DIV,
					HU.style("text-align:center;")));
                    buff.append(
				HU.open(
					HU.TAG_DIV,
					HU.style(
						 "display:inline-block;text-align:left;")));

                    continue;
                }

                if (tline.equals("-centerdiv")) {
                    buff.append(HU.close(HU.TAG_DIV));
                    buff.append(HU.close(HU.TAG_DIV));

                    continue;
                }


                if (tline.equals("+center")) {
                    buff.append("<center>");

                    continue;
                }

                if (tline.equals("-center")) {
                    buff.append("</center>");

                    continue;
                }

                if (tline.equals(":br")) {
                    buff.append("<br>");

                    continue;
                }
                if (tline.equals(":p")) {
                    buff.append("<p></p>");

                    continue;
                }

                if (tline.startsWith(":b ")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    buff.append(HU.b((toks.size() > 1)
				     ? toks.get(1)
				     : ""));

                    continue;
                }

                if (tline.startsWith(":h1")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
		    String label = toks.size() > 1 ? toks.get(1): "";
		    defineHeading.accept(buff,label,1);
                    buff.append(HU.h1(label));
                    continue;
                }

                if (tline.startsWith(":h2")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
		    String label = toks.size() > 1 ? toks.get(1): "";
		    defineHeading.accept(buff,label,2);
                    buff.append(HU.h2(label));
                    continue;
                }



                if (tline.startsWith(":h3")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
		    String label = toks.size() > 1 ? toks.get(1): "";
		    defineHeading.accept(buff,label,3);
                    buff.append(HU.h3(label));
                    continue;
                }

                if (tline.startsWith(":center")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    buff.append(HU.center((toks.size() > 1)
					  ? toks.get(1)
					  : ""));

                    continue;
                }


                if (tline.startsWith(":link")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 3);
                    String       label = (toks.size() > 2)
			? toks.get(2)
			: "link";
                    buff.append(HU.href(toks.get(1), label));

                    continue;
                }

                if (tline.startsWith(":draft")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       label = (toks.size() > 1)
			? toks.get(1)
			: "Draft";
                    buff.append(
				"<div class=ramadda-draft-container><div class=ramadda-draft>"
				+ label + "</div></div>\n");

                    continue;
                }

                if (tline.startsWith(":anchor")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
		    String label = toks.size() > 1 ? toks.get(1): "";
		    defineHeading.accept(buff,label,1);
		    continue;
		}


                if (tline.startsWith(":nav")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
		    String what = toks.get(0).trim();
                    headingsProps = 
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
		    if(what.equals(":navleft")) {
			headingsProps.put("navleft","true");
		    }
		    headingsNav = "heading_" + HU.blockCnt++;
		    buff.append("${" + headingsNav+"}");
		    continue;
		}
		


                if (tline.startsWith(":heading")
		    || tline.startsWith(":block")
		    || tline.startsWith(":credit")
		    || tline.startsWith(":note")
		    || tline.startsWith(":box")
		    || tline.startsWith(":blurb")
		    || tline.startsWith(":callout")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       what  = toks.get(0).substring(1);
                    List<String> toks2 = StringUtil.splitUpTo(what, "-", 2);
                    what = toks2.get(0);
                    String clazz = toks.get(0).trim().substring(1);
                    String blob  = (toks.size() > 1)
			? toks.get(1)
			: "";
                    if ( !clazz.equals(what)) {
                        clazz = "ramadda-" + what + "  ramadda-" + clazz;
                    } else {
                        clazz = "ramadda-" + what;
                    }
		    if(what.startsWith("heading")) {
			defineHeading.accept(buff,blob,1);
		    }
                    buff.append(
				HU.div(
				       HU.div(blob, HU.cssClass(clazz)),
				       HU.cssClass(
						   "ramadda-" + what + "-outer")));

                    continue;
                }

                if (tline.startsWith("+flow")) {
                    buff.append(
				HU.open(
					HU.TAG_DIV,
					HU.style(
						 "display:inline-block;vertical-align:top;")));

                    continue;
                }

                if (tline.startsWith("-flow")) {
                    buff.append(HU.close(HU.TAG_DIV));

                    continue;
                }

                if (tline.startsWith("+mini") || tline.startsWith("+block")
		    || tline.startsWith("+note")
		    || tline.startsWith("+box")
		    || tline.startsWith("+heading")
		    || tline.startsWith("+blurb")
		    || tline.startsWith("+callout")) {
                    List<String>  toks = StringUtil.splitUpTo(tline, " ", 2);
                    String        tag       = toks.get(0).substring(1);
                    //box-green

                    List<String>  toks2 = StringUtil.splitUpTo(tag, "-", 2);
                    String        what      = toks2.get(0);
                    //box

                    String        remainder = ((toks2.size() > 1)
					       ? toks2.get(1)
					       : "");
                    //green

                    StringBuilder extra     = new StringBuilder();
                    String        style     = getAttribute(tline, "style");
                    if (style != null) {
                        extra.append(HU.style(style));
                    }
                    extra.append(" class=\"ramadda-block ramadda-");
                    extra.append(what);
                    extra.append(" ");
                    if (remainder.length() > 0) {
                        extra.append("ramadda-");
                        extra.append(tag);
                        extra.append(" ");
                    }
                    extra.append(getAttribute(tline, "class", ""));
                    extra.append(" \" ");
                    buff.append(HU.open(HU.TAG_DIV,
					HU.cssClass("ramadda-" + what
						    + "-outer")));
                    buff.append(HU.open("div", extra.toString()));

                    continue;
                }

                if (tline.startsWith("-mini") || tline.startsWith("-block")
		    || tline.startsWith("-heading")
		    || tline.startsWith("-note")
		    || tline.startsWith("-box")
		    || tline.startsWith("-blurb")
		    || tline.startsWith("-callout")) {
                    buff.append("</div></div>");

                    continue;
                }


                if (tline.startsWith("+row")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    Hashtable props =
                        HU.parseHtmlProperties((toks.size() > 1)
					       ? toks.get(1)
					       : "");
                    rowStates.add(new RowState(buff, props));

                    continue;
                }
                if (tline.equals("-row")) {
                    if (rowStates.size() == 0) {
                        wikiError(buff, "Error: unopened row");

                        continue;
                    }
                    RowState rowState = rowStates.get(rowStates.size() - 1);
                    rowState.closeRow(buff);
                    rowStates.remove(rowStates.size() - 1);

                    continue;
                }

                if (tline.startsWith(":comment")) {
                    List<String> toks = StringUtil.splitUpTo(tline, " ", 2);
                    if (toks.size() > 1) {
                        HU.comment(buff, toks.get(1));
                    }

                    continue;
                }

                if (tline.startsWith(":rem")) {
                    continue;
                }

                if (tline.startsWith(":pad")) {
                    List<String> toks   = StringUtil.splitUpTo(tline, " ", 2);
                    String       height = "100px";
                    if (toks.size() > 1) {
                        height = toks.get(1);
                    }
                    HU.div(buff, "",
			   HU.style("height:" + height));

                    continue;
                }
                if (tline.startsWith(":col-")) {
                    RowState rowState = null;
                    if (rowStates.size() == 0) {
                        //Add a row if we're not in one
                        rowStates.add(rowState = new RowState(buff, null));
                    } else {
                        rowState = rowStates.get(rowStates.size() - 1);
                    }

                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       clazz = toks.get(0).substring(1);
                    if (clazz.matches("col-[0-9]+")) {
                        clazz = clazz.replace("col-", "col-md-");
                    }
                    String contents = "";
                    if (toks.size() > 1) {
                        contents = toks.get(1);
                    }
                    rowState.openColumn(buff,
                                        HU.cssClass(clazz
						    + " ramadda-col wiki-col"));
                    buff.append(contents);
                    rowState.closeColumn(buff);

                    continue;
                }

                if (tline.startsWith("+col-")) {
                    RowState rowState = null;
                    if (rowStates.size() == 0) {
                        //Add a row if we're not in one
                        rowStates.add(rowState = new RowState(buff, null));
                    } else {
                        rowState = rowStates.get(rowStates.size() - 1);
                    }
                    List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                    StringBuilder extra = new StringBuilder();
                    String        clazz = toks.get(0).substring(1);
                    if (toks.size() > 1) {
                        String attrs = toks.get(1);
                        String style = getAttribute(attrs, "style");
                        if (style != null) {
                            extra.append(HU.style(style));
                        }
                        clazz = clazz + " "
			    + getAttribute(attrs, "class", "");
                    }
                    if (clazz.matches("col-[0-9]+")) {
                        clazz = clazz.replace("col-", "col-md-");
                    }
                    rowState.openColumn(buff, HU.cssClass(clazz
							  + " ramadda-col wiki-col") + extra);

                    continue;
                }

                if (tline.startsWith("-col")) {
                    if (rowStates.size() == 0) {
                        wikiError(buff, "Error: unopened column");

                        continue;
                    }
                    rowStates.get(rowStates.size() - 1).closeColumn(buff);

                    continue;
                }

                if (tline.equals("----")) {
                    buff.append("<hr class=\"ramadda-hr\">\n");

                    continue;
                }


                int starCnt = 0;
                while (tline.startsWith("*")) {
                    tline = tline.substring(1);
                    starCnt++;
                }
                if (starCnt > 0) {
                    if (starCnt > ulCnt) {
                        while (starCnt > ulCnt) {
                            buff.append("<ul>\n");
                            ulCnt++;
                        }
                    } else {
                        while ((starCnt < ulCnt) && (ulCnt > 0)) {
                            buff.append("</ul>\n");
                            ulCnt--;
                        }
                    }
                    buff.append("<li> ");
                    buff.append(tline);
                    buff.append("</li> ");
                    buff.append("\n");

                    continue;
                }
                while (ulCnt > 0) {
                    buff.append("</ul>\n");
                    ulCnt--;
                }

                int hashCnt = 0;
                //Check if this is a commented attribute inside a tag
                while (tline.startsWith("#") && (tline.indexOf("=") < 0)) {
                    tline = tline.substring(1);
                    hashCnt++;
                }
                if (hashCnt > 0) {
                    if (hashCnt > olCnt) {
                        while (hashCnt > olCnt) {
                            buff.append("<ol>\n");
                            olCnt++;
                        }
                    } else {
                        while ((hashCnt < olCnt) && (olCnt > 0)) {
                            buff.append("</ol>\n");
                            olCnt--;
                        }
                    }
                    buff.append("<li> ");
                    buff.append(tline);
                    buff.append("\n");

                    continue;
                }

                while (olCnt > 0) {
                    buff.append("</ol>\n");
                    olCnt--;
                }

                buff.append(line);
                buff.append("\n");
            }
        }



        //end of processing
        while (ulCnt > 0) {
            buff.append("</ul>\n");
            ulCnt--;
        }
        while (olCnt > 0) {
            buff.append("</ol>\n");
            olCnt--;
        }
        if (js.length() > 0) {
            HU.script(buff, js.toString());
        }

        String s = buff.toString();
        for (int i = 0; i < allTabStates.size(); i++) {
            TabState tabInfo = allTabStates.get(i);
            s = s.replace("${" + tabInfo.id + "}", tabInfo.title.toString());
        }


        StringBuffer sb      = new StringBuffer();
        int          baseIdx = 0;
        while (true) {
            int idx1 = s.indexOf(TAG_PREFIX, baseIdx);
            if (idx1 < 0) {
                sb.append(s.substring(baseIdx));
                break;
            }
            int idx2 = Utils.findNext(s, idx1, TAG_SUFFIX);
            if (idx2 <= idx1) {
                sb.append(s.substring(baseIdx));
                break;
            }
            sb.append(s.substring(baseIdx, idx1));
            String property = s.substring(idx1 + 2, idx2);

            //If there were new lines in the property tag they got replaced with <P>
            //Unreplace them
            property = property.replaceAll("\n<p>\n", " ");
            baseIdx  = idx2 + 2;

            if (property.equals(PROP_NOHEADING)) {
                makeHeadings = false;
            } else if (property.equals(PROP_HEADING)) {
                makeHeadings = true;
            } else if (property.equals(PROP_NOP)) {
                replaceNewlineWithP = false;
            } else if (property.startsWith("wikip ")) {
                //the wikipedia link
                List<String> toks  = StringUtil.splitUpTo(property, " ", 3);
                String       page  = toks.get(1).trim();
                String       label = (toks.size() > 2)
		    ? toks.get(2).trim()
		    : Utils.makeLabel(page);
                HU.href(sb, "https://en.wikipedia.org/wiki/" + page,
			label);
            } else if (property.equals(PROP_DOP)) {
                replaceNewlineWithP = true;
            } else {
                String value = null;
                if (handler != null) {
                    value = handler.getWikiPropertyValue(this, property,
							 notTags);
                }
                if (value == null) {
                    value = "Unknown property:" + property;
                }
                sb.append(value);
            }
	}

        s = sb.toString();


	if(headingsNav!=null && headings2.size()>0) {
	    StringBuilder hb = new StringBuilder();
	    boolean left =  "true".equals(Utils.getProperty(headingsProps,"navleft","false"));
	    String delim = Utils.getProperty(headingsProps,"delimiter","&nbsp;|&nbsp;");
	    if(left) delim="<br>";
	    for(Object o: headings2) {
		Object[] tuple = (Object[])o;
		String id = (String)tuple[0];
		String label = (String)tuple[1];		
		int level = (int)tuple[2];
		//Handle if this is {{title}} eg.
		if(label.indexOf("{{")>=0) {
		    label = wikify(label, handler);
		    label = label.replaceAll(".*<.*?>(.*)</.*>.*","$1");
		}
		if(!left && hb.length()>0)
		    hb.append(delim);
		String clazz=" ramadda-nav-link ";
		if(left) clazz+= " ramadda-nav-link-" + level;
		    
		String href = HU.mouseClickHref("HtmlUtils.scrollToAnchor('"+ id+"',-50)",label,HU.attrs("class", clazz));
		if(left) {
		    href= HU.div(href,HU.attrs("class","ramadda-nav-left-link","navlink",id));
		}
		hb.append(href);
	    }
	    String style = Utils.getProperty(headingsProps,"style","");
	    if(left) {
		String leftStyle = "";
		String leftTop = (String) Utils.getProperty(headingsProps,"leftTop",null);
		if(leftTop!=null) leftStyle+="top:" + leftTop+"px";
		s = s.replace("${" + headingsNav+"}", "");
		
		s = "<div class=ramadda-nav-horizontal><div class=ramadda-nav-left style='" + leftStyle+"'>" + hb + "</div><div class=ramadda-nav-right>" + s +"</div></div>" + HU.script("HtmlUtils.initNavLinks()");
		
	    } else {
		s = s.replace("${" + headingsNav+"}", HU.div(hb.toString(),HU.attrs("class","ramadda-nav","style",style)));
	    }
	}




        /*
          <block title="foo">xxxxx</block>
	*/
        sb = new StringBuffer();
        while (true) {
            int idx1 = s.indexOf("<block");
            if (idx1 < 0) {
                break;
            }
            int idx2 = s.indexOf(">", idx1);
            if (idx2 < 0) {
                break;
            }
            int idx3 = s.indexOf("</block>", idx2);
            if (idx3 < 0) {
                break;
            }
            String    first = s.substring(0, idx1);
            String    attrs = s.substring(idx1 + 6, idx2);
            String    inner = s.substring(idx2 + 1, idx3);
            Hashtable props = HU.parseHtmlProperties(attrs);
            sb.append(first);

            String  before     = (String) props.get("before");
            String  after      = (String) props.get("after");
            String  show = Misc.getProperty(props, ATTR_SHOW, (String) null);
            boolean shouldShow = true;


            if (show != null) {
                if (show.equals("mobile")) {
                    if ( !getMobile()) {
                        shouldShow = false;
                    }
                } else if (show.equals("!mobile")) {
                    if (getMobile()) {
                        shouldShow = false;
                    }
                } else if (show.equals("none")) {
                    shouldShow = false;
                } else if (show.startsWith("user")) {
                    if (user == null) {
                        shouldShow = false;
                    } else {
                        shouldShow = true;
                    }
                }
            }



            if (shouldShow) {
                if (before != null) {
                    Date dttm = Utils.parseDate(before);
                    if (dttm == null) {
                        inner = "before Bad date format:" + before;
                    } else {
                        Date now = new Date();
                        if (now.getDate() > dttm.getDate()) {
                            shouldShow = false;
                        }
                    }
                }
                if (after != null) {
                    Date dttm = Utils.parseDate(after);
                    if (dttm == null) {
                        inner = "Bad date format:" + after;
                    } else {
                        Date now = new Date();
                        if (now.getDate() < dttm.getDate()) {
                            shouldShow = false;
                        }
                    }
                }
            }

            if (props.get(ATTR_VAR) != null) {
                myVars.put(props.get(ATTR_VAR).toString().trim(), inner);
            } else {
                boolean open = Misc.getProperty(props, ATTR_OPEN, true);
                boolean decorate = Misc.getProperty(props, ATTR_DECORATE,
						    false);
                String title = Misc.getProperty(props, ATTR_TITLE, "");
                //<block show="ismobile"
                if (shouldShow) {
                    if (decorate) {
                        sb.append(HU.makeShowHideBlock(title, inner,
						       open, HU.cssClass("wiki-blockheader"),
						       HU.cssClass("wiki-block")));
                    } else {
                        sb.append(inner);
                    }
                }

            }

            s = s.substring(idx3 + "</block>".length());
        }
        sb.append(s);
        s = sb.toString();
        s = s.replace("_BRACKETOPEN_", "[");
        s = s.replace("_BRACKETCLOSE_", "]");
        //        s = s.replaceAll("(\n\r)+","<br>\n");
        //        s = s.replaceAll("\n+","<br>\n");



        if (getMakeHeadings()) {
            if (headings.size() >= 2) {
                StringBuffer toc = new StringBuffer();
                makeHeadings(headings, toc, -1, "");
                String block = HU.makeShowHideBlock("Contents",
						    toc.toString(), true,
						    HU.cssClass("wiki-tocheader"),
						    HU.cssClass("wiki-toc"));
                floatBoxes.add(block);

                String blocks =
                    "<table class=\"wiki-toc-wrapper\" align=\"right\" width=\"30%\"><tr><td>"
                    + StringUtil.join("<br>", floatBoxes)
                    + "</td></tr></table>";
                s = blocks + s;
            }
        }

        if (categoryLinks.size() > 0) {
            s = s + HU.div(
			   "<b>Categories:</b> "
			   + StringUtil.join(
					     "&nbsp;|&nbsp; ", categoryLinks), HU.cssClass(
											   "wiki-categories"));
        }



        for (java.util.Enumeration keys = myVars.keys();
	     keys.hasMoreElements(); ) {
            Object key   = keys.nextElement();
            Object value = myVars.get(key);
            s = s.replace("${" + key + "}", value.toString());
        }

        return s;
    }






    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getNotTags() {
        return notTags;
    }



    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public String getTitle(String label) {
        return getTitle(label, null);
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param style _more_
     *
     * @return _more_
     */
    public String getTitle(String label, String style) {
        String url = getTitleUrl(true);

        return (url != null)
	    ? HU.href(url, label, (style == null)
		      ? null
		      : HU.style(style))
	    : label;
    }

    /**
     * _more_
     *
     * @param andClear _more_
     *
     * @return _more_
     */
    public String getTitleUrl(boolean andClear) {
        String titleUrl = (String) getProperty("title-url");
        if ((titleUrl != null) && andClear) {
            removeProperty("title-url");
        }

        return titleUrl;

    }

    /**
     * _more_
     *
     * @param url _more_
     */
    public void setTitleUrl(String url) {
        putProperty("title-url", url);
    }

    /**
     * _more_
     *
     * @param buff _more_
     * @param tline _more_
     */
    private void handleEmbed(Appendable buff, String tline) {
        try {
            handleEmbedInner(buff, tline);
        } catch (Exception exc) {
            wikiError(buff, "Error handling embed:" + tline + "<br>" + exc);
        }
    }

    /**
     * _more_
     *
     * @param buff _more_
     * @param tline _more_
     *
     * @throws Exception _more_
     */
    private void handleEmbedInner(Appendable buff, String tline)
	throws Exception {

        tline = tline.substring(2);
        if ( !tline.endsWith(")")) {
            HU.div(buff, "Could not process embed line:" + tline, "");

            return;
        }
        tline = tline.substring(0, tline.length() - 1).trim();
        Hashtable props = lineToProps(tline);
        int       index = tline.indexOf(" ");
        if (index >= 0) {
            tline = tline.substring(0, index);
        }
        String        url    = tline;
        boolean       link   = Misc.equals("true", getWikiProperty(props, "link", "embedLink","false"));
        String        label  = (String) getWikiProperty(props, "label", "embedLabel", null);
        String        width  = (String) getWikiProperty(props, "width", "embedWidth","640");
        String        height = (String) getWikiProperty(props, "height", "embedHeight","390");
        String        style  = (String) getWikiProperty(props, "style", "embedStyle", null);
	//	System.err.println(link +"  " + label +" " + width +" " + height +" " + style);
        StringBuilder sb     = new StringBuilder();

	boolean isFacebook = Pattern.matches("https://www.facebook.com/.*/posts/\\d+",  url);
	//Don't bother with the oembed for facebook as it requires an access token
	if (isFacebook) {
            if (getProperty("embedfacebook") == null) {
                putProperty("embedfacebook", "true");
                //This is the RAMADDA app id
                buff.append(
			    "<div id='fb-root'></div><script async defer crossorigin='anonymous' src='https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v10.0&appId=53108697449' nonce='bNyUnLLe'></script>");
            }
            width = Utils.getProperty(props, "width", "500");
            sb.append("<div class='fb-post' data-href='" + url
		      + "' data-width='" + width
		      + "' data-show-text='true'></div>");
	} else {
	    Oembed.Response  response = Oembed.get(url, width, height);
	    if(response!=null) {
		sb.append(response.getHtml());	    
	    } else {
		buff.append(HU.href(url, (label != null)
				    ? label
				    : url));
		return;
	    }
	}

        if ( !link && (label != null)) {
            sb = new StringBuilder(HU.div(link + "<br>" + sb,
					  "style=display-inline:block;"));
        }
        if (link) {
            sb = new StringBuilder(HU.div(HU.div(sb.toString()) 
					  + HU.href(url, (label != null)
						    ? label
						    : url), "style=display:inline-block;"));
        }
        if (style != null) {
            sb = new StringBuilder(HU.div(sb.toString(),
					  HU.style("display:inline-block;" + style)));
        }
        buff.append(sb);
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param chunk _more_
     * @param handler _more_
     *
     * @throws IOException _more_
     */
    public void handleVega(Appendable sb, String chunk,
                           WikiPageHandler handler)
	throws IOException {
        boolean addResources = getProperty("vegaimport") == null;
	if(!addResources)     putProperty("vegaimport", "true");
        if (addResources) {
            sb.append("\n");
            sb.append(
		      "<script src='https://cdn.jsdelivr.net/npm/vega@5'></script>\n");
            sb.append(
		      "<script src='https://cdn.jsdelivr.net/npm/vega-lite@4'></script>\n");
            sb.append(
		      "<script src='https://cdn.jsdelivr.net/npm/vega-embed@6'></script>\n");
        }
        String id     = "vegablock_" + HU.blockCnt++;
        String jsonId = "vegaJson" + HU.blockCnt++;
        String viewId = "vegaView" + HU.blockCnt++;
        sb.append("\n");
        HU.div(sb, "", "id=" + id);
        sb.append("\n");
        StringBuilder js = new StringBuilder();
        js.append("\n//Generated vega code\n");
        js.append("\nvar " + jsonId + "=" + chunk + "\n");
        js.append("var " + viewId + ";\n");
        js.append("vegaEmbed('#" + id + "', " + jsonId + ");\n");
        js.append("//Done generated vega code\n");
        HU.script(sb, js.toString());
    }

    /**
     * _more_
     *
     * @param mainBuffer _more_
     * @param chunk _more_
     * @param handler _more_
     *
     * @throws IOException _more_
     */
    public void handleCode(Appendable sb, Chunk chunk,
                           WikiPageHandler handler)
	throws IOException {
        if (chunk.rest.equals("vega-lite")) {
            handleVega(sb, chunk.chunk.toString(), handler);
            return;
        }
        if (chunk.rest.equals("markdown")) {
	    String srcId = HU.getUniqueId("markdownsrc");
	    String targetId = HU.getUniqueId("markdownsrc");	    
	    HU.div(sb,chunk.chunk.toString(),HU.attrs("id",srcId,"style","display:none;"));
	    HU.div(sb,chunk.chunk.toString(),HU.attrs("id",targetId,"style",""));	    
	    HU.script(sb,"HtmlUtils.applyMarkdown('" + srcId+"','" + targetId+"');");
	    return;
	}
        if (chunk.rest.equals("latex")) {
	    String srcId = HU.getUniqueId("latexsrc");
	    String targetId = HU.getUniqueId("latexsrc");	    
	    HU.div(sb,chunk.chunk.toString(),HU.attrs("id",srcId,"style","display:none;"));
	    HU.div(sb,chunk.chunk.toString(),HU.attrs("id",targetId,"style",""));	    
	    HU.script(sb,"HtmlUtils.applyLatex('" + srcId+"','" + targetId+"');");
	    return;
	}


	if(chunk.rest.equals("javascript")) {
	    sb.append(HU.cssLink(handler.getHtdocsUrl("/lib/prettify/prettify.css")));
	    sb.append(HU.importJS(handler.getHtdocsUrl("/lib/prettify/prettify.js")));
	    String id = "javascript" + HU.blockCnt++;
	    sb.append("<pre class=\"prettyprint\">\n");
	    int cnt = 0;
	    for (String line:chunk.chunk.toString().split("\n")) {
		cnt++;
		line = line.replace("\r", "");
		line = HU.entityEncode(line);
		sb.append("<span class=nocode><a "
			  + HU.attr("name", "line" + cnt)
			  + "></a><a href=#line" + cnt + ">" + cnt
			  + "</a></span>" + HU.space(1) + line + "<br>");
	    }
	    sb.append("</pre>\n");
	    sb.append(HU.script("prettyPrint();"));
	    return;
	}
        HU.pre(sb,
	       "CODE:" + chunk.rest + "\n" + chunk.chunk.toString());
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String getSize(String s) {
        if (s == null) {
            return null;
        }
        if (s.endsWith("%") || s.endsWith("px")) {
            return s;
        }

        return s + "px";
    }


    /**
     * _more_
     *
     * @param line _more_
     * @param attr _more_
     *
     * @return _more_
     */
    private String getAttribute(String line, String attr) {
        return getAttribute(line, attr, null);
    }

    /**
     * _more_
     *
     * @param line _more_
     * @param attr _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String getAttribute(String line, String attr, String dflt) {
        String v = StringUtil.findPattern(line,
                                          attr + "\\s*=\\s*\\\"(.*?)\\\"");
        if (v == null) {
            v = StringUtil.findPattern(line, attr + "\\s*=\\s*([^\\s]+)");
        }
        if (v == null) {
            return dflt;
        }

        return v;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getWikiVariable(String key) {
        return (String) myVars.get(key);
    }

    /**
     * _more_
     *
     * @param tline _more_
     *
     * @return _more_
     */
    private Hashtable lineToProps(String tline) {
        List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
        Hashtable    props = HU.parseHtmlProperties((toks.size() > 1)
						    ? toks.get(1)
						    : "");

        return props;
    }



    /**
     * _more_
     *
     * @param headings _more_
     * @param toc _more_
     * @param parentLevel _more_
     * @param parentPrefix _more_
     */
    private static void makeHeadings(List headings, StringBuffer toc,
                                     int parentLevel, String parentPrefix) {
        int    cnt          = 0;
        int    currentLevel = -1;
        String prefix       = "";
        while (headings.size() > 0) {
            Object[] pair  = (Object[]) headings.get(0);
            int      level = ((Integer) pair[0]).intValue();
            if ((level > currentLevel) && (currentLevel >= 0)) {
                makeHeadings(headings, toc, currentLevel, prefix);

                continue;
            } else if (level < currentLevel) {
                if (parentLevel >= 0) {
                    return;
                }
            }
            headings.remove(0);
            cnt++;
            String label = (String) pair[1];
            if (parentPrefix.length() > 0) {
                prefix = parentPrefix + "." + cnt;
            } else {
                prefix = "" + cnt;
            }
            //            System.err.println(prefix);
            toc.append(StringUtil.repeat("&nbsp;&nbsp;", level - 1));
            toc.append("<a href=\"#" + label + "\">");
            toc.append(prefix);
            toc.append(HU.space(1));
            toc.append(label);
            toc.append("</a><br>\n");
            currentLevel = level;
        }

    }



    /**
     * Set the MakeHeadings property.
     *
     * @param value The new value for MakeHeadings
     */
    public void setMakeHeadings(boolean value) {
        this.makeHeadings = value;
    }

    /**
     * Get the MakeHeadings property.
     *
     * @return The MakeHeadings
     */
    public boolean getMakeHeadings() {
        return this.makeHeadings;
    }


    /**
     * Set the ReplaceNewlineWithP property.
     *
     * @param value The new value for ReplaceNewlineWithP
     */
    public void setReplaceNewlineWithP(boolean value) {
        this.replaceNewlineWithP = value;
    }

    /**
     * Get the ReplaceNewlineWithP property.
     *
     * @return The ReplaceNewlineWithP
     */
    public boolean getReplaceNewlineWithP() {
        return this.replaceNewlineWithP;
    }

    /**
     *  Set the Mobile property.
     *
     *  @param value The new value for Mobile
     */
    public void setMobile(boolean value) {
        mobile = value;
    }

    /**
     *  Get the Mobile property.
     *
     *  @return The Mobile
     */
    public boolean getMobile() {
        return mobile;
    }

    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public String getUser() {
        return user;
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param note _more_
     *
     * @throws Exception _more_
     */
    public static void note(Appendable sb, String note) throws Exception {
        sb.append("\n+note\n");
        sb.append(note);
        sb.append("\n-note\n");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @throws Exception _more_
     */
    public static void title(Appendable sb, String s) throws Exception {
        sb.append("\n+title\n");
        sb.append(s);
        sb.append("\n-title\n");
    }


    /**
     * _more_
     *
     * @param buff _more_
     * @param msg _more_
     */
    public static void wikiError(Appendable buff, String msg) {
        try {
            HU.span(buff, msg + "<br>",
		    HU.cssClass("wiki-error"));
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param heading _more_
     *
     * @throws Exception _more_
     */
    public static void heading(Appendable sb, String heading)
	throws Exception {
        sb.append("\n:heading ");
        sb.append(heading.trim());
        sb.append("\n");
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jul 31, '19
     * @author         Enter your name here...
     */
    public static class ContentState {

        /** _more_ */
        String id;

        /**
         * _more_
         */
        public ContentState() {
            this.id = HU.getUniqueId("contents");
        }
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Jan 27, '19
     * @author         Enter your name here...
     */
    public static class TabState extends ContentState {

        /** _more_ */
        StringBuilder title = new StringBuilder();

        /** _more_ */
        int cnt = 0;

        /** _more_ */
        String minHeight;

        /**
         * _more_
         */
        public TabState() {}
    }



    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Jul 31, '19
     * @author         Enter your name here...
     */
    public static class AccordionState extends ContentState {

        /** _more_ */
        int segmentId = 0;

        /** _more_ */
        int activeSegment = 0;

        /** _more_ */
        String heightStyle = "content";

        /** _more_ */
        boolean collapsible;

        /** _more_ */
        int animate = 200;

        /** _more_ */
        boolean decorate = true;

        /**
         * _more_
         */
        public AccordionState() {}
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Feb 18, '19
     * @author         Enter your name here...
     */
    public static class RowState {

        /** _more_ */
        int colCnt = 0;

        /** _more_ */
        Hashtable props;

        /**
         * _more_
         *
         * @param buff _more_
         * @param props _more_
         */
        public RowState(Appendable buff, Hashtable props) {
            try {
                String clazz = "row wiki-row";
                if (props != null) {
                    String c = (String) props.get("tight");
                    if (c != null) {
                        clazz += " row-tight ";
                    }
                }
                HU.open(buff, "div", HU.clazz(clazz));
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        /**
         * _more_
         *
         * @param buff _more_
         */
        public void closeRow(Appendable buff) {
            try {
                closeColumns(buff);
                buff.append("</div>");
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        /**
         * _more_
         *
         * @param buff _more_
         */
        public void closeColumn(Appendable buff) {
            try {
                if (colCnt == 0) {
                    wikiError(buff, "Error: unopened column");

                    return;
                }
                colCnt--;
                buff.append("</div>");
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        /**
         * _more_
         *
         * @param buff _more_
         * @param attrs _more_
         */
        public void openColumn(Appendable buff, String attrs) {
            try {
                closeColumns(buff);
                HU.open(buff, "div", attrs);
                colCnt++;
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        /**
         * _more_
         *
         * @param buff _more_
         */
        public void closeColumns(Appendable buff) {
            try {
                while (colCnt > 0) {
                    colCnt--;
                    buff.append("</div>");
                }
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }



    }


    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Feb 18, '19
     * @author         Enter your name here...
     */
    public static class TableState {

        /** _more_ */
        boolean inHead = true;

        /** _more_ */
        boolean inRow = false;

        /** _more_ */
        boolean inBody = false;

        /** _more_ */
        boolean inTr = false;

        /** _more_ */
        boolean inTd = false;

        /**
         * _more_
         */
        public TableState() {}



    }


    /**
     * Class description
     *
     *
     * @version        $version$, Thu, Feb 25, '21
     * @author         Enter your name here...
     */
    private static class Chunk {

        /** _more_ */
        static int TYPE = 0;

        /** _more_ */
        static int TYPE_WIKI = TYPE++;

        /** _more_ */
        static int TYPE_CODE = TYPE++;

        /** _more_ */
        static int TYPE_NOWIKI = TYPE++;

        /** _more_ */
        static int TYPE_CSS = TYPE++;

        /** _more_ */
        static int TYPE_JS = TYPE++;

        /** _more_ */
        static int TYPE_PRE = TYPE++;



        /** _more_ */
        int type;

        /** _more_ */
        StringBuilder chunk = new StringBuilder();

        /** _more_ */
        String rest;

        /**
         * _more_
         *
         * @param type _more_
         */
        Chunk(int type) {
            this.type = type;
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param rest _more_
         */
        Chunk(int type, String rest) {
            this(type);
            this.rest = rest.trim();
        }

        /**
         * _more_
         *
         * @param line _more_
         */
        public void append(String line) {
            chunk.append(line);
            chunk.append("\n");
        }

        /**
         * _more_
         *
         * @return _more_
         */
        private String getCodeName() {
            if (type == TYPE_WIKI) {
                return "WIKI";
            }
            if (type == TYPE_CODE) {
                return "CODE";
            }
            if (type == TYPE_CSS) {
                return "CSS";
            }
            if (type == TYPE_JS) {
                return "JS";
            }
            if (type == TYPE_PRE) {
                return "PRE";
            }
            return "NOWIKI";
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return "chunk:" + getCodeName() + " text:"
		+ chunk.toString().replaceAll("\n", "_NL_") + "\n";
        }

        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        public static List<Chunk> splitText(String s) {
            List<Chunk> chunks = new ArrayList<Chunk>();
            String[]    lines  = s.split("\n");
            Chunk       chunk  = null;
            String[] prefixes = new String[] { "<nowiki>", "+css",
					       "+javascript", "+pre", "<pre>" };
            String[] suffixes = new String[] { "</nowiki>", "-css",
					       "-javascript", "-pre", "</pre>" };
            int[] codes = new int[] { TYPE_NOWIKI, TYPE_CSS, TYPE_JS,
                                      TYPE_PRE, TYPE_PRE };

            for (String line : lines) {
                if (line.startsWith("```")) {
                    if ((chunk == null) || (chunk.type != Chunk.TYPE_CODE)) {
                        //open
                        chunks.add(chunk = new Chunk(Chunk.TYPE_CODE,
						     line.substring(3)));
                    } else {
                        //close
                        chunk = null;
                    }

                    continue;
                }
                boolean matched = false;
                for (int i = 0; (i < prefixes.length) && !matched; i++) {
                    if (line.startsWith(prefixes[i])) {
                        int code = codes[i];
                        if ((chunk == null) || (chunk.type != code)) {
                            //open
                            chunks.add(chunk = new Chunk(code));
                            String rest =
                                line.substring(prefixes[i].length()).trim();
                            if (rest.length() > 0) {
                                chunk.append(rest);
                            }
                        }
                        matched = true;
                    }
                }
                if (matched) {
                    continue;
                }
                matched = false;

                for (String suffix : suffixes) {
                    if (line.startsWith(suffix)) {
                        chunk   = null;
                        matched = true;

                        break;
                    }
                }
                if (matched) {
                    continue;
                }



                if (chunk == null) {
                    chunks.add(chunk = new Chunk(Chunk.TYPE_WIKI));
                }
                chunk.append(line);
            }

            return chunks;
        }



    }


    /**
     * WikiPageHandler _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static interface WikiPageHandler {

        /**
         * _more_
         *
         * @param path _more_
         *
         * @return _more_
         */
        public String getHtdocsUrl(String path);

        /**
         * _more_
         *
         * @param wikiUtil _more_
         * @param name _more_
         * @param label _more_
         *
         * @return _more_
         */
        public String getWikiLink(WikiUtil wikiUtil, String name,
                                  String label);


        /**
         * _more_
         *
         * @param wikiUtil _more_
         * @param image _more_
         * @param props _more_
         *
         * @return _more_
         */
        public String getWikiImageUrl(WikiUtil wikiUtil, String image,
                                      Hashtable props);

        /**
         * _more_
         *
         * @param wikiUtil _more_
         * @param property _more_
         * @param notTags _more_
         *
         * @return _more_
         */
        public String getWikiPropertyValue(WikiUtil wikiUtil,
                                           String property, String[] notTags);
    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) throws Exception {
        String s =
            "\nhello there\n```vega\ncode block\nmore code\n```\n<nowiki>NOWIKI1\nNO WIKI2\n</nowiki>\nmore text\nand more text\n```\nmore code\n```\n";
        System.err.println(Utils.join(Chunk.splitText(s), "", false));



        //      String  p = "^```.*?\\[^```";   
        //      String s = "hello [asds] there\n```\n;

        /*


	  String s = "";
	  String s1 =
	  "hello there how are you ''''contents '''' and how are you";
	  for (int i = 0; i < 1000; i++) {
	  s = s + s1;
	  }
	  long t1 = System.currentTimeMillis();
	  for (int i = 0; i < 10000; i++) {
	  s.replaceAll("'''''([^']+)'''''", "<b><i>$1</i></b>");
	  }
	  long t2 = System.currentTimeMillis();
	  Utils.printTimes("t1:", t1, t2);

	  Pattern p   = Pattern.compile("'''''([^']+)'''''");
	  long    tt1 = System.currentTimeMillis();
	  for (int i = 0; i < 10000; i++) {
	  Matcher m = p.matcher(s);
	  m.replaceAll("<b><i>$1</i></b>");
	  }
	  long tt2 = System.currentTimeMillis();
	  Utils.printTimes("t2:", tt1, tt2);

        */
    }




}

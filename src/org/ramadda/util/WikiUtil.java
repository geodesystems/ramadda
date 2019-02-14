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


import org.ramadda.util.HtmlUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.util.regex.*;

import java.util.regex.*;



/**
 */
public class WikiUtil {

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
    private Hashtable properties;

    /** _more_ */
    private Hashtable wikiProperties = new Hashtable();

    /** _more_ */
    private Hashtable<String, String> myVars = new Hashtable<String,
                                                   String>();

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

    /**
     * _more_
     */
    public WikiUtil() {}

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
        sb.append(HtmlUtils.open(HtmlUtils.TAG_TABLE));
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
                    HtmlUtils.rowTop(
                        HtmlUtils.col(
                            name,
                            HtmlUtils.cssClass(
                                "wiki-infobox-entry-title")) + HtmlUtils.col(
                                    toks2[1],
                                    HtmlUtils.cssClass(
                                        "wiki-infobox-entry"))));

            }
        }
        sb.append(HtmlUtils.close(HtmlUtils.TAG_TABLE));
        String div = HtmlUtils.makeShowHideBlock(title, sb.toString(), true,
                         HtmlUtils.cssClass("wiki-infobox-title"),
                         HtmlUtils.cssClass("wiki-infobox"));
        div = wikify(div, null);
        floatBoxes.add(div);

        return "";
        //        return "<table class=\"wiki-toc-wrapper\" align=\"right\" width=\"30%\"><tr><td>"
        //                + div + "</td></tr></table><br clear=right>";
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
     * @param s _more_
     *
     * @return _more_
     */
    private static List<String> splitOnNoWiki(String s) {
        List<String> content    = new ArrayList<String>();
        int          idx        = s.indexOf("<nowiki>");
        int          tagLength1 = "<nowiki>".length();
        int          tagLength2 = "</nowiki>".length();
        //.... <nowiki>.....</nowiki> ....
        while ((idx >= 0) && (s.length() > 0)) {
            content.add(s.substring(0, idx));
            s = s.substring(idx + tagLength1);
            int idx2 = s.indexOf("</nowiki>");
            if (idx2 < 0) {
                content.add(s);
                s = "";

                break;
            }
            String nowiki = s.substring(0, idx2);
            content.add(nowiki);
            s   = s.substring(idx2 + tagLength2);
            idx = s.indexOf("<nowiki>");
        }
        content.add(s);

        return content;
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
        return wikify(text, handler, null);
    }

    /** _more_ */
    private String[] notTags;

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
            wikify(mainBuffer, text, handler, notTags);

            return mainBuffer.toString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
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
        List<String> toks   = splitOnNoWiki(text);
        boolean      isText = true;
        for (String s : toks) {
            if ( !isText) {
                isText = true;
                mainBuffer.append(s);

                continue;
            }
            isText = false;
            s      = wikifyInner(s, handler, notTags);
            mainBuffer.append(s);
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param handler _more_
     * @param notTags _more_
     *
     * @return _more_
     */
    private String wikifyInner(String s, WikiPageHandler handler,
                               String[] notTags) {

        s = s.replace("\\\\[", "_BRACKETOPEN_");
        if (getReplaceNewlineWithP()) {
            s = s.replaceAll("\r\n\r\n", "\n<p></p>\n");
            s = s.replaceAll("\r\r", "\n<p></p>\n");
        }
        //        System.err.println (s);
        s = s.replaceAll("'''''([^']+)'''''", "<b><i>$1</i></b>");
        s = s.replaceAll("'''([^']+)'''", "<b>$1</b>");
        s = s.replaceAll("''([^']+)''", "<i>$1</i>");
        Pattern pattern;
        Matcher matcher;
        //<nowiki>
        pattern = Pattern.compile("\\[\\[([^\\]|]+)\\|?([^\\]]*)\\]\\]");
        matcher = pattern.matcher(s);
        while (matcher.find()) {
            String name  = matcher.group(1);
            String label = matcher.group(2);
            int    start = matcher.start(0);
            int    end   = matcher.end(0);
            String link;
            if (handler == null) {
                if (label.trim().length() == 0) {
                    label = name;
                }
                link = "<a href=\"" + name + "\">" + label + "</a>";
            } else {
                link = handler.getWikiLink(this, name, label);
            }
            s       = s.substring(0, start) + link + s.substring(end);
            matcher = pattern.matcher(s);
        }

        int cnt = 0;
        pattern = Pattern.compile("\\[([^\\]]+)\\]");
        matcher = pattern.matcher(s);
        while (matcher.find()) {
            String name  = matcher.group(1).trim();
            int    idx   = name.indexOf(" ");
            int    start = matcher.start(0);
            int    end   = matcher.end(0);
            if (idx > 0) {
                String label = name.substring(idx);
                name = name.substring(0, idx);
                String ahref =
                    "<a title=\"" + name
                    + "\" class=\"wiki-link-external\" target=\"_blank\" href=\""
                    + name + "\">";
                s = s.substring(0, start) + ahref + label + "</a>"
                    + s.substring(end);
            } else {
                cnt++;
                String ahref =
                    "<a title=\"" + name
                    + "\" class=\"wiki-link-external\" target=\"_blank\" href=\""
                    + name + "\">";
                s = s.substring(0, start) + ahref + "_BRACKETOPEN_" + cnt
                    + "_BRACKETCLOSE_</a>" + s.substring(end);
            }
            matcher = pattern.matcher(s);
        }





        List headings = new ArrayList();
        pattern = Pattern.compile("(?m)^\\s*(==+)([^=]+)(==+)\\s*$");
        matcher = pattern.matcher(s);
        while (matcher.find()) {
            String prefix = matcher.group(1).trim();
            String label  = matcher.group(2).trim();
            //            System.err.println("MATCH " + prefix + ":" + label);
            int    start = matcher.start(0);
            int    end   = matcher.end(0);
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
            s       = s.substring(0, start) + value + s.substring(end);
            matcher = pattern.matcher(s);
        }

        boolean       closeTheTag  = false;


        int           ulCnt        = 0;
        int           olCnt        = 0;
        StringBuffer  buff         = new StringBuffer();

        StringBuilder js           = new StringBuilder();
        List<TabState> allTabStates  = new ArrayList<TabState>();
        List<TabState> tabInfos     = new ArrayList<TabState>();
        List<String>  accordianIds = new ArrayList<String>();

        List<TableState> tableStates  = new ArrayList<TableState>();
        for (String line :
                (List<String>) StringUtil.split(s, "\n", false, false)) {
            String tline = line.trim();
            if (tline.startsWith("+table")) {
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                String width = "100%";
                String height = null;
                String ordering = null;
                String paging = null;
                String xclazz = null;
                String searching = "false";
                String clazz = "ramadda-table";
                if(toks.size()==2) {
                    Hashtable props = StringUtil.parseHtmlProperties(toks.get(1));
                    width  = Utils.getProperty(props, "width", width);
                    height  = Utils.getProperty(props, "height",height);
                    ordering  = Utils.getProperty(props, "ordering",ordering);
                    paging  = Utils.getProperty(props, "paging",paging);
                    searching  = Utils.getProperty(props, "searching",height);

                    if(Misc.equals(Utils.getProperty(props, "rowborder",null),"true"))
                        clazz= "row-border " + clazz;
                    if(Misc.equals(Utils.getProperty(props, "cellborder",null),"true"))
                        clazz= "cell-border " + clazz;
                    if(Misc.equals(Utils.getProperty(props, "stripe",null),"true"))
                        clazz= "stripe " + clazz;
                    if(Misc.equals(Utils.getProperty(props, "hover",null),"true"))
                        clazz= "hover " + clazz;
                }

                buff.append("<table class='" + clazz  +"' width=" + width + " table-searching=" + searching +" "  + 
                            (height!=null?" table-height=" + height :"") +
                            (ordering!=null?" table-ordering=" + ordering :"") +
                            (paging!=null?" table-paging=" + paging :"") +
                            "><thead>");
                tableStates.add(new TableState());
                continue;
            }
            if (tline.equals("-table")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                if(state.inTd)
                    buff.append("</td>");
                if(state.inTr)
                    buff.append("</tr>");
                if(state.inHead)
                    buff.append("</thead>");
                if(state.inBody)
                    buff.append("</tbody>");
                buff.append("</table>");
                tableStates.remove(tableStates.size()-1);

                continue;
            }
            if (tline.startsWith(":tr")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                buff.append("<tr valign=top>");
                if(toks.size()==2) {
                    for(String td:Utils.parseCommandLine(toks.get(1))) {
                        if(state.inHead)
                            buff.append(HtmlUtils.th(td));
                        else
                            buff.append(HtmlUtils.td(td));
                    }
                }
                if(state.inHead) {
                    buff.append("</thead>");
                    buff.append("<tbody>");
                    state.inHead = false;
                    state.inBody = true;
                }
                continue;
            }
            if (tline.startsWith("+tr")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                buff.append("<tr valign=top>");
                continue;
            }
            if (tline.startsWith("-tr")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                buff.append("</tr>");
                if(state.inHead) {
                    buff.append("</thead>");
                    buff.append("<tbody>");
                    state.inHead = false;
                    state.inBody = true;
                }
                continue;
            }

            if (tline.startsWith("+td")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                String width = null;
                if(toks.size()==2) {
                    Hashtable props = StringUtil.parseHtmlProperties(toks.get(1));
                    width  = Utils.getProperty(props, "width", width);
                }

                if(state.inHead)
                    buff.append("<th " + (width!=null?" width=" + width:"")+">");
                else
                    buff.append("<td valign=top " + (width!=null?" width=" + width:"")+">");
                continue;
            }
            if (tline.startsWith("-td")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                if(state.inHead)
                    buff.append("</th>");
                else
                    buff.append("</td>");
                continue;
            }
            if (tline.startsWith(":td")) {
                TableState state=tableStates.size()>0?tableStates.get(tableStates.size()-1):null;
                if(state==null) {
                    buff.append("Not in a table");
                    continue;
                }
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                String td = toks.size()==2?toks.get(1):"";
                if(state.inHead)
                    buff.append(HtmlUtils.th(td));
                else
                    buff.append(HtmlUtils.td(td,"valign=top"));
                continue;
            }


            if (tline.startsWith("+tabs")) {
                TabState tabInfo = new TabState();
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                if(toks.size()==2) {
                    Hashtable props = StringUtil.parseHtmlProperties(toks.get(1));
                    tabInfo.minHeight = (String) props.get("minHeight");
                    if(tabInfo.minHeight!=null && !tabInfo.minHeight.endsWith("px"))
                        tabInfo.minHeight = tabInfo.minHeight+"px";
                }
                tabInfos.add(tabInfo);
                allTabStates.add(tabInfo);
                buff.append("\n");
                HtmlUtils.open(buff, HtmlUtils.TAG_DIV, "id", tabInfo.id,
                               "class", "ui-tabs");
                buff.append("\n");
                HtmlUtils.open(tabInfo.title, HtmlUtils.TAG_UL);
                tabInfo.title.append("\n");
                buff.append("\n");
                buff.append("${" + tabInfo.id + "}");
                buff.append("\n");

                continue;
            }
            if (tline.equals("+tab") || tline.startsWith("+tab ")) {
                if (tabInfos.size() == 0) {
                    buff.append("No +tabs tag");
                    continue;
                }
                List<String> toks    = StringUtil.splitUpTo(tline, " ", 2);
                String       title   = (toks.size() > 1)
                                       ? toks.get(1)
                                       : "";
                TabState      tabInfo = tabInfos.get(tabInfos.size() - 1);
                tabInfo.cnt++;
                tabInfo.title.append("<li><a href=\"#" + tabInfo.id + "-"
                                     + (tabInfo.cnt) + "\">" + title
                                     + "</a></li>\n");
                String style = "";
                if(tabInfo.minHeight!=null)
                    style = " style=min-height:" + tabInfo.minHeight+";";
                buff.append(
                    HtmlUtils.open(
                        "div",
                        style +
                        HtmlUtils.id(tabInfo.id + "-" + (tabInfo.cnt))
                        + HtmlUtils.cssClass("ui-tabs-hide")));
                buff.append("\n");

                continue;
            }
            if (tabInfos.size() > 0) {
                if (tline.equals("-tab")) {
                    TabState tabInfo = tabInfos.get(tabInfos.size() - 1);
                    buff.append(HtmlUtils.close("div"));
                    buff.append("\n");
                    js.append(
                        "jQuery(function(){\njQuery('#" + tabInfo.id
                        + "').tabs({activate: HtmlUtil.tabLoaded})});\n");

                    continue;
                }
                if (tline.equals("-tabs")) {
                    TabState tabInfo = tabInfos.get(tabInfos.size() - 1);
                    tabInfo.title.append("\n");
                    tabInfo.title.append("</ul>");
                    tabInfo.title.append("\n");
                    tabInfos.remove(tabInfos.size() - 1);
                    buff.append(HtmlUtils.close("div"));

                    continue;
                }


            }

            if (tline.equals("+accordian")) {
                String accordianId = HtmlUtils.getUniqueId("accordian");
                accordianIds.add(accordianId);
                buff.append("\n");
                buff.append(
                    HtmlUtils.open(
                        HtmlUtils.TAG_DIV,
                        HtmlUtils.cssClass(
                            " ui-accordion ui-widget ui-helper-reset") + HtmlUtils.id(
                            accordianId)));
                buff.append("\n");

                continue;
            }
            if (accordianIds.size() > 0) {
                if (tline.equals("-accordian")) {
                    buff.append("\n");
                    buff.append("</div>");
                    buff.append("\n");
                    String accordianId = accordianIds.get(accordianIds.size()
                                             - 1);
                    accordianIds.remove(accordianIds.size() - 1);
                    String args =
                        "{autoHeight: false, navigation: true, collapsible: true, active: 0}";
                    js.append("HtmlUtil.makeAccordian(\"#" + accordianId
                              + "\" " + "," + args + ");\n");
                    buff.append("\n");

                    continue;
                }

                if (tline.startsWith("+segment")) {
                    List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                    String       title = (toks.size() > 1)
                                         ? toks.get(1)
                                         : "";

                    buff.append("\n");
                    buff.append(
                        HtmlUtils.open(
                            HtmlUtils.TAG_H3,
                            HtmlUtils.cssClass(
                                " ui-accordion-header ui-helper-reset ui-state-active ui-corner-top")));
                    buff.append("\n");
                    buff.append("<a href=\"#\">");
                    buff.append(title);
                    buff.append("</a></h3>");
                    buff.append("\n");
                    String contentsId =
                        HtmlUtils.getUniqueId("accordion_contents_");
                    buff.append(
                        HtmlUtils.open(
                            "div",
                            HtmlUtils.id(contentsId)
                            + HtmlUtils.cssClass(
                                "ramadda-accordian-contents")));
                    buff.append("\n");

                    continue;
                }
                if (tline.startsWith("-segment")) {
                    buff.append("\n");
                    buff.append("</div>");
                    buff.append("\n");

                    continue;
                }
            }

            if (tline.equals("-div")) {
                buff.append("</div>");

                continue;
            }
            if (tline.startsWith("+pagehead")) {
                String weight = StringUtil.findPattern(tline, "-([0-9]+)");
                if (weight == null) {
                    weight = "8";
                }
                buff.append("<div class=\"row\">");
                buff.append("<div class=\"col-md-" + weight + "\">");
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
                    for (String side : new String[] { "top", "left", "bottom",
                            "right" }) {
                        String v = getAttribute(tline, side);
                        if (v != null) {
                            styles.append("margin-" + side + ":" + v + "px;");
                        }
                    }

                    if (styles.length() > 0) {
                        extra.append(HtmlUtils.style(styles.toString()));
                    }
                }

                buff.append(HtmlUtils.open("div",
                                           HtmlUtils.cssClass("inset")
                                           + extra));

                continue;
            }
            if (tline.equals("-inset")) {
                buff.append("</div>");

                continue;
            }


            if (tline.startsWith("+info-sec")
                    || tline.startsWith("+section")) {

                String  label      = getAttribute(tline, "label");
                String  heading    = getAttribute(tline, "heading");
                String  title      = getAttribute(tline, "title");
                String  classArg   = getAttribute(tline, "class");
                String  extraArg   = getAttribute(tline, "style");
                boolean doEvenOdd  = tline.indexOf("#") >= 0;
                String  extraClass = "";
                String  extraAttr  = ((extraArg == null)
                                      ? ""
                                      : " style=\"" + extraArg + "\" ");
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
                    extraClass = classArg;
                }

                String clazz = "ramadda-section " + extraClass;
                buff.append("<div class=\"");
                buff.append(clazz);
                buff.append("\"   " + extraAttr + ">");
                if (label == null) {
                    label = heading;
                }
                if (label != null) {
                    buff.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                            HtmlUtils.cssClass("ramadda-heading-outer")));
                    buff.append(HtmlUtils.div(label,
                            HtmlUtils.cssClass("ramadda-heading")));
                    buff.append(HtmlUtils.close(HtmlUtils.TAG_DIV));
                }
                if (title != null) {
                    buff.append(HtmlUtils.div(getTitle(title),
                            HtmlUtils.cssClass("ramadda-page-title")));
                }

                continue;
            }
            if (tline.startsWith("-info-sec")
                    || tline.startsWith("-section")) {
                buff.append("</div>");

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
                    buff.append(HtmlUtils.div(getTitle(toks.get(1)),
                            HtmlUtils.cssClass("ramadda-page-title")));
                }

                continue;
            }


            if (tline.startsWith("+title")) {
                StringBuilder extra = new StringBuilder();
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                HtmlUtils.open(buff, "div",
                               HtmlUtils.cssClass("ramadda-page-title"));
                String url = getTitleUrl(true);
                if (url != null) {
                    closeTheTag = true;
                    HtmlUtils.open(buff, "a", "href=\"" + url + "\"");
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

            if (tline.startsWith(":heading") || tline.startsWith(":block")
                    || tline.startsWith(":note") || tline.startsWith(":box")
                    || tline.startsWith(":blurb")
                    || tline.startsWith(":callout")) {
                List<String> toks  = StringUtil.splitUpTo(tline, " ", 2);
                String       what  = toks.get(0).substring(1);
                List<String> toks2 = StringUtil.splitUpTo(what, "-", 2);
                what = toks2.get(0);
                String clazz = toks.get(0).trim().substring(1);
                String text  = (toks.size() > 1)
                               ? toks.get(1)
                               : "";
                if ( !clazz.equals(what)) {
                    clazz = "ramadda-" + what + "  ramadda-" + clazz;
                } else {
                    clazz = "ramadda-" + what;
                }
                buff.append(
                    HtmlUtils.div(
                        HtmlUtils.div(text, HtmlUtils.cssClass(clazz)),
                        HtmlUtils.cssClass("ramadda-" + what + "-outer")));

                continue;
            }


            if (tline.startsWith("+mini") || tline.startsWith("+block")
                    || tline.startsWith("+note") || tline.startsWith("+box")
                    || tline.startsWith("+heading")
                    || tline.startsWith("+blurb")
                    || tline.startsWith("+callout")) {
                List<String>  toks      = StringUtil.splitUpTo(tline, " ", 2);
                String        tag       = toks.get(0).substring(1);
                //box-green

                List<String>  toks2     = StringUtil.splitUpTo(tag, "-", 2);
                String        what      = toks2.get(0);
                //box

                String        remainder = ((toks2.size() > 1)
                                           ? toks2.get(1)
                                           : "");
                //green

                StringBuilder extra     = new StringBuilder();
                String        style     = getAttribute(tline, "style");
                if (style != null) {
                    extra.append(HtmlUtils.style(style));
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
                buff.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                           HtmlUtils.cssClass("ramadda-"
                                               + what + "-outer")));
                buff.append(HtmlUtils.open("div", extra.toString()));

                continue;
            }

            if (tline.startsWith("-mini") || tline.startsWith("-block")
                    || tline.startsWith("-heading")
                    || tline.startsWith("-note") || tline.startsWith("-box")
                    || tline.startsWith("-blurb")
                    || tline.startsWith("-callout")) {
                buff.append("</div></div>");

                continue;
            }

            if (tline.startsWith(":col-")) {
                List<String> toks     = StringUtil.splitUpTo(tline, " ", 2);
                String       clazz    = toks.get(0).substring(1);
                String       contents = "";
                if (toks.size() > 1) {
                    contents = toks.get(1);
                }
                buff.append(HtmlUtils.div(contents,
                                          HtmlUtils.cssClass(clazz)));

                continue;
            }


            if (tline.equals("+row")) {
                buff.append("<div class=\"row\">");

                continue;
            }
            if (tline.equals("-row")) {
                buff.append("</div>");

                continue;
            }

            if (tline.startsWith("+col-")) {
                List<String>  toks  = StringUtil.splitUpTo(tline, " ", 2);
                StringBuilder extra = new StringBuilder();
                String        clazz = toks.get(0).substring(1);
                if (toks.size() > 1) {
                    String attrs = toks.get(1);
                    String style = getAttribute(attrs, "style");
                    if (style != null) {
                        extra.append(HtmlUtils.style(style));
                    }
                    clazz = clazz + " " + getAttribute(attrs, "class", "");
                }
                if (clazz.matches("col-[0-9]+")) {
                    clazz = clazz.replace("col-", "col-md-");
                }
                buff.append(HtmlUtils.open("div",
                                           HtmlUtils.cssClass(clazz)
                                           + extra));

                continue;
            }

            if (tline.startsWith("-col")) {
                buff.append("</div>");

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
            while (tline.startsWith("#") && tline.indexOf("=")<0) {
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

        while (ulCnt > 0) {
            buff.append("</ul>\n");
            ulCnt--;
        }
        while (olCnt > 0) {
            buff.append("</ol>\n");
            olCnt--;
        }
        if (js.length() > 0) {
            HtmlUtils.script(buff, js.toString());
        }
        s = buff.toString();
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
            int idx2 = s.indexOf(TAG_SUFFIX, idx1);
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
            Hashtable props = StringUtil.parseHtmlProperties(attrs);
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
                        sb.append(HtmlUtils.makeShowHideBlock(title, inner,
                                open, HtmlUtils.cssClass("wiki-blockheader"),
                                HtmlUtils.cssClass("wiki-block")));
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
                String block = HtmlUtils.makeShowHideBlock("Contents",
                                   toc.toString(), true,
                                   HtmlUtils.cssClass("wiki-tocheader"),
                                   HtmlUtils.cssClass("wiki-toc"));
                floatBoxes.add(block);

                String blocks =
                    "<table class=\"wiki-toc-wrapper\" align=\"right\" width=\"30%\"><tr><td>"
                    + StringUtil.join("<br>", floatBoxes)
                    + "</td></tr></table>";
                s = blocks + s;
            }
        }

        if (categoryLinks.size() > 0) {
            s = s + HtmlUtils.div(
                "<b>Categories:</b> "
                + StringUtil.join(
                    "&nbsp;|&nbsp; ", categoryLinks), HtmlUtils.cssClass(
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
     * @param label _more_
     *
     * @return _more_
     */
    public String getTitle(String label) {
        String url = getTitleUrl(true);

        return (url != null)
               ? HtmlUtils.href(url, label)
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
            toc.append(HtmlUtils.space(1));
            toc.append(label);
            toc.append("</a><br>\n");
            currentLevel = level;
        }

    }


    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        try {
            //            String contents = IOUtil.readContents(new java.io.File(args[0]));
            //            contents = new WikiUtil().wikify(contents, null);
            //            System.out.println("\ncontents:" + contents);
            for (String c : new String[] {
                "just text", "<nowiki>no wiki</nowiki>",
                "text<nowiki>no wiki</nowiki>",
                "text<nowiki>no wiki</nowiki>more text",
                "text<nowiki>no wiki</nowiki>more text<nowiki>more no wiki</nowiki>",
                "text<nowiki>no wiki</nowiki>more text<nowiki>more no wiki</nowiki>and more text"
            }) {
                System.out.println("Content:" + c);
                System.out.println(splitOnNoWiki(c));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
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
     * @version        $version$, Sun, Jan 27, '19
     * @author         Enter your name here...
     */
    public static class TabState {

        /** _more_ */
        String id;

        /** _more_ */
        StringBuilder title = new StringBuilder();

        /** _more_ */
        int cnt = 0;

        String minHeight;

        /**
         * _more_
         */
        public TabState() {
            this.id = HtmlUtils.getUniqueId("tabs");
        }



    }


    public static class TableState {

        boolean inHead=true;
        boolean inRow=false;
        boolean inBody=false;
        boolean inTr=false;
        boolean inTd=false;

        /**
         * _more_
         */
        public TableState() {
        }



    }

}

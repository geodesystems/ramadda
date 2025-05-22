// Copyright (c) 2008-2024 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0


package org.ramadda.util;


import org.json.*;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.IOException;

import java.net.URL;

import java.text.SimpleDateFormat;
import java.util.TimeZone;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.*;



@SuppressWarnings("unchecked")
public class WikiUtil implements HtmlUtilsConstants {
    private static final HtmlUtils HU = null;
    public static final String NAVDELIM =  "&#9675;";
    public static final String ATTR_OPEN = "open";
    public static final String ATTR_VAR = "var";
    public static final String ATTR_DECORATE = "decorate";
    public static final String ATTR_SHOW = "show";
    public static final String PROP_NOHEADING = "noheading";
    public static final String PROP_NOP = "nop";
    public static final String PROP_DOP = "dop";
    public static final String PROP_HEADING = "heading";
    public static final String TAG_PREFIX = "{{";
    public static final String TAG_SUFFIX = "}}";
    private static Hashtable globalProperties;
    private Hashtable properties;
    private Hashtable wikiProperties = new Hashtable();
    private Hashtable wikiAttributes = new Hashtable();
    private StringBuilder javascript = new StringBuilder();
    private Hashtable<String, String> myVars = new Hashtable<String,String>();
    private Hashtable<String, String> rawMacros;
    private Hashtable<String, String> macros;    
    private Hashtable<String,String> templates=  new Hashtable<String,String>();
    private boolean hasSet = false;
    private List categoryLinks = new ArrayList();
    private List floatBoxes = new ArrayList();
    private boolean makeHeadings = false;
    private boolean replaceNewlineWithP = true;
    private boolean mobile = false;
    private String user;
    private HashSet notTags;
    private HashSet  seen = new HashSet();
    private WikiPageHandler handler;
    List headings2 = new ArrayList();

    private List<TabState>       allTabStates    = new ArrayList<TabState>();
    private List<TabState>       tabStates       = new ArrayList<TabState>();
    private List<RowState>       rowStates       = new ArrayList<RowState>();
    private List<AccordionState> accordionStates = new ArrayList<AccordionState>();
    private List<TableState> tableStates       = new ArrayList<TableState>();


    public WikiUtil() {}

    public WikiUtil(WikiUtil that) {
        this.properties = that.properties;
	this.templates = that.templates;
    }

    public WikiUtil(Hashtable properties) {
        this.properties = properties;
    }
    
    public void appendJavascript(String code) {
        if ((code == null) || (code.trim().length() == 0)) {
            return;
        }
	if(javascript==null) javascript = new StringBuilder();
        javascript.append(code);
        javascript.append("\n");
    }
    
    public String getJavascript(boolean andClear) {
	if(javascript==null) return null;
	String j = javascript.toString();
	if(andClear) javascript = null;
        return j;
    }

    public void addTemplate(String name, String value) {
	templates.put(name, value);
    }


    public static void setGlobalProperties(Hashtable value) {
        globalProperties = value;
    }

    public static Hashtable getGlobalProperties() {
        return globalProperties;
    }

    public void removeProperty(Object key) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.remove(key);
    }

    public void putProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key, value);
    }
    
    public void putWikiProperty(Object key, Object value) {
        if (value == null) {
            wikiProperties.remove(key);
        } else {
            wikiProperties.put(key, value);
        }
    }
    
    public Hashtable getWikiProperties() {
	return wikiProperties;

    }

    public Object getWikiProperty(Object key) {
        return wikiProperties.get(key);
    }
    
    public String getMacro(String key) {
	if(rawMacros==null) return null;
	return rawMacros.get(key);
    }

    public Object getWikiProperty(Hashtable props, Object... args) {
        Object dflt = args[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            Object key = args[i];
            Object v   = props.get(key);
            if (v == null) {
                v = getWikiProperty(key);
            }
            if (v != null) {
                return v;
            }
        }

        return dflt;
    }
  
    public void removeWikiProperty(Object key) {
        wikiProperties.remove(key);
    }
    
    public void putWikiAttribute(Object key, Object value) {
        wikiAttributes.put(key, value);
    }
    
    public Hashtable getWikiAttributes() {
        return wikiAttributes;
    }
    
    public void clearWikiAttributes() {
        wikiAttributes = new Hashtable();
    }
    
    public void addWikiAttributes(List l) throws Exception {
        for (Enumeration keys =
		 wikiAttributes.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) wikiAttributes.get(key);
            l.add(key);
            l.add(JsonUtil.quote(value));
        }
    }
    
    public void addCategoryLink(String link) {
        categoryLinks.add(link);
    }
    
    public Object getProperty(Object key) {
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }

    public void setProperty(Object key,Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key,value);
    }

    public Hashtable getProperties() {
	return properties;
    }
    
    public boolean getProperty(Hashtable props, String prop, boolean dflt) {
        String v = getProperty(props, prop, (String) null);
        if (v == null) {
            return dflt;
        }

        return Boolean.valueOf(v);
    }
    
    public static String getProperty(WikiUtil wikiUtil, Hashtable props,
                                     String prop, String dflt) {
        return wikiUtil.getProperty(props, prop, dflt);
    }
    
    public String getProperty(Hashtable props, String prop, String dflt) {
        String value = Utils.getProperty(props, prop, (String) null);
        if (value == null) {
            value = Utils.getProperty(props, prop.toLowerCase(),
                                      (String) null);
        }
        if (value == null) {
            value = (String) this.getWikiProperty(prop);
            if (value == null) {
                value = (String) this.getWikiProperty(prop.toLowerCase());
            }
        }
        if (value == null) {
            return dflt;
        }

        return value;
    }
   
    public String getInfoBox(String property) {
        StringBuffer sb = new StringBuffer();
        List<String> toks = (List<String>) Utils.split(property, "\n",
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
				    HU.col(name, HU.cssClass("wiki-infobox-entry-title"))
				    + HU.col(
					     toks2[1], HU.cssClass("wiki-infobox-entry"))));

            }
        }
        sb.append(HU.close(HU.TAG_TABLE));
        String div = HU.makeShowHideBlock(title, sb.toString(), true,
                                          HU.cssClass("wiki-infobox-title"),
                                          HU.cssClass("wiki-infobox"));
        floatBoxes.add(wikify(div, null));

        return "";
    }

   
    public String getPropertyValue(String property) {
        if (property.startsWith("Infobox")) {
            return getInfoBox(property);
        }

        return null;
    }
    
    public String wikify(String text, WikiPageHandler handler) {
        this.handler = handler;
        return wikify(text, handler, null);
    }
    
    public String wikify(String text, WikiPageHandler handler,
                         HashSet notTags) {
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
    
    public void wikify(Appendable mainBuffer, String text,
                       WikiPageHandler handler)
	throws IOException {
        wikify(mainBuffer, text, handler, new HashSet());
    }
   
    public void wikify(Appendable mainBuffer, String text,
                       WikiPageHandler handler, HashSet notTags)
	throws IOException {
        if (text.startsWith("<wiki>")) {
            text = text.substring("<wiki>".length());
            text = text.replaceFirst("\\s*\\R?", "");
        }
	String wikiId  = HU.getUniqueId("wiki_");
	text =text.replace("${wikiid}",wikiId);
        List<Chunk> chunks = Chunk.splitText(this,text);
        String      s      = wikifyInner(chunks, handler, notTags);
        mainBuffer.append(s);
    }
    
    private String applyPatterns(WikiPageHandler handler, List headings,
                                 String s)
	throws IOException {
        if (getReplaceNewlineWithP()) {
            s = s.replaceAll("\r\n\r\n", "\n<p></p>\n");
            s = s.replaceAll("\r\r", "\n<p></p>\n");
        }

        s = s.replace("\\[", "_BRACKETOPEN_");
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
        Pattern pattern2 = Pattern.compile("\\[([^\\]]+)\\]",Pattern.DOTALL);
        Matcher matcher2 = pattern2.matcher(s);
        while (matcher2.find()) {
            String name  = matcher2.group(1).trim();
	    String target= null;
	    if(name.startsWith("<")) {
		int idx2 = name.indexOf(">");
		if(idx2>=0) {
		    target = name.substring(1,idx2);
		    name = name.substring(idx2+1);
		} else {
		    name = name.substring(1);
		    target="_blank";
		}
	    }
            int    idx   = name.indexOf(" ");
            int    nlidx   = name.indexOf("\n");	    
	    if(idx==-1) idx = nlidx;
	    else if(nlidx>=0) idx = Math.min(idx,nlidx);
            int    start = matcher2.start(0);
            int    end   = matcher2.end(0);
	    
            if (idx > 0) {
                String label = name.substring(idx);
                name = name.substring(0, idx);
		String attrs = HU.attrs("title",name,"class","wiki-link-external",
					"href",name);
		if(target!=null) attrs+=HU.attr("target",target);
                String ahref =HU.open("a",attrs);
                s = s.substring(0, start) + ahref + label + "</a>"
                    + s.substring(end);
            } else {
                cnt++;
		String attrs = HU.attrs("title",name,"class","wiki-link-external",
					"href",name);
		if(target!=null) attrs+=HU.attr("target",target);
                String ahref =HU.open("a",attrs);
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
                headings.add(new Object[] {  Integer.valueOf(level), label });
            }
            s        = s.substring(0, start) + value + s.substring(end);
            matcher3 = pattern3.matcher(s);
        }

        s = s.replaceAll("\r", "");

        return s;
    }

    private String getHeadingLabel(String label) {
	String tmp = StringUtil.findPattern(label,"<label>(.*)</label>");
	if(tmp!=null) label = tmp;
	return label;
    }

    private String wikifyInner(List<Chunk> chunks, WikiPageHandler handler,
                               HashSet notTags)
	throws IOException {

        List      headings      = new ArrayList();
        String    headingsNav   = null;
        Hashtable headingsProps = null;
	Function<String,Hashtable> getProps = (tline)->{
	    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
	    Hashtable props = HU.parseHtmlProperties(toks.size()>1?toks.get(1):"");
	    return props;
	};

        Utils.TriConsumer<StringBuffer,String,Integer> defineHeading = (sb,label,level) -> {
	    label = getHeadingLabel(label);
	    String id = Utils.makeID(label);
	    String noTags = label.replaceAll("<[^>]+>[^<]*<[^>]+>","").trim().replaceAll("-+$","").trim();
	    if(noTags.length()>0) {
		label = noTags;
	    }  else {
		label = Utils.stripTags(label).trim();
	    }
            //      String id = "heading_" + HU.blockCnt++;
            headings2.add(new Object[]{id, label,level});
            sb.append("<a class=ramadda-nav-anchor name='" + id +"'></a>");
        };


	Utils.QuadConsumer<StringBuffer,String,String,Integer> headingLinker = (sb,label,tag,level) ->{
	    defineHeading.accept(sb,label,level);
	    String id = "heading-" +Utils.makeID(label);
	    sb.append(HU.anchorName(id));
	    label += HU.span("",HU.attrs("id",id+"-hover",ATTR_CLASS,"ramadda-linkable-link"));
	    String attrs = HU.attrs("id",id,ATTR_CLASS,"ramadda-linkable");
	    HU.tag(sb,tag,attrs, HU.div(label,HU.attrs(ATTR_CLASS,"ramadda-heading-inner")));
	};


        boolean              closeTheTag     = false;
        int                  ulCnt           = 0;
        int                  olCnt           = 0;
        StringBuffer         buff            = new StringBuffer();
        StringBuilder        js              = new StringBuilder("\n");




        String           currentVar        = null;
        StringBuilder    currentVarValue   = null;
	String templateName = null;
	StringBuilder templateSB = null;
	String applyTemplateName = null;
	Hashtable<String,String> applyTemplateVars= null;
	String applyVarName = null;
	StringBuilder applySB=null;
	
	List<NamedList<String>> repeatList = null;
        StringBuilder    repeatBuffer = null;
        StringBuilder    splashBuffer = null;
        boolean          inScroll          = false;
        String           slidesId           = null;
        Hashtable        slidesProps       = null;
        List<String> slideTitles=null;
        String           afterId           = null;
        String           afterPause        = null;
        String           afterFade         = null;
        boolean          inPropertyTag     = false;
        String           dragId            = null;
        boolean          dragToggle        = false;
        boolean          dragToggleVisible = false;

	String navId = null;


        String           leftWidth          = "50%";
	String           middleWidth          = "50%";
	String           rightWidth          = "50%";
        String           leftStyle          = "";
	String           middleStyle          = "";
	String           rightStyle          = "";	
	int menuCnt = 0;
	List<String> menuIds = new ArrayList<String>();

	Utils.UniConsumer<String> segmentOpen = (tline) ->{
	    if (accordionStates.size() == 0) {
		buff.append("No open accordion tag");
		return;
	    }
	    AccordionState accordionState =
	    accordionStates.get(accordionStates.size() - 1);
	    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
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
	    buff.append(HU.span(title,"class=ramadda-clickable"));
	    buff.append("</a></h3>");
	    buff.append("\n");
	    String contentsId = HU.getUniqueId("accordion_contents_");
	    buff.append(
                        HU.open(
				TAG_DIV,
				HU.id(contentsId)
				+ HU.cssClass("ramadda-accordion-contents")));
	    buff.append("\n");
	    accordionState.segmentId++;
	};

	Utils.UniConsumer<String> segmentClose = (tline) ->{
	    buff.append("\n");
	    buff.append("</div>");
	    buff.append("\n");
	};

        for (Chunk chunk : chunks) {
            if (chunk.type == chunk.TYPE_CODE) {
		//                buff.append("<nowiki>");
                handleCode(buff, chunk, handler, true);
		//                buff.append("</nowiki>");
                continue;
            }

            if (chunk.type == chunk.TYPE_NOWIKI) {
		//                buff.append("<nowiki>");
                buff.append(chunk.buff);
		//                buff.append("</nowiki>");
                continue;
            }
            if (chunk.type == chunk.TYPE_CSS) {
		//                buff.append("<nowiki>");
                buff.append("<style type='text/css'>\n");
                buff.append(chunk.buff);
                buff.append("</style>\n");
		//                buff.append("</nowiki>");
                continue;
            }
            if (chunk.type == chunk.TYPE_JS) {
		//                buff.append("<nowiki>");
                buff.append("\n<script type='text/JavaScript'>\n");
                buff.append(chunk.buff);
                buff.append("\n</script>\n");
		//                buff.append("</nowiki>");
                continue;
            }
            if (chunk.type == chunk.TYPE_JSTAG) {
		//                buff.append("<nowiki>");
                buff.append("\n<script");
		if(Utils.stringDefined(chunk.attrs)) {
		    buff.append(" ");
		    buff.append(chunk.attrs);
		}
		buff.append(">");
                buff.append(chunk.buff);
                buff.append("\n</script>\n");
		//                buff.append("</nowiki>");
                continue;
            }
            if (chunk.type == chunk.TYPE_PRETAG) {
		//                buff.append("<nowiki>");
                buff.append("\n<pre");
		if(Utils.stringDefined(chunk.attrs)) {
		    buff.append(" ");
		    buff.append(chunk.attrs);
		}
		buff.append(">");
                buff.append(chunk.buff);
                buff.append("</pre>\n");
		//                buff.append("</nowiki>");
                continue;
            }


            if (chunk.type == chunk.TYPE_PRE || chunk.type==chunk.TYPE_XML) {
		String id = HU.getUniqueId("block_");
		String attrs = HU.attrs("id",id);
		Hashtable props =  HU.parseHtmlProperties(chunk.rest!=null?chunk.rest:"");
		boolean addLink  = false;
		if(Utils.getProperty(props,"addCopy",false)) {
		    attrs+=HU.attrs("add-copy","true");
		    addLink=true;
		}
		if(Utils.getProperty(props,"addDownload",false)) {
		    addLink=true;
		    attrs+=HU.attrs("add-download","true");
		    attrs+=HU.attrs("download-file",Utils.getProperty(props,"downloadFile",Utils.getProperty(props,"downloadFileName",chunk.type == chunk.TYPE_PRE?"download.txt":"download.xml")));
		}


		buff.append("\n<pre ");
		buff.append(attrs);
		if(Utils.stringDefined(chunk.rest)) {
		    buff.append(" ");
		    buff.append(chunk.rest);
		}
		buff.append(">");
                String s = chunk.buff.toString().replaceAll("\\{\\{",
							    "{<noop>{");
		if(chunk.type==chunk.TYPE_XML) {
		    s=s.replace("<","&lt;").replace(">","&gt;");
		}
                buff.append(s);
                buff.append("</pre>\n");
		if(addLink)
		    HU.script(buff,"Utils.addCopyLink('" +id+"');");

		//                buff.append("</nowiki>");
                continue;
	    }


            String text = chunk.buff.toString();
            text = applyPatterns(handler, headings, text);
	    boolean skipping = false;
	    int avatarCount=0;
	    String balloonAfter = null;
	    Hashtable enlargeProps = null;
	    Hashtable context = new Hashtable();
	    boolean inMermaid=false;


            for (String line : text.split("\n")) {
                if ((line.indexOf("${") >= 0)
		    && (hasSet || (globalProperties != null))) {
                    if (macros != null) {
                        for (java.util.Enumeration keys = macros.keys();
			     keys.hasMoreElements(); ) {
                            String key   = (String)keys.nextElement();
                            String value = (String)macros.get(key);
			    key ="${" + key + "}";
			    if(line.indexOf(key)>=0) {
				value = wikify(value, handler);
				line = line.replace(key,   value);
			    }
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

                if (tline.equals("+skip")) {
		    skipping =true;
		    continue;
		}


                if (tline.equals("-skip")) {
		    skipping =false;
		    continue;
		}


		if(skipping) {
		    continue;
		}


                if (tline.startsWith("+template")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    templateName = toks.size()==2?toks.get(1): "template";
		    templateSB = new StringBuilder();
		    //		    System.err.println("Put:" + templateName+":");
		    continue;
		}

		if (tline.startsWith("-template")) {
		    if(templateSB==null) {
			wikiError(buff, "Error: No +template section specified:" + line);
			continue;
		    }
		    templates.put(templateName.trim(), templateSB.toString());
		    templateSB = null;
		    templateName = null;		    
		    continue;
		}

		if(templateSB!=null) {
		    templateSB.append(line);
		    templateSB.append("\n");
		    continue;
		}

                if (tline.startsWith("+apply")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    applyTemplateName = toks.size()==2?toks.get(1): "template";
		    applyTemplateVars = new Hashtable<String,String>();
		    continue;
		}

                if (tline.startsWith(":var")) {
		    if(applyTemplateVars==null) {
			wikiError(buff, "Error: No +apply section specified:" + line);
			continue;
		    }
                    List<String> toks = Utils.splitUpTo(tline, " ", 3);
		    String name = toks.size()>1?toks.get(1):"name";
		    String value= toks.size()>2?toks.get(2):"value";
		    applyTemplateVars.put(name,value);
		    continue;
		    
		}

                if (tline.startsWith("+var")) {
		    if(applyTemplateVars==null) {
			wikiError(buff, "Error: No +apply section specified:" + line);
			continue;
		    }
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    applyVarName = toks.size()==2?toks.get(1): "template";
		    applySB = new StringBuilder();
		    continue;
		}

                if (tline.startsWith("-var")) {
		    if(applySB!=null && applyVarName!=null) {
			applyTemplateVars.put(applyVarName,applySB.toString());
		    } else {
			wikiError(buff, "Error: No +var section specified:" + line);
		    }
		    applySB = null;
		    continue;
		}

		if(tline.startsWith(":apply")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 3);
		    if(toks.size()<2) {
                        wikiError(buff, "Error: no template specified");
			continue;
		    }
		    applyTemplateName = toks.get(1);
		    applyTemplateVars =  HU.parseHtmlProperties(toks.size()>2?toks.get(2):"");
		    tline = "-apply";
		}

		if(tline.startsWith("-apply")) {
		    if(applyTemplateName==null) {
                        wikiError(buff, "Error: no template specified");
			continue;
		    }
		    String template = templates.get(applyTemplateName.trim());
		    if(template==null) {
                        wikiError(buff, "Error: no template found:"+ applyTemplateName);
			continue;
		    }
		    if(applyTemplateVars==null) {
                        wikiError(buff, "Error: no template apply section found:"+ applyTemplateName);
			continue;
		    }
		    //		    System.err.println("T:" + template);
		    for (java.util.Enumeration keys = applyTemplateVars.keys();  keys.hasMoreElements(); ) {
			String key   = (String) keys.nextElement();
			String value = (String) applyTemplateVars.get(key);
			//			System.err.println("\t" + "key: " + key +" value:" + value);
			value = wikify(value, handler);
			template = template.replace("${" + key +"}",value);
		    }
		    //		    System.err.println("after T:" + template);
		    template = wikify(template, handler);
		    buff.append(template);
		    applyTemplateVars = null;
		    applySB = null;
		    continue;
		}
		
		if (applySB!=null) {
		    applySB.append(line);
		    applySB.append("\n");
		    continue;
		}



                if (tline.startsWith("+macro")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 3);
                    currentVar      = ((toks.size() > 1)
                                       ? toks.get(1)
                                       : "");
		    currentVar = currentVar.trim();
                    currentVarValue = new StringBuilder();
                    continue;
                }

                if (tline.startsWith("-macro")) {
		    if(currentVarValue!=null) {
			String macro = currentVarValue.toString();
			if(macros == null) {
			    macros = new Hashtable<String,String>();
			    rawMacros = new Hashtable<String,String>();
			}
			rawMacros.put(currentVar,macro);
			//		    macro = wikify(macro, handler);
			macros.put(currentVar, macro);
		    }
		    currentVar      = null;
                    currentVarValue = null;
                    continue;
                }

		if(tline.startsWith(":process ")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 3);
		    if(toks.size()<=1) {
			wikiError(buff,"Bad :process tag");
			continue;
		    }
		    if(rawMacros==null) {
			wikiError(buff,"No macros defined");
			continue;
		    }
		    String proc = toks.get(1);
		    Hashtable props = HU.parseHtmlProperties(toks.size()>2?toks.get(2):"");
		    String macro =  rawMacros.get(proc);
		    if(macro==null) {
			wikiError(buff, "Could not find macro:" + proc);
			continue;
		    }
		    for (java.util.Enumeration keys = props.keys();
			 keys.hasMoreElements(); ) {
			String key   = (String)keys.nextElement();
			String value = (String)props.get(key);
			macro = macro.replace("${" + key+"}",value);
		    }
		    macro = wikify(macro, handler);
		    buff.append(macro);
		    continue;
		}


                if (currentVar != null) {
                    currentVarValue.append(line);
                    currentVarValue.append("\n");		    
                    continue;
                }



		if(tline.startsWith("+mermaid")) {
		    inMermaid=true;
		    if(!seen.contains("addedmermaid")) {
			seen.add("addedmermaid");
			buff.append("\n<script type='module'>\nimport mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';\nmermaid.initialize({ startOnLoad: true });\n</script>\n");
		    }
		    buff.append("<pre class='mermaid preplain'>\n");
		    continue;

		}
		if(tline.startsWith("-mermaid")) {
		    inMermaid=false;
		    buff.append("\n</pre>\n");
		    continue;
		}
		if(inMermaid) {
		    buff.append(line);
		    buff.append("\n");
		    continue;
		}



                if (tline.startsWith("+snippet")) {
		    continue;
		}
                if (tline.equals("-snippet")) {
		    continue;
		}		


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

                if (tline.startsWith("+splash")) {
		    splashBuffer = new StringBuilder();
		    continue;
		}

                if (tline.startsWith("-splash")) {
		    if(splashBuffer==null) continue;
		    String s = wikify(splashBuffer.toString(), handler);
		    String id = HU.getUniqueId("splash_");
		    buff.append("\n");
		    HU.div(buff, s,HU.attrs("id", id, ATTR_STYLE,"display:none;"));
		    buff.append("\n");
                    HU.script(buff,
                              "HtmlUtils.makeSplash('',{src:'" + id +"'})");
		    buff.append("\n");
		    splashBuffer = null;
		    continue;
		}

		if(splashBuffer!=null) {
		    splashBuffer.append(line);
		    splashBuffer.append("\n");		    
		    continue;
		}





                if (tline.startsWith("+repeat")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
		    String args = toks.size()>1?toks.get(1):"";
		    repeatList = new ArrayList<NamedList<String>>();
		    List<String> argList  = Utils.parseCommandLine(args);
		    for(int i=0;i<argList.size();i+=2) {
			if(i+1>=argList.size())  throw new IllegalArgumentException("Bad repeat:" +tline);
			NamedList<String> l = new NamedList<String>(argList.get(i),Utils.split(argList.get(i+1),","));
			repeatList.add(l);
			
		    }
		    if(repeatList.size()==0)  throw new IllegalArgumentException("Bad repeat:" +tline);
		    repeatBuffer = new StringBuilder();
		    continue;
		}

                if (tline.startsWith("-repeat")) {
		    if(repeatBuffer!=null) {
			int max = 0;
			for(NamedList<String> l: repeatList)
			    max = Math.max(l.getList().size(), max);
			
			for(int i=0;i<max;i++) {
			    String s = repeatBuffer.toString();
			    for(NamedList<String> l: repeatList) {
				String key = l.getName();
				String value = i<l.getList().size()?(String)l.getList().get(i):l.getList().get(l.getList().size()-1);
				s = s.replace("${" + key +"}",value);
			    }
			    buff.append(s);
			}
		    }
		    repeatBuffer = null;
		    repeatList= null;		    
		    continue;
		}

		if(repeatBuffer!=null) {
		    repeatBuffer.append(line);
		    repeatBuffer.append("\n");		    
		    continue;
		}



                if (tline.startsWith("@(")) {
                    handleEmbed(buff, tline);
                    continue;
                }

                if (tline.startsWith(":property")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 3);
                    if (toks.size() > 2) {
                        putWikiProperty(toks.get(1), toks.get(2));
                    } else {
                        putWikiProperty(toks.get(1), null);
                    }

                    continue;
                }


		if (tline.startsWith(":menuitem")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
		    HU.open(buff, "li");
		    HU.open(buff,TAG_DIV);
		    buff.append(toks.size()>1?toks.get(1):"No Label");
		    HU.close(buff,TAG_DIV,"li","\n");
		    continue;
		}
		    
		if (tline.startsWith(":menuheader")) {
		    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
		    HU.open(buff, "li",HU.attrs(ATTR_CLASS,"ui-widget-header"));
		    HU.open(buff,TAG_DIV);
		    buff.append(toks.size()>1?toks.get(1):"No Label");
		    HU.close(buff,TAG_DIV,"li","\n");
		    continue;
		}


		if (tline.startsWith("+popup")) {
		    Hashtable props = getProps.apply(tline);
		    String icon = (String) props.get("icon");
		    String link =  Utils.getProperty(props,"link","");
		    String style =  Utils.getProperty(props,ATTR_STYLE,"");
		    String linkStyle =  Utils.getProperty(props,"linkStyle","");
		    String linkTitle =  Utils.getProperty(props,"linkTitle","");		    		    		    
		    if(icon!=null) link = HU.image(handler.getWikiImageUrl(this, icon, props))+HU.SPACE + link;
		    else if(link.length()==0) link = "Link";
		    NamedValue[]args = new NamedValue[]{
			arg(ATTR_TITLE,Utils.getProperty(props,ATTR_TITLE,null)),
 			arg("header",Utils.getProperty(props,"header","false")),
			arg("decorate",Utils.getProperty(props,"decorate","true")),
			arg("animate",Utils.getProperty(props,"animate","false")),
			arg("my",Utils.getProperty(props,"my",null)),
			arg("at",Utils.getProperty(props,"at",null)),
			arg("draggable",Utils.getProperty(props,"draggable","false")),
			arg("sticky",Utils.getProperty(props,"sticky","false")),
			arg("slideLeft",Utils.getProperty(props,"slideLeft","false")),
			arg("animateSpeed",Utils.getProperty(props,"animateSpeed","300")),
					
			arg("toggleid",HU.getUniqueId("popup"))
		    };

		    link = HU.span(link,HU.attr(ATTR_TITLE,linkTitle) +HU.style(linkStyle));
		    String []tuple=HtmlUtils.makePopupLink(link, args);
		    String compId = tuple[0];
		    buff.append(tuple[1]);
		    HU.open(buff,TAG_DIV,
			    HU.id(compId)
			    + HU.style(style)
			    + HU.attr(ATTR_STYLE, "display:none;")
			    + HU.cssClass(HU.CSS_CLASS_POPUP_CONTENTS));
		    continue;
		}


		if (tline.startsWith("-popup")) {
		    buff.append("</div>");
		    continue;
		}		

		if (tline.startsWith("+menuitem")) {
		    Hashtable props = getProps.apply(tline);
		    String attrs = HU.attrs(ATTR_STYLE,Utils.getProperty(props,ATTR_STYLE,""));
		    HU.open(buff, "li",attrs);
		    HU.open(buff,TAG_DIV);
		    continue;
		}
		if (tline.startsWith("-menuitem")) {
		    HU.close(buff,TAG_DIV,"li","\n");
		    continue;
		}
                if (tline.startsWith("+menu")) {
		    buff.append("\n");
		    HU.open(buff,TAG_DIV,ATTR_CLASS,"ramadda-menutree");
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
		    String attrs = "";
		    menuCnt++;
		    if(menuCnt==1) {
			String menuId = HU.getUniqueId("menu_");
			attrs += HU.attrs("id",menuId);
			menuIds.add(menuId);
		    } else {
			HU.open(buff,"li");
			if(toks.size()>1) {
			    buff.append(HU.div(toks.get(1)));
			}
		    }
		    HU.open(buff,"ul",attrs);
		    buff.append("\n");
		    continue;
		}

                if (tline.startsWith("-menu")) {
		    menuCnt--;
		    HU.close(buff,"ul","\n");
		    if(menuCnt>0) {
			HU.close(buff,"li","\n");
		    }
		    HU.close(buff,TAG_DIV);
		    continue;

		}



                if (tline.startsWith(":macro")) {
                    hasSet = true;
                    List<String> toks  = Utils.splitUpTo(tline, " ", 3);
                    String       var   = ((toks.size() > 1)
                                          ? toks.get(1)
                                          : "");
                    String       value = ((toks.size() > 2)
                                          ? toks.get(2)
                                          : "");
		    if(macros == null) {
			rawMacros = new Hashtable<String,String>();
			macros = new Hashtable<String,String>();
		    }
		    value = value.replace("\\n","\n");
                    rawMacros.put(var.trim(), value.trim());
		    //		    value =  wikify(value,handler);
                    macros.put(var.trim(), value.trim());
                    continue;
                }



                if (tline.equals("+leftright")) {
		    Hashtable props = getProps.apply(tline);
                    buff.append("<table width=100%><tr valign=" + Utils.getProperty(props,"valign","top") +">");
                    leftWidth = Utils.getProperty(props,"leftWidth","33%");
                    rightWidth = Utils.getProperty(props,"rightWidth","33%");
                    leftStyle = Utils.getProperty(props,"leftStyle","");
                    rightStyle = Utils.getProperty(props,"rightStyle","");		    
                    continue;
                }
                if (tline.startsWith("+leftmiddleright")) {
		    Hashtable props = getProps.apply(tline);
                    buff.append("<table width=100%><tr valign=" + Utils.getProperty(props,"valign","top") +">");
                    leftWidth = Utils.getProperty(props,"leftWidth","33%");
                    middleWidth = Utils.getProperty(props,"middleWidth","33%");
                    rightWidth = Utils.getProperty(props,"rightWidth","33%");
                    leftStyle = Utils.getProperty(props,"leftStyle","");
                    middleStyle = Utils.getProperty(props,"middleStyle","");
                    rightStyle = Utils.getProperty(props,"rightStyle","");		    
                    continue;
                }

                if (tline.equals("-leftright")) {
                    buff.append("</tr></table>");
                    continue;
                }
                if (tline.equals("-leftmiddleright")) {
                    buff.append("</tr></table>");
                    continue;
                }
                if (tline.equals("+left")) {
                    buff.append("<td width=" + leftWidth+">");
		    HU.open(buff,TAG_DIV,HU.attr(ATTR_STYLE, leftStyle));
                    continue;
                }
                if (tline.equals("-left")) {
                    buff.append("</div></td>");
                    continue;
                }
                if (tline.equals("+middle")) {
                    buff.append("<td align=center width=" + middleWidth + ">");
		    HU.open(buff,TAG_DIV,HU.attr(ATTR_STYLE, middleStyle));
                    continue;
                }
                if (tline.equals("-middle")) {
                    buff.append("</div></td>");
                    continue;
                }
                if (tline.equals("+right")) {
                    buff.append("<td align=right width=" + rightWidth+">");
		    HU.open(buff,TAG_DIV,HU.attr(ATTR_STYLE, rightStyle));
                    continue;
                }
                if (tline.equals("-right")) {
                    buff.append("</div></td>");
                    continue;
                }




                if (tline.startsWith("+table")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    String       width     = "100%";
                    String       height    = null;
                    String       ordering  = null;
                    String       paging    = null;
                    String       xclazz    = null;
                    String       searching = "false";
                    String       clazz     = "ramadda-table";
                    if (toks.size() == 2) {
                        Hashtable props = HU.parseHtmlProperties(toks.get(1));
                        width  = Utils.getProperty(props, ATTR_WIDTH, width);
                        height = Utils.getProperty(props, ATTR_HEIGHT, height);
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
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
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
		    Hashtable props = getProps.apply(tline);
		    String width = Utils.getProperty(props, ATTR_WIDTH, null);
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
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
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

                if (tline.startsWith("+slides")) {
                    String       divClass = "";
		    slidesProps =  getProps.apply(tline);
		    slideTitles = new ArrayList<String>();
		    slidesId = HU.getUniqueId("slides_");
		    buff.append(HU.script("HtmlUtils.loadSlides();"));
		    slidesProps.remove("bigArrow");
		    boolean bigArrow  = Utils.getProperty(slidesProps,"bigArrow",true);
		    String style = Utils.getProperty(slidesProps,"slidesStyle","");
		    String contStyle = "";
		    String width = Utils.getProperty(slidesProps,"width",null);
		    if(width!=null) {
			style+=HU.css("width",width);
			contStyle+=HU.css("grid-template-columns","minmax(200px, 300px) " + width);
		    }
		    if(Utils.getProperty(slidesProps,"center",false)) {
			style+=HU.css("margin","0 auto");
		    }
		    String headerStyle="";
		    if(!Utils.getProperty(slidesProps,"showHeader",false)) {
			headerStyle+=HU.css("display","none");
		    }
		    boolean headerLeft = Utils.getProperty(slidesProps,"headerLeft",false);
		    if(headerLeft) {
			//			style+=HU.css("display","inline-block");
		    }

		    HU.open(buff,"div",
			    
			    HU.attrs(ATTR_STYLE, contStyle,
				     ATTR_CLASS,"ramadda-slides-container " + (headerLeft?"ramadda-slides-container-left":"")));

		    HU.div(buff,"",HU.attrs(ATTR_ID,slidesId+"_header",
					    ATTR_CLASS,"ramadda-slides-child "+(headerLeft?"ramadda-slides-header-left":"ramadda-slides-header"),
					    ATTR_STYLE,headerStyle));
		    HU.open(buff,TAG_DIV,HU.attrs(ATTR_STYLE,style,ATTR_ID,slidesId,
						  ATTR_CLASS,"ramadda-slides-child ramadda-slides " +(bigArrow?"ramadda-slides-bigarrow":"")));

		    continue;
		}

                if (tline.startsWith("+slide")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    String title = toks.size()>1?toks.get(1):null;
		    if(slideTitles !=null) {
			slideTitles.add(title);
		    }
		    String style= "margin:10px;padding:10px;" + (slidesProps!=null?Utils.getProperty(slidesProps,ATTR_STYLE,""):"");
		    HU.open(buff,TAG_DIV,HU.attrs(ATTR_STYLE,style));
		    continue;
		}
		if (tline.equals("-slide")) {
		    buff.append("</div>\n");
		    continue;
		}

                if (tline.equals("-slides")) {
		    HU.close(buff,TAG_DIV,TAG_DIV);
		    if(slidesId==null) {
			buff.append("No open slides tag");
			continue;
		    }
		    slidesProps.remove(ATTR_STYLE);
		    List<String> args = Utils.makeStringList(Utils.makeListFromDictionary(slidesProps));
		    String slidesArgs = JsonUtil.mapAndGuessType(args);
		    boolean anyTitles = false;
		    for(String title: slideTitles)
			if(title!=null) anyTitles = true;
		    if(anyTitles) {
			StringBuilder header = new StringBuilder();
			int cnt = 0;
			for(int i=0;i<slideTitles.size();i++) {
			    String title  =  slideTitles.get(i);
			    if(title!=null) {
				String clazz = " ramadda-slides-header-item ";
				if(cnt++==0) 
				    clazz += " ramadda-slides-header-item-selected ";
				HU.div(header,title,HU.attrs(ATTR_TITLE, title,ATTR_CLASS,clazz,"slideindex",i+""));
			    }
			}
			HU.div(buff,header.toString(),HU.attrs("id",slidesId+"_headercontents",ATTR_STYLE,""));
		    }
		    buff.append(HU.script(JQuery.ready("HtmlUtils.makeSlides('" + slidesId+"'," + slidesArgs+");")));
		    continue;
		}




                if (tline.startsWith("+tabs")) {
                    TabState     tabInfo  = new TabState();
                    String       divClass = "";
		    Hashtable props = getProps.apply(tline);
		    if (Utils.getProperty(props,"center",false)) {
			divClass += " ramadda-tabs-center ";
		    }
		    if(Utils.getProperty(props,"cullEmpty",false)) {
			tabInfo.cullEmpty=true;
		    }
		    if (Utils.getProperty(props,"min",false)) {
			divClass += " ramadda-tabs-min ";
		    } else if (Utils.getProperty(props,"minarrow",false) ||
			       Utils.getProperty(props,"minArrow",false)) {
			divClass +=
			    " ramadda-tabs-min ramadda-tabs-minarrow ";
		    }

		    if (Utils.getProperty(props,"transparent",false)) {
			divClass += " ramadda-tabs-transparent ";
		    }
		    String clazz= "";
		    if(Utils.getProperty(props,"tight",false)) {
			clazz += " ramadda-tabs-tight ";
		    }
		    
		    if(Utils.getProperty(props,"noBorder",false)) {
			clazz+= " ramadda-tabs-noborder ";
		    }

		    tabInfo.minHeight = (String) props.get("minHeight");
		    if (tabInfo.minHeight != null) {
			tabInfo.minHeight = getSize(tabInfo.minHeight);
		    }
                    tabStates.add(tabInfo);
                    allTabStates.add(tabInfo);
                    buff.append("\n");
                    HU.open(buff, HU.TAG_DIV, ATTR_CLASS, divClass);
                    HU.open(buff, HU.TAG_DIV, "id", tabInfo.id, ATTR_CLASS,
                            "ui-tabs " + clazz);
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
                    List<String> toks    = Utils.splitUpTo(tline, " ",
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
                    buff.append(
				HU.open(
					TAG_DIV,
					style + HU.id(tabInfo.id + "-" + (tabInfo.cnt))
					+ HU.cssClass("ui-tabs-hide")));
                    buff.append("\n");
                    continue;
                }
                if (tabStates.size() > 0) {
                    if (tline.equals("-tab")) {
                        TabState tabInfo = tabStates.get(tabStates.size()
							 - 1); 
                       buff.append(HU.close(TAG_DIV));
                        buff.append("\n");
                        continue;
                    }
                    if (tline.equals("-tabs")) {
                        TabState tabInfo = tabStates.get(tabStates.size()
							 - 1);
			/*
                        js.append(
				  "jQuery(function(){\njQuery('#" + tabInfo.id
				  + "').tabs({activate: HtmlUtil.tabLoaded})});\n");
			*/
                        js.append(
				  "jQuery(function(){\nHtmlUtils.initTabs(" + HU.squote(tabInfo.id)+"," + tabInfo.cullEmpty+")});");
                        tabInfo.title.append("\n");
                        tabInfo.title.append("</ul>");
                        tabInfo.title.append("\n");
                        tabStates.remove(tabStates.size() - 1);
                        buff.append(HU.close(TAG_DIV));
                        buff.append(HU.close(TAG_DIV));
                        continue;
                    }


                }

                if (tline.startsWith("+sidetoggle")) {
		    Hashtable props = getProps.apply(tline);
		    String guid = HU.getUniqueId("toggle");
		    props.put("id",guid);
		    context.put("sidetoggleprops",props);
		    String label = Utils.getProperty(props,"label","Click");
		    HU.div(buff,label,HU.attrs("class","ramadda-clickable ramadda-side-toggle-button",
					       "id",guid+"-button"));
		    buff.append(HU.open("div",HU.attrs("class","ramadda-side-toggle",
						       "id",guid)));
		    continue;
		}

                if (tline.startsWith("-sidetoggle")) {
		    Hashtable props=  (Hashtable)context.get("sidetoggleprops");
		    if(props==null) {
                        wikiError(buff, "Error: unopened sidetoggle");
			continue;
		    }
		    context.remove("sidetoggleprops");
		    String id = Utils.getProperty(props,"id","");
		    buff.append("</div>");
		    List<String> attrs =new ArrayList<String>();
		    for (java.util.Enumeration keys = props.keys();
			 keys.hasMoreElements(); ) {
			String key   = (String)keys.nextElement();
			if(key.equals("id")) continue;
			attrs.add(key);
			attrs.add(JsonUtil.quote((String)props.get(key)));
		    }
		    String opts = JsonUtil.map(attrs);
                    HU.script(buff, HU.call("HtmlUtils.initSideToggle",
					    HU.squote(id+"-button"),
					    HU.squote(id),opts));
		    continue;
		}

                if (tline.startsWith("+accordian")
		    || tline.startsWith("+accordion")||
		    tline.startsWith("+toggle")) {
                    AccordionState accordionState = new AccordionState();
                    accordionStates.add(accordionState);
                    String       divClass = "";
		    boolean isToggle = tline.startsWith("+toggle");
		    Hashtable props = isToggle?new Hashtable():getProps.apply(tline);
		    if(isToggle) Utils.put(props,"activeSegment",tline.startsWith("+toggleopen")?
					   "0":"-1","decorate","false");
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

                    buff.append("\n");
                    buff.append(
				HU.open(
					HU.TAG_DIV,
					HU.cssClass(
						    " ui-accordion ui-widget ui-helper-reset") + HU
					.id(accordionState.id)));
                    buff.append("\n");
		    if(isToggle) {
			segmentOpen.accept(tline);
		    }
                    continue;
                }

                if (tline.startsWith("-accordian")
		    || tline.startsWith("-accordion")
		    || tline.startsWith("-toggle")) {
                    if (accordionStates.size() == 0) {
                        buff.append("No open accordion tag");
                        continue;
                    }
		    if(tline.startsWith("-toggle")) {
			segmentClose.accept(tline);
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
		    segmentOpen.accept(tline);
		    continue;
		}

                if (tline.startsWith("-segment")) {
		    segmentClose.accept(tline);
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
                    List<String>  toks  = Utils.splitUpTo(tline, " ", 2);
                    StringBuilder extra = new StringBuilder();
		    Hashtable props = getProps.apply(tline);
		    StringBuilder styles = new StringBuilder();
		    String space = Utils.getProperty(props,"space",null);
		    for (String side : new String[] { "top", "left",
						      "bottom", "right" }) {
			String v = Utils.getProperty(props, side,space);
			if (v != null) {
			    v = getSize(v);
			    styles.append("margin-" + side + ":" + v
					  + ";");
                        }
		    }
		    if (styles.length() > 0) {
			extra.append(HU.style(styles.toString()));
                    }
                    buff.append(HU.open(TAG_DIV, HU.cssClass("inset") + extra));

                    continue;
                }
                if (tline.equals("-inset")) {
                    buff.append("</div>");

                    continue;
                }

                if (tline.startsWith("+quote")) {
                    buff.append("<div class=boxquote>\n");
		    continue;
		}
                if (tline.startsWith("-quote")) {
                    buff.append("</div>\n");
		    continue;
		}		


		if (tline.equals("+ralign")) {
                    buff.append("<div style='text-align:right;'>\n");
		    continue;
                }

                if (tline.equals("-ralign")) {
	            buff.append("</div>\n");
	            continue;
                }


                if (tline.startsWith(":setlang")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    HU.script(buff, HU.call("Translate.setDefaultLanguage",
					    toks.size()>1?HU.squote(toks.get(1)):"null"));
		    continue;
		}

                if (tline.startsWith(":langswitcher")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    String guid = HU.getUniqueId("langs");
                    HU.span(buff,"",HU.id(guid));
                    HU.script(buff, HU.call("Translate.addSwitcher",HU.squote(guid),
					    toks.size()>1?HU.squote(toks.get(1)):"null"));
		    continue;
		}

                if (tline.startsWith("+lang")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    String lang = toks.size()>1?toks.get(1):"en";
		    HU.open(buff,"div",HU.style(HU.css("display","none"))+HU.clazz("ramadda-language-block") +HU.attr("data-lang",lang));
		    continue;
		}

                if (tline.startsWith("-lang")) {
	            buff.append("</div>\n");
		    continue;
		}		

		if(tline.startsWith("+dict")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    buff.append(HU.open("div",HU.attrs("class","ramadda-dict",
						       "lang" ,toks.size()>1?toks.get(1):"")));
		    continue;
		}

		if(tline.startsWith("-dict")) {
	            buff.append("</div>\n");
		    continue;
		}

                if (tline.startsWith("+div") || tline.startsWith("+span") || tline.startsWith("+inlineblock")) {
                    String       style = "";
                    String       clazz = "";
		    Hashtable props = getProps.apply(tline);
		    String    tmp   = (String) props.get(ATTR_CLASS);
		    if (tmp != null) {
			clazz = tmp;
		    }
                    String       id = Utils.getProperty(props,"id",(String)null);
		    style = (String) props.get(ATTR_STYLE);
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
		    buff.append(HU.open(tline.startsWith("+div")?HU.TAG_DIV:
					tline.startsWith("+span")?HU.TAG_SPAN:"inlineblock",
					(id!=null?HU.id(id):"") +
					HU.cssClass(clazz)
					+ HU.style(style)));

		    continue;
		}

                if (tline.startsWith("-div")) {
                    buff.append(HU.close(HU.TAG_DIV));
                    continue;
                }
                if (tline.startsWith("-span")) {
                    buff.append(HU.close(HU.TAG_SPAN));
                    continue;
                }		

                if (tline.startsWith("-inlineblock")) {
                    buff.append(HU.close("inineblock"));
                    continue;
                }		
		
                if (tline.equals(":filler")) {
		    buff.append("<div style='flex:1;'></div>");
		    continue;
		}

                if (tline.startsWith("+gridbox")) {
		    Hashtable props = getProps.apply(tline);
		    String style = "";
		    String flex =  Utils.getProperty(props,"flex",(String)null);
		    if(flex!=null)
			style+= "flex:" + flex+";";
		    String width =  Utils.getProperty(props,ATTR_WIDTH,(String)null);
		    if(Utils.stringDefined(width))
			style+= "width:" + width+";";		    

		    String _style =  Utils.getProperty(props,"s",(String)null);
		    if(_style!=null)
			style+= _style;
		    
                    buff.append(HU.open(HU.TAG_DIV,
					HU.style(style)+
                                        HU.cssClass("ramadda-gridbox")));
		    String title = Utils.getProperty(props,ATTR_TITLE,(String)null);
                    if (title!=null) {
                        buff.append(
				    HU.tag(HU.TAG_DIV,
					   HU.cssClass("ramadda-gridbox-header"),
					   title));
                    }
                    buff.append(
				HU.open(
					HU.TAG_DIV,
					HU.cssClass("ramadda-gridbox-contents")));

                    continue;
                }


                if (tline.startsWith("-gridbox")) {
                    buff.append(HU.close(HU.TAG_DIV));
                    buff.append(HU.close(HU.TAG_DIV));
                    continue;
                }


		if (tline.startsWith("+grid")) {
                    tline = tline.substring(1);
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    tline = toks.get(0).trim();
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
                    String clazz = "ramadda-" + tline +" ";
		    String style="";
		    String columns = Utils.getProperty(props,"columns",(String)null);
		    if(columns!=null)
			style+="display:grid;grid-template-columns:" + columns +";";
		    if(Utils.getProperty(props,"decorated",false))
			clazz+=" ramadda-grid-decorated ";
                    buff.append(HU.open(HU.TAG_DIV,
					HU.style(style)+
                                        HU.cssClass("ramadda-grid "
						    + clazz)));

                    continue;
                }


                if (tline.startsWith("-grid")) {
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
                              + afterFade + ",1.0);}," + afterPause + ");");

                    continue;
                }


                if (tline.startsWith("+draggable")) {
		    Hashtable props = getProps.apply(tline);
                    dragId = HU.getUniqueId("draggable");

                    dragToggle = Utils.getProperty(props, "toggle", false);
                    dragToggleVisible = Utils.getProperty(props,
							  "toggleVisible", true);
                    String style  = (String) props.get(ATTR_STYLE);
                    String header = (String) props.get("header");
                    String clazz  = "ramadda-draggable";
                    if (Misc.equals("true", props.get("framed"))) {
                        clazz = "ramadda-draggable-frame";
                    }
                    if (style == null) {
                        style = "";
                    }
                    HU.open(buff, TAG_DIV, ATTR_STYLE,"position:relative;");
                    HU.open(buff, TAG_DIV, "id", dragId, ATTR_STYLE,
                            "display:inline-block;z-index:500;" + style);
                    if (header != null) {
                        if (dragToggle) {
                            header = HU.image("", "id", dragId + "_img")
				+ " " + header;
                        }
                        HU.div(buff, header,
                               HU.attrs(ATTR_CLASS, "ramadda-draggable-header"));
                    }
                    HU.open(buff, TAG_DIV, ATTR_CLASS, clazz, "id",
                            dragId + "_frame");
                    if (dragToggle) {
                        HU.script(buff,
                                  "HU.makeToggle('" + dragId + "_img','"
                                  + dragId + "_frame'," + dragToggleVisible
                                  + ");");
                    }

                    continue;
                }

                if (tline.startsWith("-draggable")) {
                    if (dragId != null) {
                        HU.close(buff, TAG_DIV);
                        HU.close(buff, TAG_DIV);
                        HU.close(buff, TAG_DIV);
                        //              HU.script(buff, "$('#" + dragId +"').draggable();\n");
                        HU.script(buff,
                                  "HU.makeDraggable('#" + dragId + "');\n");
                    }

                    continue;
                }



                if (tline.startsWith("+expandable")) {
		    Hashtable props = getProps.apply(tline);
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
                    HU.open(buff, TAG_DIV, "id", dragId, ATTR_STYLE,
                            "position:relative;", ATTR_CLASS, clazz2);
                    if (header != null) {
                        HU.div(buff, header,
                               HU.attrs(ATTR_CLASS,
                                        "ramadda-expandable-header"));
                    }
                    HU.open(buff, TAG_DIV, ATTR_CLASS, clazz);

                    continue;
                }

                if (tline.startsWith("-expandable")) {
                    if (dragId != null) {
                        HU.close(buff, TAG_DIV);
                        HU.close(buff, TAG_DIV);
                        //              HU.script(buff, "$('#" + dragId +"').expandable();\n");
                        HU.script(buff,
                                  "HU.makeExpandable('#" + dragId + "');\n");
                    }

                    continue;
                }

                if (tline.startsWith("+enlarge")) {
		    Hashtable props = enlargeProps = getProps.apply(tline);
		    String enlargeId = HU.getUniqueId("enlarge");
		    enlargeProps.put("id",enlargeId);
		    String height=Utils.getProperty(props,ATTR_HEIGHT,"200px");
		    String style=Utils.getProperty(props,ATTR_STYLE,"");
		    String enlargeMsg=Utils.getProperty(props,"enlargeMsg",null);
		    String shrinkMsg=Utils.getProperty(props,"shrinkMsg",null);		    
		    buff.append(HU.open(TAG_DIV,ATTR_CLASS,"ramadda-enlarge-outer","id",enlargeId));
		    buff.append(HU.open(TAG_DIV,ATTR_STYLE,style,ATTR_CLASS,"ramadda-enlarge-inner","id",enlargeId+"_inner"));
		    buff.append(HU.open(TAG_DIV,ATTR_CLASS,"ramadda-enlarge-contents","id",enlargeId+"_contents",ATTR_STYLE,HU.css(ATTR_HEIGHT,HU.makeDim(height,"px"))));		    
		    continue;
		}

                if (tline.startsWith("-enlarge")) {
		    if(enlargeProps!=null) {
			buff.append(HU.close(TAG_DIV));
			buff.append(HU.close(TAG_DIV));
			buff.append(HU.close(TAG_DIV));
			String m1 = (String) enlargeProps.get("enlargeLabel");
			String m2 = (String) enlargeProps.get("shrinkLabel");			
			HU.script(buff,HU.call("HU.makeEnlargable",
					       HU.squote(enlargeProps.get("id")),
					       m1==null?"null":HU.squote(m1),
					       m2==null?"null":HU.squote(m2)));					       
					       
		    }
		    continue;
		}		


                if (tline.startsWith("+fullscreen")) {
		    Hashtable props = getProps.apply(tline);
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
                    HU.open(buff, TAG_DIV, "id", dragId, ATTR_STYLE,
                            "position:relative;", ATTR_CLASS, clazz2);
                    if (header != null) {
                        HU.div(buff, header,
                               HU.attrs(ATTR_CLASS,
                                        "ramadda-expandable-header"));
                    }
                    HU.open(buff, TAG_DIV, ATTR_CLASS, clazz);

                    continue;
                }

                if (tline.startsWith("-fullscreen")) {
                    if (dragId != null) {
                        HU.close(buff, TAG_DIV);
                        HU.close(buff, TAG_DIV);
                        //              HU.script(buff, "$('#" + dragId +"').expandable();\n");
                        HU.script(buff,
                                  "HU.makeExpandable('#" + dragId + "',true);\n");
                    }

                    continue;
                }

                if (tline.startsWith("+bigtext")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
		    
		    buff.append("<div class=ramadda-bigtext ");
		    String labelMore =(String)props.get("labelMore");
		    if(labelMore!=null)
			buff.append(HU.attr("bigtext-label-more",labelMore));
		    String labelLess =(String)props.get("labelLess");
		    if(labelLess!=null)
			buff.append(HU.attr("bigtext-label-less",labelLess));
		    String height =(String)props.get("height");
		    if(height!=null)
			buff.append(HU.attr("bigtext-height",height));
		    String length =(String)props.get("length");
		    if(length!=null)
			buff.append(HU.attr("bigtext-length",length));
		    buff.append("/>");
		    continue;
		}

                if (tline.startsWith("-bigtext")) {
		    buff.append("</div>");
		    continue;
		}		    		


                if (tline.startsWith("+section")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");

                    String       tag       = toks.get(0).substring(1);
                    List<String> toks2     = Utils.splitUpTo(tag, "-",
							     2);
                    String       remainder = ((toks2.size() > 1)
					      ? toks2.get(1)
					      : "");

                    String       baseClass = "ramadda-section";
                    if (remainder.length() > 0) {
                        baseClass = baseClass+" " + baseClass + "-" + remainder;
                    }

                    String  label       = (String) props.get("label");
                    String  heading     = (String) props.get("heading");
                    String  title       = (String) props.get(ATTR_TITLE);
                    String  titleStyle  = (String) props.get("titleStyle");
                    String  subTitle    = (String) props.get("subTitle");
		    if(Misc.equals(getWikiProperty("showTitle"),"false")) {
			title = subTitle = null;
		    }
						   


                    String  classArg    = (String) props.get(ATTR_CLASS);
                    String  style       = (String) props.get(ATTR_STYLE);
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
                            scnt  = Integer.valueOf(-1);
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
                        putProperty("section-cnt",  Integer.valueOf(newCnt));
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


                    if (label == null) {
                        label = heading;
                    }
                    if (label != null) {
                        buff.append(
				    HU.open(
					    HU.TAG_DIV,
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
			if(handler.titleOk(this)) {
			    title  = title.replace("_quote_","\"");
			    HU.div(buff,getTitle(title, titleStyle!=null?titleStyle:"") + sub,
				   HU.cssClass("ramadda-page-title")
				   + ((headerStyle == null)
				      ? ""
				      : HU.style(headerStyle)));
			}
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

                if (tline.startsWith("+callout")|| tline.startsWith("+credit") ) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    boolean credit = tline.startsWith("+credit");
                    String what =
                        toks.get(0).trim().substring((credit?"+credit":"+callout").length());
		    if(credit) what = "-info";
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
                    HU.open(buff, TAG_DIV,
                            HU.attrs(ATTR_CLASS,
                                     "ramadda-callout ramadda-callout"
                                     + what));
                    String iconSize = Utils.getProperty(props, "iconSize",
							"24px");
                    String icon = (String) props.get("icon");
                    if (icon == null) {
                        icon = what.equals("-tip")?
			    "fa-circle-check":
			    what.equals("-question")
			    ? "fa-question-circle"
			    : what.equals("-info")
			    ? "fa-info-circle"
			    : what.equals("-warning")
			    ? "fa-exclamation-triangle"
			    : null;
                    }
                    if (icon != null) {
                        icon = HU.faIcon(icon, ATTR_STYLE, (iconSize!=null?"font-size:" + iconSize:""));
			icon = HU.div(icon,
				      HU.attrs(ATTR_CLASS, "ramadda-callout-icon"));
                    } else {
                        icon = "";
                    }
                    buff.append(
				"<table width=100%><tr valign=top><td width=1%>");
		    buff.append(icon);
                    buff.append("</td><td>");
                    HU.open(buff, TAG_DIV,
                            HU.attrs(ATTR_CLASS, "ramadda-callout-inner"));
                    continue;
                }

                if (tline.startsWith("-callout") || tline.startsWith("-credit")) {
                    HU.close(buff, TAG_DIV);
                    buff.append("</td></tr></table>");
                    HU.close(buff, TAG_DIV);
                    continue;
                }

                if (tline.startsWith(":wikip")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 3);
                    String       page  = toks.get(1).trim();
                    String       label = (toks.size() > 2)
			? toks.get(2).trim()
			: Utils.makeLabel(page);
                    HU.href(buff, "https://en.wikipedia.org/wiki/" + page,
                            label);

                    continue;
                }

                if (tline.startsWith(":reload")) {
                    String       id   = HU.getUniqueId("reload");
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
                    String time = Utils.getProperty(props, "seconds", "60");
                    boolean showCbx = Utils.getProperty(props,
							"showCheckbox", true);
                    boolean showLabel = Utils.getProperty(props, "showLabel",
							  true);
                    if (showCbx) {
                        HU.checkbox(buff, "", "true", true, HU.id(id));
                        buff.append(" ");
                    }
                    HU.span(buff, showLabel
			    ? ""
			    : "Reload", HU.id(id + "_label"));
                    //                if (showLabel) {
                    //                    buff.append(" ");
                    //                    HU.span(buff, "", HU.id(id + "_label"));
                    //                }
                    buff.append(HU.script("Utils.initPageReload(" + time
                                          + ",'" + id + "'," + showLabel
                                          + ");"));

                    continue;
                }

                if (tline.startsWith(":script") || tline.startsWith(":javascript")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    HU.importJS(buff, toks.get(1));

                    continue;
                }

                if (tline.startsWith("+panel")) {
                    buff.append("\n");
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
                    String name  = (String) props.get("name");
                    String style = (String) props.get(ATTR_STYLE);
                    String color = (String) props.get("color");
                    String fromColor = (String) props.get("fromColor");
                    String toColor = (String) props.get("toColor");		    
		    if(fromColor!=null && toColor!=null) {
			if(style==null) style= "";
			style +="background:linear-gradient("+fromColor+","+toColor+");";
		    }
                    String extra = "";
                    String clazz = "panel ";
                    if (name != null) {
                        extra += " data-section-name=\"" + name.replace(" ","-") + "\"  ";
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
		    if(handler.titleOk(this)) {
			List<String> toks = Utils.splitUpTo(tline, " ", 2);
			if (toks.size() > 1) {
			    String label = toks.get(1);
			    if (label.indexOf("{{") >= 0) {
				label = wikify(label, handler);
				label = label.replaceAll(".*<.*?>(.*)</.*>.*",
							 "$1");
			    }
			    defineHeading.accept(buff, label, 0);
			    buff.append(
					HU.div(getTitle(toks.get(1)),
					       HU.cssClass("ramadda-page-title")));
			}
                    }
                    continue;
                }


                if (tline.startsWith("+frame")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
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
                    String title = (String) props.get(ATTR_TITLE);
                    HU.open(buff, TAG_DIV,
                            HU.cssClass(outerClazz) + HU.style(frameStyle));
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
                    HU.open(buff, TAG_DIV,
                            HU.cssClass("ramadda-frame-inner")
                            + HU.style(innerStyle));

                    continue;
                }


                if (tline.startsWith("-frame")) {
                    HU.close(buff, TAG_DIV);
                    HU.close(buff, TAG_DIV);

                    continue;
                }

                if (tline.startsWith("+title")) {
                    StringBuilder extra = new StringBuilder();
                    List<String>  toks  = Utils.splitUpTo(tline, " ", 2);
                    HU.open(buff, TAG_DIV, HU.cssClass("ramadda-page-title"));
                    String url = getTitleUrl(true);
                    if (url != null) {
                        closeTheTag = true;
			String attrs = HU.attr("href", url);
			String target = getLinkTarget();	
			if(target!=null) attrs+=HU.attr("target",target);	
                        HU.open(buff, "a", attrs);
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
                    List<String> toks  = Utils.splitUpTo(tline, " ", 3);
                    String       tag   = toks.get(0).substring(1);
                    String       url   = ((toks.size() > 1)
                                          ? toks.get(1)
                                          : "");
                    String       label = ((toks.size() > 2)
                                          ? toks.get(2)
                                          : url);
                    List<String> toks2 = Utils.splitUpTo(tag, "-", 2);
                    String       clazz = ((toks2.size() > 1)
                                          ? toks2.get(1)
                                          : "");
                    if (clazz.length() > 0) {
                        clazz = "ramadda-button-" + clazz;
                    }
		    buff.append(makeHref(url,label,
					 " class='ramadda-button " + clazz
					 + "' role='button' "));

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
                    String    style = (String) props.get(ATTR_STYLE);
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
                    HU.open(buff, TAG_DIV, HU.style(style));

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


		if(tline.startsWith(":lorem")) {
		    buff.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
		    continue;
		}

                if (tline.startsWith(":vspace")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    String space = toks.size()==1?"10px":HU.makeDim(toks.get(1),"px");
		    buff.append(HU.div("",HU.style(HU.css("margin-top",space))));
		    continue;
		}

                if (tline.startsWith(":hspace")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
		    String space = toks.size()==1?"10px":HU.makeDim(toks.get(1),"px");
		    buff.append(HU.div("",HU.style(HU.css("display","inline-block",ATTR_WIDTH,space))));
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

                if (tline.startsWith(":anchor ")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    buff.append(HU.anchorName((toks.size() > 1)
					      ? toks.get(1)
					      : ""));
                    continue;
		}

                if (tline.startsWith(":b ")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    buff.append(HU.b((toks.size() > 1)
                                     ? toks.get(1)
                                     : ""));

                    continue;
                }

                if (tline.startsWith(":h1")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
                    String       label = (toks.size() > 1)
			? toks.get(1)
			: "";
		    headingLinker.accept(buff,label,"h1",1);
                    continue;
                }

                if (tline.startsWith(":h2")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
                    String       label = (toks.size() > 1)
			? toks.get(1)
			: "";
		    headingLinker.accept(buff,label,"h2",2);
                    continue;
                }



                if (tline.startsWith(":h3")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
                    String       label = (toks.size() > 1)
			? toks.get(1)
			: "";
		    headingLinker.accept(buff,label,"h3",3);
                    continue;
                }

                if (tline.startsWith(":center")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    buff.append(HU.center((toks.size() > 1)
                                          ? toks.get(1)
                                          : ""));

                    continue;
                }


                if (tline.startsWith(":link")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 3);
                    String       label = (toks.size() > 2)
			? toks.get(2)
			: "link";
                    buff.append(makeHref(toks.get(1), label,""));
                    continue;
                }

                if (tline.startsWith(":draft")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
                    String       label = (toks.size() > 1)
			? toks.get(1)
			: "Draft";
                    buff.append(
				"<div class=ramadda-draft-container><div class=ramadda-draft>"
				+ label + "</div></div>\n");

                    continue;
                }

                if (tline.startsWith("+draft")) {
                    buff.append("<div class=ramadda-draft>");
                    continue;
                }

                if (tline.startsWith("-draft")) {
                    buff.append("</div>");
		    continue;
		}


                if (tline.startsWith(":anchor")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
                    String       label = (toks.size() > 1)
			? toks.get(1)
			: "";
                    defineHeading.accept(buff, label, 1);
                    continue;
                }


                if (tline.startsWith(":pagesearch")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
		    List<String> opts = new ArrayList<String>();
		    String sel1 = Utils.getProperty(props,"selector",TAG_DIV);
		    String sel2 = Utils.getProperty(props,"parentSelector",null);
		    String label = Utils.getProperty(props,"label",null);
		    String hideAll = Utils.getProperty(props,"hideAll","false");
		    String linkSelector = Utils.getProperty(props,"linkSelector",null);
		    if(linkSelector!=null) 
			Utils.add(opts,"linkSelector",JsonUtil.quote(linkSelector));

		    Utils.add(opts,"hideAll",JsonUtil.quoteType(hideAll));
		    HtmlUtils.addPageSearch(buff,sel1,sel2,label,opts);
		    continue;
		}

                if (tline.startsWith(":nav")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    String       what = toks.get(0).trim();
                    headingsProps = HU.parseHtmlProperties((toks.size() > 1)
							   ? toks.get(1)
							   : "");

                    if (what.equals(":navleft")) {
                        headingsProps.put("navleft", "true");
                    } else if (what.equals(":navlist")) {
                        headingsProps.put("navlist", "true");
                    } else if (what.equals(":navpopup")) {
                        headingsProps.put("navpopup", "true");			
                    }
                    headingsNav = "heading_" + HU.blockCnt++;
                    buff.append("${" + headingsNav + "}");
		    if(Utils.getProperty(headingsProps,"fixed",false)) {
			navId = HU.getUniqueId("nav");
			buff.append(HU.open(TAG_DIV,HU.cssClass("ramadda-nav-top-fixed")+HU.style("max-height:" + HU.makeDim(Utils.getProperty(headingsProps,"fixedHeight","1000px"),"px")+";") +HU.attrs("id",navId)));
		    }
                    continue;
                }



                if (tline.startsWith(":heading")
		    ||  tline.startsWith(":lheading")
		    ||  tline.startsWith(":nonavheading")		    
		    ||  tline.startsWith(":noheading")		    
		    || tline.startsWith(":block")
		    || tline.startsWith(":credit")
		    || tline.startsWith(":note")
		    || tline.startsWith(":box")
		    || tline.startsWith(":blurb")
		    || tline.startsWith(":callout")) {
                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
                    String       what  = toks.get(0).substring(1);
                    List<String> toks2 = Utils.splitUpTo(what, "-", 2);
                    what = toks2.get(0);
                    String clazz = toks.get(0).trim().substring(1);
                    String blob  = (toks.size() > 1)
			? toks.get(1)
			: "";
		    boolean noNav = what.startsWith("nonavheading");
		    if(noNav) {
			clazz="ramadda-heading";
			what = "heading";
		    } else  if ( !clazz.equals(what)) {
                        clazz = "ramadda-" + what + "  ramadda-" + clazz;
                    } else {
                        clazz = "ramadda-" + what;
                    }
		    if(tline.startsWith(":heading2")) {
			clazz="ramadda-heading2";
		    }
		    String attrs = "";
                    if (noNav ||
			what.startsWith("heading") || what.startsWith("lheading") || what.startsWith("noheading")) {
			String id = "heading-" +Utils.makeID(getHeadingLabel(blob));
                        if(!what.startsWith("heading2") && !noNav)
			    defineHeading.accept(buff, blob, 1);
			buff.append(HU.anchorName(id));
			blob += HU.span("",HU.attrs("id",id+"-hover",ATTR_CLASS,"ramadda-linkable-link"));
			attrs = HU.attrs("id",id);
			clazz+=" ramadda-linkable ";
                    }
		    if(what.startsWith("noheading")) blob = "";
		    blob = blob.replace("<label>","").replace("</label>","");
                    buff.append(HU.div(HU.div(blob, HU.cssClass(clazz)+attrs),
                                       HU.cssClass("ramadda-" + what
						   + "-outer")));

                    continue;
                }

                if (tline.startsWith("+flow")) {
		    buff.append(HU.open(HU.TAG_DIV,
					HU.style("display:inline-block;vertical-align:top;")));

                    continue;
                }

                if (tline.startsWith("-flow")) {
                    buff.append(HU.close(HU.TAG_DIV));

                    continue;
                }

                if (tline.startsWith("+hbox")) {
		    Hashtable props = getProps.apply(tline);
		    String innerStyle = "";
		    String style = Utils.getProperty(props,"style","");
		    String space = Utils.getProperty(props,"space",null);
		    if(space!=null) innerStyle+=HU.css("margin-left",HU.makeDim(space,"px"));
		    buff.append(HU.open(HU.TAG_DIV,
					HU.style("display:table-cell;vertical-align:top;"+style)));
		    buff.append(HU.open(HU.TAG_DIV,
					HU.style(innerStyle)));

                    continue;
                }

                if (tline.startsWith("-hbox")) {
                    HU.close(buff, HU.TAG_DIV,HU.TAG_DIV);
                    continue;
                }		

                if (tline.startsWith("+mini") || tline.startsWith("+block")
		    || tline.startsWith("+note")
		    || tline.startsWith("+box")
		    || tline.startsWith("+heading")
		    || tline.startsWith("+blurb")) {
                    List<String>  toks = Utils.splitUpTo(tline, " ", 2);
                    String        tag       = toks.get(0).substring(1);
                    //box-green

                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");

                    List<String>  toks2 = Utils.splitUpTo(tag, "-", 2);
                    String        what      = toks2.get(0);
                    //box

                    String        remainder = ((toks2.size() > 1)
					       ? toks2.get(1)
					       : "");
                    //green

                    StringBuilder extra     = new StringBuilder();
                    String        style     = (String)props.get(ATTR_STYLE);    
                    String        background     = (String)props.get("background");
		    if(background!=null) {
			if(style==null) style="";
			style = "background:" + background+";" + style;
		    }


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
                    extra.append(getAttribute(tline, ATTR_CLASS, ""));
                    extra.append(" \" ");
                    buff.append(HU.open(HU.TAG_DIV,
                                        HU.cssClass("ramadda-" + what
						    + "-outer")));
                    buff.append(HU.open(TAG_DIV, extra.toString()));

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


                if (tline.startsWith("+balloon")) {
		    Hashtable props = getProps.apply(tline);
		    boolean right = tline.indexOf("-right")>=0;
		    String balloonClass =  tline.indexOf("-right")>=0?
			"ramadda-balloon-right":"ramadda-balloon-left";
		    balloonAfter= null;
		    if(Utils.getProperty(props,"avatar",false)) {
			avatarCount++;
			if(avatarCount>8) avatarCount=1;
			balloonAfter = HU.div(HU.image(getHandler().getHtdocsUrl("/avatars/avatar" + avatarCount+".png"),ATTR_WIDTH,"40"),
					      HU.cssClass(right?"ramadda-balloon-from-right":
							  "ramadda-balloon-from-left"));
		    }
		    String style=Utils.getProperty(props,ATTR_STYLE,"");
		    String width = Utils.getProperty(props,ATTR_WIDTH,null);
		    String outerStyle="";
		    if(width!=null) outerStyle+="width:" + HU.makeDim(width,"px")+";";
		    buff.append(HU.open(TAG_DIV,HU.style(outerStyle)));
		    buff.append(HU.open(TAG_DIV,HU.cssClass(balloonClass)+HU.style(style)));
		    continue;
		}

                if (tline.startsWith("-balloon")) {
		    buff.append(HU.close(TAG_DIV));
		    if(balloonAfter!=null) buff.append(balloonAfter);
		    buff.append(HU.close(TAG_DIV));		    
		    balloonAfter=null;
		    continue;
		}


                if (tline.startsWith("+row")) {
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    Hashtable props = HU.parseHtmlProperties((toks.size() > 1)
							     ? toks.get(1)
							     : "");
		    if(tline.equals("+rowtight")) props.put("tight","true");
                    rowStates.add(new RowState(buff, props));
                    continue;
                }
                if (tline.startsWith("-row")) {
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
                    List<String> toks = Utils.splitUpTo(tline, " ", 2);
                    if (toks.size() > 1) {
                        HU.comment(buff, toks.get(1));
                    }

                    continue;
                }

                if (tline.startsWith(":rem") || tline.startsWith(":skip")) {
                    continue;
                }

                if (tline.startsWith(":pad")) {
                    List<String> toks   = Utils.splitUpTo(tline, " ", 2);
                    String       height = "100px";
                    if (toks.size() > 1) {
                        height = toks.get(1);
                    }
                    HU.div(buff, "", HU.style("height:" + height));

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

                    List<String> toks  = Utils.splitUpTo(tline, " ", 2);
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

                if (tline.startsWith("-+col-")) {
                    if (rowStates.size() == 0) {
                        wikiError(buff, "Error: unopened column");
                        continue;
                    }
                    rowStates.get(rowStates.size() - 1).closeColumn(buff);
		    tline = tline.substring(1);
		}
                if (tline.startsWith("+col-")) {
                    RowState rowState = null;
                    if (rowStates.size() == 0) {
                        //Add a row if we're not in one
                        rowStates.add(rowState = new RowState(buff, null));
                    } else {
                        rowState = rowStates.get(rowStates.size() - 1);
                    }
                    List<String>  toks  = Utils.splitUpTo(tline, " ", 2);
                    StringBuilder extra = new StringBuilder();
                    String        clazz = toks.get(0).substring(1);
                    if (toks.size() > 1) {
                        String attrs = toks.get(1);
                        String style = getAttribute(attrs, ATTR_STYLE);
                        if (style != null) {
                            extra.append(HU.style(style));
                        }
                        clazz = clazz + " "
			    + getAttribute(attrs, ATTR_CLASS, "");
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
			    if(ulCnt==0)
				buff.append("<ul class=ramadda-ul>\n");				
			    else
				buff.append("<ul>\n");
                            ulCnt++;
                        }
                    } else {
                        while ((starCnt < ulCnt) && (ulCnt > 0)) {
                            buff.append("</ul>\n");
                            ulCnt--;
                        }
                    }
                    buff.append("<li class='ramadda-bullet'> ");
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


	//Add the menus init calls
	for(String menuId:menuIds) {
	    HU.script(buff, "$('#" +menuId+"').menu();\n");
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
                List<String> toks  = Utils.splitUpTo(property, " ", 3);
                String       page  = toks.get(1).trim();
                String       label = (toks.size() > 2)
		    ? toks.get(2).trim()
		    : Utils.makeLabel(page);
                HU.href(sb, "https://en.wikipedia.org/wiki/" + page, label);
            } else if (property.equals(PROP_DOP)) {
                replaceNewlineWithP = true;
            } else {
                String value = handleProperty(property);
                if (value == null) {
                    value = "Unknown tag:" + property;
                }
                sb.append(value);
            }
        }


	if(navId!=null) {
	    sb.append(HU.close(TAG_DIV));
	}
        s = sb.toString();


        if (headingsNav != null) {
            StringBuilder hb = new StringBuilder();
            boolean left = "true".equals(Utils.getProperty(headingsProps,
							   "navleft", "false"));
            boolean list = "true".equals(Utils.getProperty(headingsProps,
							   "navlist", "false"));
            boolean popup = "true".equals(Utils.getProperty(headingsProps,
							    "navpopup", "false"));	    
            String delim;
            int maxLevel = Utils.getProperty(headingsProps, "maxLevel", 100);
            int minLevel = Utils.getProperty(headingsProps, "minLevel", -1);	    
            if (left || list || popup) {
                delim = "<br>";
            } else {
		delim = NAVDELIM;
		delim= HU.span(Utils.getProperty(headingsProps, "delimiter", delim),
			       HU.cssClass("ramadda-nav-top-delimiter"));
	    }
            for (Object o : headings2) {
                Object[] tuple = (Object[]) o;
                int      level = (int) tuple[2];
                if (level > maxLevel || level<minLevel) {
                    continue;
                }
                String id    = (String) tuple[0];
                String label = (String) tuple[1];
                //Handle if this is {{title}} eg.
                if (label.indexOf("{{") >= 0) {
                    label = wikify(label, handler);
                    label = label.replaceAll(".*<.*?>(.*)</.*>.*", "$1");
                }
                if ( !left && (hb.length() > 0)) {
                    hb.append(delim);
                }
                String clazz = " ramadda-link ramadda-nav-link ";
                if ((list || popup) && (level == 0)) {
                    continue;
                }
                if (left) {
                    clazz += " ramadda-nav-link-" + level;
                } else if (list) {
                    clazz += "ramadda-nav-list-link ramadda-nav-list-link-"
			+ level;
                } else if (popup) {
                    clazz += "ramadda-nav-popup-link ramadda-nav-popup-link-"
			+ level;
                }
                String href = HU.mouseClickHref((left?"HtmlUtils.navLinkClicked('":"HtmlUtils.scrollToAnchor('")
						+ id + "',-50,"+(navId==null?"null":"'" + navId+"'")+")", label,
						HU.attrs(ATTR_CLASS, clazz,"id",id+"_href"));
                if (left) {
                    href = HU.div(href,
                                  HU.attrs(ATTR_CLASS, "ramadda-nav-left-link",
                                           "navlink", id));
                } else if (list || popup) {
		}
		hb.append(href);
            }
            if (left) {
		StringBuilder args = new StringBuilder();
		boolean open = Utils.getProperty(headingsProps,"leftOpen", true);
		String theLeftWidth = Utils.getProperty(headingsProps,"leftWidth", "250px");
                String theLeftStyle = (String) Utils.getProperty(headingsProps,
								 "leftStyle", "");
                String theRightStyle = (String) Utils.getProperty(headingsProps,
								  "rightStyle", "");
		String title = Utils.getProperty(headingsProps, ATTR_TITLE,null);
		theLeftStyle = HU.css(ATTR_WIDTH,theLeftWidth) +
		    theLeftStyle;
		args.append("leftOpen:" + open +",");
		args.append("leftWidth:'" + theLeftWidth +"',");		
		if(!open) {
		    theRightStyle+=HU.css("margin-left","0px");
		    theLeftStyle+=HU.css("display","none");		    
		} else {
		    theRightStyle+=HU.css("margin-left",theLeftWidth);
		}
                s = s.replace("${" + headingsNav + "}", "");
		boolean haveLinks = hb.length()>0;
                String leftLinks = HU.div(hb.toString(),
                                          HU.attrs(ATTR_CLASS,"ramadda-nav-left-links"));

		if(title!=null && haveLinks) {
		    leftLinks = "<div class=ramadda-links>" +HU.h3(title) + "</div>\n" + leftLinks;
		}
                s = "<div class=ramadda-nav-horizontal><div class=ramadda-nav-left style='"
                    + theLeftStyle + "'><div id=ramadda-nav-1></div>"
                    + (haveLinks?leftLinks:"")
                    + "<div id=ramadda-nav-2></div><div id=ramadda-nav-3></div></div><div style='"
                    + theRightStyle + "' class=ramadda-nav-right>" + s
                    + "</div></div>" + HU.script("HtmlUtils.initNavLinks({" + args+"})");
            } else if (list) {
                String style = Utils.getProperty(headingsProps, ATTR_STYLE, "");
                s = s.replace("${" + headingsNav + "}",
                              HU.div(hb.toString(),
                                     HU.attrs(ATTR_CLASS, "ramadda-nav-list",
					      ATTR_STYLE, style)));
            } else if (popup) {
                String style = Utils.getProperty(headingsProps, ATTR_STYLE, "");
		String id = HU.getUniqueId("popup");
		String p =  HU.div(hb.toString(),
				   HU.attrs("id",id+"-popup", ATTR_CLASS, "ramadda-shadow-box  ramadda-popup ramadda-nav-popup",
					    ATTR_STYLE, style));
		String icon = HU.span(HU.getIconImage("fa-align-right"),HU.attrs("id",id,ATTR_CLASS,"ramadda-nav-popup-link",ATTR_TITLE,"Click to view table of contents"));
		String container = HU.div(icon +p, HU.attrs(ATTR_CLASS,"ramadda-nav-popup-container"));
                String align = Utils.getProperty(headingsProps, "align", "left");
		String args = JsonUtil.map(Utils.makeListFromValues("align",JsonUtil.quote(align)));
		container += HU.script(JQuery.ready("HtmlUtils.initNavPopup('" + id+"',"+ args+");"));
                s = s.replace("${" + headingsNav + "}",container);


            } else {
                String style = Utils.getProperty(headingsProps, ATTR_STYLE, "");
                s = s.replace("${" + headingsNav + "}",
                              HU.div(hb.toString(),
                                     HU.attrs(ATTR_CLASS, "ramadda-nav-top",
					      ATTR_STYLE, style)));

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
                        if (now.getTime() > dttm.getTime()) {
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
                        if (now.getTime() < dttm.getTime()) {
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
                        sb.append(HU.makeShowHideBlock(title, inner, open,
						       HU.cssClass("wiki-blockheader"),
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
        s = s.replace("_BRACKETOPEN_", "[").replace("_BRACKETCLOSE_", "]");

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
	//	System.err.println("WIKI:" + s);
        return s;

    }





    
    private String handleProperty(String property) {
        property = property.trim();
        if (property.length() == 0) {
            return "";
        }
        property = property.replaceAll("(?m)^\\s*//.*?$", "");
	    property = property.replaceAll(".*<p></p>[\\n\\r]+", "");
	    List<String> toks;
	    int          i1 = property.indexOf(" ");
	    int          i2 = property.indexOf("\n");
	    if ((i1 >= 0) && (i1 < i2)) {
		toks = Utils.splitUpTo(property, " ", 2);
	    } else if (i2 >= 0) {
		toks = Utils.splitUpTo(property, "\n", 2);
	    } else {
		toks = Utils.splitUpTo(property, " ", 2);
	    }
	    if (toks.size() == 0) {
		return "<b>Incorrect tag specification:" + property + "</b>";
	    }
	    String tag = toks.get(0);
	    if (notTags != null) {
		if (notTags.contains(tag)) {
		    return "";
		}
	    }
	    String remainder = "";
	    if (toks.size() > 1) {
		remainder = toks.get(1);
	    }

	    MyHandler myHandler = getHandler(tag);
	    if (myHandler != null) {
		return myHandler.handle(this, tag, remainder);
	    }

	    if (handler != null) {
		return handler.getWikiPropertyValue(this, property, tag,
						    remainder, notTags);
	    }

	    return null;
    }


    
    public HashSet getNotTags() {
        return notTags;
    }



    public NamedValue arg(String name, Object value) {
	return new NamedValue(name, value);
    }


    
    public String getTitle(String label) {
        return getTitle(label, null);
    }

    
    public String getTitle(String label, String style) {
        String url = getTitleUrl(true);
	if(url==null) return label;
	String attrs = "";
	if(style!=null) attrs+=HU.attr("style",style);
	return makeHref(url,label,attrs);
    }

    public String makeHref(String url,String label,String attrs)    {
        String target = getLinkTarget();	
	if(target!=null) attrs+=HU.attr("target",target);	
        return  HU.href(url, label, attrs);
    }


    
    public String getTitleUrl(boolean andClear) {
        String titleUrl = (String) getProperty("title-url");
        if ((titleUrl != null) && andClear) {
            removeProperty("title-url");
        }

        return titleUrl;

    }

    public String getLinkTarget() {
	String target =   (String) getProperty("linktarget");
	if(target==null)target =   (String) getWikiProperty("linktarget");
	return target;
    }    

    
    public void setTitleUrl(String url,String target) {
        putProperty("title-url", url);
	if(target!=null)
	    putProperty("linktarget", target);	    
    }

    
    private void handleEmbed(Appendable buff, String tline) {
        try {
            handleEmbedInner(buff, tline);
        } catch (Exception exc) {
            wikiError(buff, "Error handling embed:" + tline + "<br>" + exc);
        }
    }

    public void  embedGithub(Appendable sb,Hashtable props) throws Exception {
	try {
	    embedGithubInner(sb, props);
	} catch(Exception exc) {
	    sb.append("Error embedding github:"+ exc.getMessage());
	    exc.printStackTrace();
	}
    }

    public void  embedGithubInner(Appendable sb,Hashtable props) throws Exception {	
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(Utils.TIMEZONE_DEFAULT);
        SimpleDateFormat rsdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmz");
        rsdf.setTimeZone(Utils.TIMEZONE_DEFAULT);	
	List<Github.Item> items = Github.fetch(handler,props);
	boolean decorate = Utils.getProperty(props,"decorate",true);
	boolean showAuthor = Utils.getProperty(props,"showAuthor",true);	
	String height = Utils.getProperty(props,ATTR_HEIGHT,"200");
	for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
	    Github.Item item=  items.get(itemIdx);
	    if(itemIdx==0) {
		sb.append("<table table-height='" + height+"' class='ramadda-table stripe'><thead><tr>");
		if(showAuthor)sb.append("<th width=30%>Author</th>");
		sb.append("<th width=20%>Date</th><th>Commit</th></tr></thead><tbody>");
	    }
	    Github.User user = item.getUser(); 
	    String name = user.getName();
	    if(decorate && user.getAvatarUrl()!=null) {
		name = HU.image(user.getAvatarUrl(),ATTR_WIDTH,"40px") +" "+ name;
	    }
	    name= makeHref(user.getUrl(),name,"");
	    String date = sdf.format(item.getDate());
	    String message= makeHref(item.getItemUrl(),item.getMessage(),"");
	    HU.row(sb,showAuthor?HU.cols(name,date,message):HU.cols(date,message),HU.cssClass("search-component"));
	}
	if(items.size()>0) {
	    sb.append("</table>");
	} else {
	    sb.append("Github: No commits found");
	}
    }

    
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
        String url = tline;
	embedMedia(buff,url,props);
    }

    public void embedMedia(Appendable buff, String url, Hashtable props)
	throws Exception {	
        boolean link = Misc.equals("true",
                                   getWikiProperty(props, "link",
						   "embedLink", "false"));
        boolean decorate = Misc.equals("true",""+getWikiProperty(props,  "decorate","decorateEmbed","false"));
        String label = (String) getWikiProperty(props, "label", "embedLabel",
						null);
        String width = (String) getWikiProperty(props, ATTR_WIDTH, "embedWidth",
						"640");

	if(width.endsWith("%")) {
	    width = "640";
	}
        String height = (String) getWikiProperty(props, ATTR_HEIGHT,   "embedHeight", "390");
        String style = (String)getWikiProperty(props, ATTR_STYLE, "embedStyle",getWikiProperty(props, ATTR_STYLE, ATTR_STYLE,null));
        StringBuilder sb = new StringBuilder();

        boolean isFacebook =
            Pattern.matches("https://www.facebook.com/.*/posts/\\d+", url);
        //Don't bother with the oembed for facebook as it requires an access token
	String article;
        if (isFacebook) {
            if (getProperty("embedfacebook") == null) {
                putProperty("embedfacebook", "true");
                //This is the RAMADDA app id
                buff.append(
			    "<div id='fb-root'></div><script async defer crossorigin='anonymous' src='https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v10.0&appId=53108697449' nonce='bNyUnLLe'></script>");
            }
            width = Utils.getProperty(props, ATTR_WIDTH, "500");
            sb.append("<div class='fb-post' data-href='" + url
                      + "' data-width='" + width
                      + "' data-show-text='true'></div>");
	} else if(Pattern.matches("https://music.apple.com/.*\\d+",url)) {
	    url = url.replace("https://music","https://embed.music");
	    sb.append("<iframe allow='autoplay *; encrypted-media *; fullscreen *' frameborder='0' height='" + height+"' style='width:100%;max-width:660px;overflow:hidden;background:transparent;' sandbox='allow-forms allow-popups allow-same-origin allow-scripts allow-storage-access-by-user-activation allow-top-navigation-by-user-activation' src='" + url+"'></iframe>");

	} else if((article=StringUtil.findPattern(url,"https://.*.wikipedia.org/wiki/(.*)"))!=null) {
	    String wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" +article;
            JSONObject obj = null;

	    try {
		obj = new JSONObject(
				     IO.readContents(
						     wikiUrl));
	    } catch(Exception exc) {
		if(!Utils.getProperty(props, "ignoreError",false))
		    buff.append("Error reading Wikipedia URL: " + url);
		return;
	    }
	    String thumb = null;
	    if(Utils.getProperty(props, "showImage",true))  {
		thumb = JsonUtil.readValue(obj,"thumbnail.source",null);
		if(thumb!=null) {
		    String iwidth = Utils.getProperty(props, "imageWidth","200px");
		    thumb = HU.image(thumb,ATTR_WIDTH,HU.makeDim(iwidth));
		}
	    }

	    String title = obj.getString(ATTR_TITLE);
	    String wurl = JsonUtil.readValue(obj, "content_urls.desktop.page","");
            width = Utils.getProperty(props, ATTR_WIDTH,null);


            height = Utils.getProperty(props, ATTR_HEIGHT,"200px");	    
	    String extract = obj.optString("extract_html","").trim();
	    extract=extract.replaceAll("^(<p>)+","").replaceAll("(</p>)+$","");
	    if(height!=null) {
		extract = HU.div(extract,HU.style("max-height:" + HU.makeDim(height)+";overflow-y:auto;"));
	    }
	    String source = "<div style='text-align:center;font-style:italic;font-size:80%;'>"+
		makeHref(url,"Source: Wikipedia","")+"</div>";


	    if(thumb!=null) {
		extract = HU.leftRight(extract,HU.div(thumb+source,HU.style("margin-left:5px;")));
	    } else {
		extract+=source;
	    }
	    if(width!=null) {
		extract = HU.div(extract,HU.style("width:" + HU.makeDim(width)));
	    }
            String wstyle = Utils.getProperty(props, ATTR_STYLE,"padding:5px;");
	    String pstyle =  Utils.getProperty(props, ATTR_STYLE,"");	    
	    extract = HU.div(extract,HU.style("display:inline-block;" +  wstyle+pstyle));
	    String header = "";
	    if(Utils.getProperty(props, "addHeader",true)) 
		header  = HU.div(HU.href(wurl,title,"target='_other' style='text-decoration:none;' "),HU.cssClass("wikipedia-header"));
	    extract = HU.div(header+extract,HU.cssClass("wikipedia-summary") + HU.style(""));
	    sb.append(extract);
        } else if(Pattern.matches("",url)) {
	    sb.append("");
        } else if(url.equals("github")) {
	    embedGithub(sb,props);
	} else {
	    Oembed.Response response = Oembed.get(url, width, height);
            if (response != null) {
                sb.append(response.getHtml());
            } else {
                buff.append(HU.href(url, (label != null)
				    ? label
				    : url));
                return;
            }
        }

	if(decorate) {
            sb = new StringBuilder(HU.div(sb.toString(), "class=wiki-embed-decorated"));
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
                                          HU.style("display:inline-block;"
						   + style)));
        }
        buff.append(sb);
    }



    
    public void handleVega(Appendable sb, String chunk,
                           WikiPageHandler handler)
	throws IOException {
        boolean addResources = getProperty("vegaimport") == null;
        if ( !addResources) {
            putProperty("vegaimport", "true");
        }
        if (addResources) {
            sb.append("\n");
	    sb.append("<script src='https://cdn.jsdelivr.net/npm/vega@5'></script>\n");
	    sb.append("<script src='https://cdn.jsdelivr.net/npm/vega-lite@5'></script>\n");
	    sb.append("<script src='https://cdn.jsdelivr.net/npm/vega-embed@6'></script>\n");
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

    
    public boolean handleCode(Appendable sb, Chunk chunk,
			      WikiPageHandler handler, boolean doDflt)
	throws IOException {
        if (chunk.rest.equals("vega-lite")) {
            handleVega(sb, chunk.buff.toString(), handler);
            return true;
        }
        if (chunk.rest.equals("markdown") || chunk.rest.equals("md")) {
            String srcId    = HU.getUniqueId("markdownsrc");
            String targetId = HU.getUniqueId("markdownsrc");
            HU.div(sb, chunk.buff.toString(),
                   HU.attrs("id", srcId, ATTR_STYLE, "display:none;"));
            HU.div(sb, chunk.buff.toString(),
                   HU.attrs("id", targetId, ATTR_STYLE, ""));
            HU.script(sb,
                      "HtmlUtils.applyMarkdown('" + srcId + "','" + targetId
                      + "');");

            return true;
        }
        if (chunk.rest.equals("latex")) {
            String srcId    = HU.getUniqueId("latexsrc");
            String targetId = HU.getUniqueId("latexsrc");
            HU.div(sb, chunk.buff.toString(),
                   HU.attrs("id", srcId, ATTR_STYLE, "display:none;"));
            HU.div(sb, chunk.buff.toString(),
                   HU.attrs("id", targetId, ATTR_STYLE, ""));
            HU.script(sb,
                      "HtmlUtils.applyLatex('" + srcId + "','" + targetId
                      + "');");

            return true;
        }
        if (chunk.rest.equals("javascript") || chunk.rest.equals("js")) {
            sb.append(
		      HU.cssLink(
				 handler.getHtdocsUrl("/lib/prettify/prettify.css")));
            sb.append(
		      HU.importJS(
				  handler.getHtdocsUrl("/lib/prettify/prettify.js")));
            String id = HU.getUniqueId("javascript");
            HU.open(sb,"pre","class='prettyprint'");
            int cnt = 0;
	    String c  = chunk.buff.toString().trim();
	    boolean seenOne = false;
            for (String line : c.split("\n")) {
                cnt++;
                line = line.replace("\r", "");
		if(line.length()==0 &&!seenOne) continue;
		seenOne = true;
                line = HU.entityEncode(line);
                sb.append("<span class=nocode><a "
                          + HU.attr("name", "line" + cnt)
                          + "></a><a href=#line" + cnt + ">" + cnt
                          + "</a></span>" + HU.space(1) + line + "<br>");
            }
            sb.append("</pre>\n");
	    sb.append(HU.script("prettyPrint();"));

            return true;
        }
        if (chunk.rest.equals("raw") || doDflt) {
	    HU.pre(sb,   chunk.buff.toString());
	    return true;
	}
	return false;
    }



    
    private String getSize(String s) {
        if (s == null) {
            return null;
        }
        if (s.endsWith("%") || s.endsWith("px")) {
            return s;
        }

        return s + "px";
    }


    
    private String getAttribute(String line, String attr) {
        return getAttribute(line, attr, null);
    }

    
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


    
    public String getWikiVariable(String key) {
        return (String) myVars.get(key);
    }

    
    private Hashtable lineToProps(String tline) {
        List<String> toks  = Utils.splitUpTo(tline, " ", 2);
        Hashtable    props = HU.parseHtmlProperties((toks.size() > 1)
						    ? toks.get(1)
						    : "");

        return props;
    }



    
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
            toc.append(StringUtil.repeat("&nbsp;&nbsp;", level - 1));
            toc.append("<a href=\"#" + label + "\">");
            toc.append(prefix);
            toc.append(HU.space(1));
            toc.append(label);
            toc.append("</a><br>\n");
            currentLevel = level;
        }

    }



    
    public void setMakeHeadings(boolean value) {
        this.makeHeadings = value;
    }

    
    public boolean getMakeHeadings() {
        return this.makeHeadings;
    }


    
    public void setReplaceNewlineWithP(boolean value) {
        this.replaceNewlineWithP = value;
    }

    
    public boolean getReplaceNewlineWithP() {
        return this.replaceNewlineWithP;
    }

    
    public void setMobile(boolean value) {
        mobile = value;
    }

    
    public boolean getMobile() {
        return mobile;
    }

    
    public void setUser(String value) {
        user = value;
    }

    
    public String getUser() {
        return user;
    }

    
    public static void note(Appendable sb, String note) throws Exception {
        sb.append("\n+note\n");
        sb.append(note);
        sb.append("\n-note\n");
    }

    public static String note(String note) throws Exception {
        return "\n+note\n"+note+"\n-note\n";
    }

    public static String box(String note) throws Exception {
        return "\n+box\n"+note+"\n-box\n";
    }        

    
    public static void title(Appendable sb, String s) throws Exception {
        sb.append("\n+title\n");
        sb.append(s);
        sb.append("\n-title\n");
    }


    
    public static String wikiError(Appendable buff, String msg) {
        try {
	    String m =   HU.span(msg + "", HU.cssClass("ramadda-wiki-error"));
	    buff.append(m);
	    return m;
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }

    
    public static void heading(Appendable sb, String heading)
	throws Exception {
        sb.append("\n:heading ");
        sb.append(heading.trim());
        sb.append("\n");
    }


    
    public static class ContentState {

        
        String id;

        
        public ContentState() {
            this.id = HU.getUniqueId("contents");
        }
    }



    
    public static class TabState extends ContentState {

        
        StringBuilder title = new StringBuilder();

	boolean cullEmpty=false;
        
        int cnt = 0;

        
        String minHeight;

        
        public TabState() {}
    }



    
    public static class AccordionState extends ContentState {

        
        int segmentId = 0;

        
        int activeSegment = 0;

        
        String heightStyle = "content";

        
        boolean collapsible = true;

        
        int animate = 200;

        
        boolean decorate = true;

        
        public AccordionState() {}
    }


    
    public static class RowState {

        
        int colCnt = 0;

        
        Hashtable props;

        
        public RowState(Appendable buff, Hashtable props) {
            try {
                String clazz = "row wiki-row";
                if (props != null) {
                    String c = (String) props.get("tight");
                    if (c != null) {
                        clazz += " row-tight ";
                    }
                }
                HU.open(buff, TAG_DIV, HU.clazz(clazz));
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        
        public void closeRow(Appendable buff) {
            try {
                closeColumns(buff);
                buff.append("</div>");
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        
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

        
        public void openColumn(Appendable buff, String attrs) {
            try {
                closeColumns(buff);
                HU.open(buff, TAG_DIV, attrs);
                colCnt++;
            } catch (Exception exc) {
                throw new IllegalArgumentException(exc);
            }
        }

        
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


    
    public static class TableState {

        
        boolean inHead = true;

        
        boolean inRow = false;

        
        boolean inBody = false;

        
        boolean inTr = false;

        
        boolean inTd = false;

        
        public TableState() {}



    }


    
    public static class Chunk {

        
        static int TYPE = 0;

        
        static int TYPE_NA = -1;


        
        static int TYPE_WIKI = TYPE++;

        
        static int TYPE_CODE = TYPE++;

        
        static int TYPE_NOWIKI = TYPE++;

        
        static int TYPE_CSS = TYPE++;

        
        static int TYPE_JS = TYPE++;

        static int TYPE_JSTAG = TYPE++;	

        
        static int TYPE_PRE = TYPE++;

        static int TYPE_PRETAG = TYPE++;	

        
        static int TYPE_XML = TYPE++;


        
        int type;

        
        StringBuilder buff = new StringBuilder();

	String attrs;

        
        String rest;

        
        Chunk(int type) {
            this.type = type;
        }

        
        Chunk(int type, String rest) {
            this(type);
            this.rest = rest.trim();
        }

        public Chunk(String rest, StringBuilder sb) {
	    this.rest = rest;
	    buff=  sb;
	}


        
        public void append(String line) {
            append(line, true);
        }

        
        public void append(String line, boolean addNewline) {
            buff.append(line);
            if (addNewline) {
                buff.append("\n");
            }
        }

        
        private static String getTypeName(int type) {
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
            if (type == TYPE_JSTAG) {
                return "JS";
            }	    
            if (type == TYPE_PRE) {
                return "PRE";
            }
            if (type == TYPE_XML) {
                return "XML";
            }	    
            if (type == TYPE_PRETAG) {
                return "PRE";
            }	    
            if (type == TYPE_NOWIKI) {
                return "NOWIKI";
            }

            return "NA";

        }

        
        public String toString() {
            //.replaceAll("\n", "_NL_");
            String tmp = this.buff.toString();
            tmp = tmp.replaceAll("\n", "_NL_");

            return getTypeName(type) + ":" + tmp;
        }

        
        public static List<Chunk> splitText(WikiUtil wikiUtil, String s) {

            boolean debug = false;
            if (debug) {
                System.err.println("splitText:" + s);
            }
            List<Chunk> chunks = new ArrayList<Chunk>();
            //      s = s.replaceAll("</pre>(^\\R)?","</pre>\n$1");
            String[] lines           = s.split("\n");
            Chunk    chunk           = null;
            String[] prefixes        = new String[] {
                "<nowiki>", "+css", "+javascript", "+script","<script", "+pre", "<pre", "```","+xml"
            };
            String[] suffixes        = new String[] {
                "</nowiki>", "-css", "-javascript", "-script","</script>", "-pre", "</pre>", "```","-xml"
            };
            int[]    types           = new int[] {
                TYPE_NOWIKI, TYPE_CSS, TYPE_JS, TYPE_JS,TYPE_JSTAG,TYPE_PRE, TYPE_PRETAG, TYPE_CODE,TYPE_XML
            };
            String   lookingForClose = null;
	    boolean ifOk = true;
	    boolean inIf = false;
            for (String line : lines) {
                if (line.startsWith("+if")) {
                    List<String> toks = Utils.splitUpTo(line, " ", 2);
		    String ifAttrs = toks.size()>1?toks.get(1):"";
		    ifOk = wikiUtil.handler.ifBlockOk(wikiUtil, ifAttrs);
		    inIf = true;
		    continue;
		}
                if (line.startsWith("-if")) {
		    if(!inIf) {
			//			wikiError(buff,"Error: no opening +if<br>");
			continue;
		    }
		    inIf=false;
		    ifOk=true;
		    continue;
		} 
		if(inIf && !ifOk) continue;




                int currentType = (chunk != null)
		    ? chunk.type
		    : TYPE_NA;
                if (debug) {
                    System.err.println("code:" + getTypeName(currentType)
                                       + " LINE:" + line);
                }
                boolean gotIt = false;
                if (currentType == TYPE_PRETAG) {
                    int index = line.indexOf("</pre>");
                    if (index > 0) {
                        String preStuff = line.substring(0, index);
                        chunk.append(preStuff, false);
                        chunk = null;
                        line  = line.substring(index + 6).trim();
                    }
                }
                if (currentType == TYPE_JSTAG) {
                    int index = line.indexOf("</script>");
                    if (index > 0) {
                        String jsStuff = line.substring(0, index);
                        chunk.append(jsStuff, false);
                        chunk = null;
                        line  = line.substring(index + 9).trim();
                    }
                }

                if ((currentType == TYPE_WIKI) || (currentType == TYPE_NA)) {
                    for (int i = 0; i < prefixes.length; i++) {
                        String prefix = prefixes[i];
			String tline = line.trim();

                        if (Utils.startsWithIgnoreCase(tline, prefix)) {
                            gotIt = true;
                            int type = types[i];
                            String rest =
                                tline.substring(prefix.length()).trim();
                            lookingForClose = suffixes[i];
                            if (debug) {
                                System.err.println("opened:"
						   + getTypeName(type) + " rest:" + rest
						   + " looking for close:"
						   + lookingForClose);
                            }
                            if (type == TYPE_CODE||type==TYPE_PRE || type==TYPE_XML) {
                                chunks.add(chunk = new Chunk(type, rest));
                            } else {
                                chunks.add(chunk = new Chunk(type));
				if (type == TYPE_PRETAG || type==TYPE_JSTAG) {
				    String theTag = (type == TYPE_PRETAG?"pre":"script");
				    int gtIndex = rest.indexOf(">");
				    if(gtIndex>=0) {
					String attrs = rest.substring(0,gtIndex);
					chunk.attrs = attrs;
					rest = rest.substring(gtIndex+1);
				    }
				    int index = rest.indexOf("</" + theTag+">");
				    if (index<0) {
					//Strip off the '>'
					chunk.append(rest);
				    } else {
					lookingForClose = null;
					//                                      <theTag>.....</theTag>
					String preStuff =
					    rest.substring(0, index);
					chunk.append(preStuff, false);
					line = rest.substring(index + 2 +theTag.length()+1).trim();
					chunk = null;
					if (debug) {
					    System.err.println(
							       theTag +" tag had a close pre. rest of line:"
							       + line);
					}
					if (line.length() > 0) {
					    if (debug) {
						System.err.println(
								   "setting gotit to false so we continue processing line");
					    }
					    gotIt = false;
					}

					break;
				    }
				} else {
				    chunk.append(rest);
                                }
                            }

                            break;
                        }
                    }
                    if (gotIt) {
                        continue;
                    }
                }

                if (lookingForClose != null) {
                    if (debug) {
                        System.err.println("Looking for:" + lookingForClose);
                    }
                    if (Utils.startsWithIgnoreCase(line, lookingForClose)) {
                        //Not quite sure what to do with 
                        String rest =
                            line.substring(lookingForClose.length());
                        line            = null;
                        lookingForClose = null;
                        if (debug) {
                            System.err.println("closed:" + rest);
                        }
                        if (rest.length() > 0) {
                            line = rest;
                        }
                        chunk = null;
                    }
                    if (line == null) {
                        continue;
                    }
                }
                if (chunk == null) {
                    chunks.add(chunk = new Chunk(Chunk.TYPE_WIKI));
                }
                if (debug) {
                    System.err.println("appending:" + getTypeName(chunk.type)
                                       + " line:" + line);
                }
                chunk.append(line);
            }
	    for(Chunk c: chunks) {
		//		System.err.println("chunk: "+ c.getTypeName(c.type)+" attrs:" + c.attrs +"\nrest:" + c.rest+":\nchunk:" + c.buff +":");
	    }


            if (debug) {
                System.err.println("done:" + chunks);
            }

            return chunks;

        }
    }



    
    public interface MyHandler {

        
        public String handle(WikiUtil wikiUtil, String tag, String remainder);
    }


    
    private static Object handlerMutex = new Object();

    
    private static Hashtable<String, MyHandler> myHandlers;

    
    private static void addHandler(String tag, MyHandler handler) {
        if (myHandlers == null) {
            myHandlers = new Hashtable<String, MyHandler>();
        }
        myHandlers.put(tag, handler);
    }

    
    private static String errorTag(String msg) {
        return HU.b(msg);
    }


    
    private static void makeHandlers() {
        addHandler("code", (wikiUtil, tag, remainder) ->{return HU.span(remainder,HU.attrs(ATTR_CLASS,"ramadda-code"));});
	//        addHandler("fa",(wikiUtil, tag, remainder) ->{return "<span><i class='fa " + remainder +"'></i></span>";});
        addHandler("path",(wikiUtil, tag, remainder) ->{
                StringBuilder sb = new StringBuilder();
                String delim =  " <span style='color:#000;font-size:10pt;'><i class='fas fa-caret-right'></i></span> ";
                String path = Utils.join(Utils.parseCommandLine(remainder),delim);
                return HU.span(path,HU.attrs(ATTR_CLASS,"ramadda-code ramadda-code-path"));
            });
        addHandler("counter",(wikiUtil, tag, remainder) ->{
                Hashtable props = HU.parseHtmlProperties(remainder);
                String key = wikiUtil.getProperty(props, "key", "key") +"_counter";
                Integer count = (Integer)wikiUtil.getProperty(key);
                if(count == null) {
                    count = Integer.valueOf(1);
                } else {
                    count = Integer.valueOf(count.intValue()+1);
                }
                wikiUtil.putProperty(key, count);
                return count.toString();
            });
        MyHandler imageHandler = (wikiUtil, tag, remainder) ->{
            Hashtable props = HU.parseHtmlProperties(remainder);
            boolean image2 = tag.equals("image2");
            String prefix = getProperty(wikiUtil, props, "prefix","Image #:");
            String label = getProperty(wikiUtil, props, "label","");
            if(prefix.equals("none")) prefix = null;
            if(label.length()>0 && prefix!=null) {
                if(prefix.indexOf("#")>=0) {
                    Integer count = (Integer)wikiUtil.getProperty("imagecaption");
                    if(count == null) {
                        count = Integer.valueOf(1);
                    } else {
                        count = Integer.valueOf(count.intValue()+1);
                    }
                    wikiUtil.putProperty("imagecaption", count);
                    prefix = prefix.replace("#",count.toString());
                }
                label = prefix +" " + label;
            }
            String caption = "";
            if(label.length()>0)
                caption = HU.div(label,HU.attrs(ATTR_CLASS,"ramadda-caption"));
            if(image2) {
                String src = getProperty(wikiUtil, props, "src",null);
                String style = getProperty(wikiUtil, props, ATTR_STYLE,"");
                String width = getProperty(wikiUtil, props, ATTR_WIDTH,"none");
                if(src==null) {
                    return errorTag("No src given");
                }
                String image = HU.href(src,HU.image(src,ATTR_STYLE,style,"alt",label,ATTR_WIDTH,width.equals("none")||width.equals("")?null:width)) + caption;
                return HU.anchorName(src) + HU.div(image,HU.attrs(ATTR_CLASS,"ramadda-image-centered"));
            }
            return caption;
        };
        addHandler("image2",imageHandler);
        addHandler("caption",imageHandler);
        addHandler("javascript",(wikiUtil, tag, remainder)->{
                Hashtable props = HU.parseHtmlProperties(remainder);
                String path = (String) props.get("path");
                if (path == null) {
                    return errorTag("No path attribute specified");
                }
                if (path.startsWith("/")) {
                    path = wikiUtil.getHandler().getHtdocsUrl(path);
                }
                return HU.importJS(path);
            });
        addHandler("odometer",(wikiUtil,tag,remainder) ->{
                Hashtable props = HU.parseHtmlProperties(remainder);
                String initCount = wikiUtil.getProperty(props, "initCount", "0");
                String count     = wikiUtil.getProperty(props, "count", "100");
                boolean immediate = wikiUtil.getProperty(props, "immediate",false);
                StringBuilder buff  = new StringBuilder();
                String        id    = HU.getUniqueId("odometer");
                String        style = wikiUtil.getProperty(props, ATTR_STYLE, "");
                String        pause = wikiUtil.getProperty(props, "pause", "0");
                if (wikiUtil.getProperty("added odometer") == null) {
                    wikiUtil.putProperty("added odometer", "yes");
                    buff.append(
                                HU.cssLink(
                                           wikiUtil.getHandler().getHtdocsUrl(
									      "/lib/odometer/odometer-theme-default.css")));
                    buff.append(
                                HU.importJS(
                                            wikiUtil.getHandler().getHtdocsUrl(
									       "/lib/odometer/odometer.js")));
                }

                buff.append(HU.span(initCount,
                                    HU.id(id)
                                    + HU.cssClass("odometer")
                                    + HU.style(style)));
                buff.append(HU.script("HU.initOdometer('" + id
                                      + "'," + count + "," + pause + ","
                                      + immediate + ");"));

                return buff.toString();
            });
    }

    public static MyHandler getHandler(String tag) {
        if (myHandlers == null) {
            synchronized (handlerMutex) {
                makeHandlers();
            }
        }
        return myHandlers.get(tag);
    }

}

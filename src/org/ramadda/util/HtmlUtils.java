/**
Copyright (c) 2008-2024 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.util;


import org.apache.commons.net.ftp.*;


import org.apache.commons.text.StringEscapeUtils;


import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.awt.Color;

import java.io.IOException;
import java.io.StringWriter;

import java.lang.reflect.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.*;




@SuppressWarnings("unchecked")
public class HtmlUtils implements HtmlUtilsConstants {




    public static final String NL = "&#013;";
    public static final String SPACE = "&nbsp;";
    public static final String SPACE2 = "&nbsp;&nbsp;";
    public static final String SPACE3 = "&nbsp;&nbsp;&nbsp;";
    public static final String AUDIO_HEIGHT = "40";
    public static final String ICON_CLOSE = "fas fa-window-close";
    public static final String CSS_CLASS_POPUP_CONTENTS =  "ramadda-popup-contents";
    public static final String CSS_CLASS_POPUP = "ramadda-popup";

    public static String comma(String... s) {
        return StringUtil.join(",", s);
    }
    
    
    public static void comma(Appendable sb, String... s) {
        try {
            for (int i = 0; i < s.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(s[i]);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }




    
    public static String open(String comp) {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(comp);
        sb.append(">");

        return sb.toString();
    }




    
    public static String open(String comp, String... attrs) {
        StringBuilder sb = new StringBuilder();
        open(sb, comp, attrs);

        return sb.toString();
    }

    
    public static Appendable open(Appendable sb, String tag,
                                  String... attrs) {
        try {
            sb.append("<");
            sb.append(tag);
            if (attrs.length == 1) {
                sb.append(" ");
                sb.append(attrs[0]);
                sb.append(" ");
            } else {
                attrs(sb, attrs);
            }
            sb.append(">");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }

    
    public static void dangleOpen(Appendable sb, String comp) {
        try {
            sb.append("<");
            sb.append(comp);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    
    public static void dangleClose(Appendable sb) {
        try {
            sb.append(">");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    
    public static String close(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String comp : args) {
            close(sb, comp);
        }

        return sb.toString();
    }

    
    public static Appendable close(Appendable sb, String... args) {
        try {
            for (String comp : args) {
                if (comp.equals("\n")) {
                    sb.append(comp);
                } else {
                    sb.append("</");
                    sb.append(comp);
                    sb.append(">");
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }


    
    public static String tag(String comp) {
        StringBuilder sb = new StringBuilder();
        tag(sb, comp);

        return sb.toString();
    }


    
    public static Appendable tag(Appendable sb, String tag) {
        try {
            sb.append("<");
            sb.append(tag);
            sb.append("/>");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }


    
    public static String tag(String comp, String attrs) {
        StringBuilder sb = new StringBuilder();
        tag(sb, comp, attrs);

        return sb.toString();
    }

    
    public static Appendable tag(Appendable sb, String comp, String attrs) {
        try {
            sb.append("<");
            sb.append(comp);
            sb.append(attrs);
            sb.append("/>");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }



    
    public static String tag(String tag, String attrs, String inner) {
        StringBuilder sb = new StringBuilder();
        tag(sb, tag, attrs, inner);

        return sb.toString();
    }

    
    public static Appendable tag(Appendable sb, String tag, String attrs,
                                 String inner) {
        try {
            if (inner != null) {
                open(sb, tag, attrs);
                sb.append(inner);
                close(sb, tag);
            } else {
                tag(sb, tag, attrs);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }


    
    public static Appendable makeTag(Appendable sb, String tag, String inner,
                                     String... attrs) {
        try {
            open(sb, tag, attrs);
            sb.append(inner);
            close(sb, tag);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }


    
    public static String hiddenBase64(String name, Object value) {
        String s = value.toString();

        return hidden(name, Utils.encodeBase64(s), "");
    }


    
    public static String hidden(String name, Object value) {
        return hidden(name, value, "");
    }

    
    public static String hidden(String name, Object value, String extra) {
        StringBuilder sb = new StringBuilder();
        hidden(sb, name, value, extra);

        return sb.toString();
    }


    
    public static Appendable hidden(Appendable sb, String name, Object value,
                                    String extra) {
        tag(sb, TAG_INPUT,
            extra
            + attrs(ATTR_TYPE, TYPE_HIDDEN, ATTR_NAME, name, ATTR_VALUE,
                    "" + value, ATTR_CLASS, CLASS_HIDDENINPUT));

        return sb;
    }



    
    public static String hbox(Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr valign=top>");
        for (Object s : args) {
            sb.append("<td>" + s + "</td>");
        }
        sb.append("</tr></table>");

        return sb.toString();
    }

    
    public static void br(Appendable sb) throws Exception {
        sb.append("<br>");
    }


    
    public static String br() {
        return "<br>";
    }

    
    public static String br(String line) {
        return line + open(TAG_BR);
    }

    
    public static String hr() {
        return open(TAG_HR,clazz("ramadda-hr"));
    }

    
    public static String p() {
        return "<p>";
    }

    public static String vspace() {
	return vspace("0.5em");
    }

    public static String vspace(String space) {
	return div("",style("margin-top:" + space));
    }    



    
    public static String nobr(String inner) {
        return tag(TAG_NOBR, "", inner);
    }


    
    public static void b(Appendable sb, String inner) throws Exception {
        tag(sb, TAG_B, inner);
    }


    
    public static String b(String inner) {
        return tag(TAG_B, "", inner);
    }

    
    public static String italics(String inner) {
        return tag(TAG_I, "", inner);
    }

    
    public static String li(String inner, String extra) {
        return tag(TAG_LI, extra, inner);
    }


    
    public static Appendable centerBlock(Appendable sb, String inner)
            throws Exception {
        sb.append(
            "<center><div style='display:inline-block;text-align:left;'>");
        sb.append(inner);
        sb.append("</div></center>");

        return sb;
    }


    public static String centerDiv(String contents) {
	return open(TAG_DIV,
		    style("text-align:center;")) +
	    div(contents,
		style(
			    "display:inline-block;text-align:left;")) +
	    close(TAG_DIV);
    }


    
    public static String center(String inner) {
        return tag(TAG_CENTER, "", inner);
    }

    
    public static void center(Appendable sb, String inner) throws Exception {
        tag(sb, TAG_CENTER, "", inner);
    }



    
    public static String pad(String s) {
        StringBuilder sb = new StringBuilder();
        pad(sb, s);

        return sb.toString();
    }

    
    public static Appendable pad(Appendable sb, String s) {
        try {
            String space = space(1);
            sb.append(space);
            sb.append(s);
            sb.append(space);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }

    
    public static String padLeft(String s) {
        StringBuilder sb = new StringBuilder();
        padLeft(sb, s);

        return sb.toString();
    }

    
    public static Appendable padLeft(Appendable sb, String s) {
        try {
            sb.append(space(1));
            sb.append(s);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }

    
    public static String padRight(String s) {
        StringBuilder sb = new StringBuilder();
        padRight(sb, s);

        return sb.toString();
    }



    
    public static Appendable padRight(Appendable sb, String s) {
        try {
            sb.append(s);
            sb.append(space(1));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }


    
    public static String backButton(String label) {
        String link = href("javascript:history.back()", (label != null)
                ? label
                : "Back");

        return button(link);
    }


    
    public static String button(String html) {
        return span(html, cssClass("ramadda-button"));
    }

    
    public static String buttons(String... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
	    if(args[i]==null) continue;
            if (i > 0) {
                sb.append(buttonSpace());
            }
            sb.append(args[i]);
        }

        return sb.toString();
    }

    
    public static String buttons(List<String> args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
	    Object o =args.get(i); 
	    if(o==null) continue;
            if (i > 0) {
                sb.append(buttonSpace());
            }
            sb.append(o);
        }

        return sb.toString();
    }


    
    public static String buttonSpace() {
        return space(2);
    }

    
    public static String space(int cnt) {
        if (cnt == 1) {
            return " ";
        }
        if (cnt == 2) {
            return ENTITY_NBSP2;
        }
        if (cnt == 3) {
            return ENTITY_NBSP3;
        }
        if (cnt == 4) {
            return ENTITY_NBSP4;
        }
        String s = "";
        while (cnt-- > 0) {
            s = s + ENTITY_NBSP;
        }

        return s;
    }

    
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        quote(sb, s);

        return sb.toString();
    }


    
    public static Appendable quote(Appendable sb, String s) {
        try {
            sb.append("\"");
            //            s = s.replaceAll("\"", "\\\"");
            s = s.replaceAll("\"", "&quot;");
            sb.append(s);
            sb.append("\"");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }

    
    public static String squote(Object s) {
        StringBuilder sb = new StringBuilder();
        squote(sb, s);

        return sb.toString();
    }

    
    public static Appendable squote(Appendable sb, Object o) {
        try {
	    String s = o.toString();
            sb.append("'");
            if (s.indexOf("\\'") <= 0) {
                s = s.replace("'", "\\'");
            }
            sb.append(s);
            sb.append("'");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }

    
    public static String img(String path) {
        return img(path, "");
    }

    
    public static String img(String path, String title) {
        return img(path, title, "");
    }

    
    public static String image(String path, String... args) {
        return img(path, null, attrs(args));
    }


    
    public static String img(String path, String title, String extra) {
        return image(path, title, extra, null);
    }



    
    public static String image(String path, String title, String extra,
                               String inner) {
        if (Utils.stringDefined(title)) {
            if (isFontAwesome(path)) {
                return faIcon(path, attrs(ATTR_TITLE, title) + " " + extra);
            }

            return tag(TAG_IMG,
                       attrs(ATTR_BORDER, "0", ATTR_SRC, path, ATTR_TITLE,
                             title, ATTR_ALT, title) + " " + extra);
        }
        if (isFontAwesome(path)) {
            return faIcon(path, extra);
        }
        String img = tag(TAG_IMG,
                         attrs(ATTR_BORDER, "0", ATTR_SRC, path) + " "
                         + extra, inner);

        return img;
    }

    
    public static boolean isFontAwesome(String icon) {
        if (icon == null) {
            return false;
        }

        return icon.startsWith("fa-") || icon.startsWith("fas ")
               || icon.startsWith("fab ");
    }



    
    public static String faIcon(String icon, String... args) {
        String clazz = (icon.trim().indexOf(" ") >= 0)
                       ? icon
                       : "fas " + icon;

        return span(tag("i", " class='" + clazz + "' " + attrs(args), ""),
                    "");
    }

    
    public static String faIconClass(String icon, String extraClass,
                                     String... args) {
        String clazz = (icon.trim().indexOf(" ") >= 0)
                       ? icon
                       : "fas " + icon;

        return span(tag("i",
                        " class='" + clazz + " " + extraClass + "' "
                        + attrs(args), ""), "");
    }



    
    public static String getIconImage(String url, String... args) {
        if (isFontAwesome(url)) {
            return faIcon(url, args);
        } else {
            return image(url, args);
        }
    }



    
    public static String cssClass(String c) {
        return attr(ATTR_CLASS, c);
    }

    
    public static String clazz(String c) {
        return attr(ATTR_CLASS, c);
    }

    
    public static void clazz(Appendable sb, String c) {
        attr(sb, ATTR_CLASS, c);
    }

    
    public static Appendable cssClass(Appendable sb, String c) {
        attr(sb, ATTR_CLASS, c);

        return sb;
    }

    
    public static String title(String c) {
        return attr(ATTR_TITLE, c);
    }

    
    public static String id(String c) {
        return attr(ATTR_ID, c);
    }


    
    public static Appendable id(Appendable sb, String c) {
        attr(sb, ATTR_ID, c);

        return sb;
    }

    
    public static String style(String... args) {
        if (args.length == 1) {
            return attr(ATTR_STYLE, args[0]);
        }

        return attr(ATTR_STYLE, css(args));
    }

    
    public static String css(String... s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length; i += 2) {
            if (s[i + 1] == null) {
                continue;
            }
            sb.append(s[i]);
            sb.append(":");
            sb.append(s[i + 1]);
            sb.append(";");
        }

        return sb.toString();
    }

    
    public static String bold(String v1) {
        return tag(TAG_B, "", v1);
    }

    
    public static String col(String v1) {
        return col(v1, "");
    }


    
    public static Appendable col(Appendable sb, String v1) {
        col(sb, v1, "");

        return sb;
    }

    
    public static String col(String v1, String attr) {
        return tag(TAG_TD, " " + attr + " ", v1);
    }

    
    public static Appendable col(Appendable sb, String v1, String attr) {
        tag(sb, TAG_TD, " " + attr + " ", v1);

        return sb;
    }




    
    public static String colRight(String v1) {
        return tag(TAG_TD, " " + attr(ATTR_ALIGN, "right") + " ", v1);
    }


    
    public static String highlightable(String c) {
        return span(c, cssClass("ramadda-highlightable"));
    }


    
    public static String span(String content, String extra) {
        return tag(TAG_SPAN, extra, content);
    }

    
    public static Appendable span(Appendable sb, String content,
                                  String extra) {
        tag(sb, TAG_SPAN, extra, content);

        return sb;
    }


    public static boolean debug = false;

    
    public static boolean debug1 = false;

    
    public static boolean debug2 = false;

    
    public static Hashtable parseHtmlProperties(String s) {

        Hashtable properties = new Hashtable();
        if (debug1) {
            System.err.println("Source:" + s);
        }
        // single title=foo full --- name=bar
        s = s.trim();
        int           length             = s.length();
        int           MODE_START         = 0;
        int           MODE_NAME          = 1;
        int           MODE_EQUALS        = 2;
        int           MODE_OPEN_QUOTE    = 3;
        int           MODE_VALUE         = 4;
        int           MODE_VALUE_QUOTE   = 5;
        int           MODE_VALUE_NOQUOTE = 6;
        int           mode               = MODE_START;
        StringBuilder nb                 = new StringBuilder();
        StringBuilder vb                 = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            //        System.err.println(mode +" c:" + c);
            if (mode == MODE_START) {
                if (c == ' ') {
                    continue;
                }
                if (c == '=') {
                    mode = MODE_VALUE;

                    continue;
                }
                nb.append(c);
                if (debug2) {
                    System.err.println("start:" + nb);
                }
                mode = MODE_NAME;

                continue;
            }

            if (mode == MODE_NAME) {
                if ((c == ' ') || (c == '\n')) {
                    mode = MODE_EQUALS;

                    continue;
                }
                if (c == '=') {
                    mode = MODE_VALUE;

                    continue;
                }
                nb.append(c);
                if (debug2) {
                    System.err.println("name:" + nb);
                }

                continue;
            }
            if (mode == MODE_EQUALS) {
                if (c == '=') {
                    mode = MODE_VALUE;

                    continue;
                }
                String name = nb.toString().trim();
                if (name.length() > 0) {
                    if (debug1) {
                        System.err.println("PROP:" + name + "=" + ";");
                    }
                    properties.put(name, "");
                }
                nb = new StringBuilder();
                nb.append(c);
                if (debug2) {
                    System.err.println("=:" + nb);
                }
                mode = MODE_START;

                continue;
            }
            if (mode == MODE_VALUE) {
                if (c == ' ') {
                    continue;
                }
                if (c == '"') {
                    mode = MODE_VALUE_QUOTE;

                    continue;
                }
                mode = MODE_VALUE_NOQUOTE;
                vb.append(c);

                continue;
            }
            if (mode == MODE_VALUE_QUOTE) {
                if (c == '"') {
                    mode = MODE_START;
                    String name = nb.toString().trim();
                    if (name.length() > 0) {
                        if (debug1) {
                            System.err.println("PROP:" + name + "=" + vb
                                    + ";");
                        }
                        properties.put(name.trim(), vb.toString());
                    }
                    nb = new StringBuilder();
                    vb = new StringBuilder();

                    continue;
                }
                vb.append(c);
                if (debug2) {
                    System.err.println("quote:" + vb);
                }

                continue;
            }

            if (mode == MODE_VALUE_NOQUOTE) {
                if ((c == ' ') || (c == '\n')) {
                    mode = MODE_START;
                    String name = nb.toString();
                    if (name.length() > 0) {
                        if (debug1) {
                            System.err.println("PROP:" + name + "=" + vb
                                    + ";");
                        }
                        properties.put(name.trim(), vb.toString());
                    }
                    nb = new StringBuilder();
                    vb = new StringBuilder();

                    continue;
                }
                vb.append(c);
                if (debug2) {
                    System.err.println("no quote:" + vb);
                }

                continue;
            }


        }
        String name = nb.toString();
        if (name.length() > 0) {
            if (debug1) {
                System.err.println("PROP:" + name + "=" + vb + ";");
            }
            properties.put(name.trim(), vb.toString());
        }
        if (debug1) {
            //            System.err.println("props:" + properties);
        }


        return properties;

    }




    
    public static String section(String content) {
        return section(content, null);
    }


    
    public static String section(String content, String title) {
        if (title != null) {
            StringBuilder sb = new StringBuilder();
            open(sb, TAG_DIV, "class", "ramadda-section");
            tag(sb, TAG_DIV, cssClass("ramadda-page-heading"), title);
            sb.append(content);
            close(sb, TAG_DIV);

            return sb.toString();
        }

        return div(content, cssClass("ramadda-section"));

    }

    
    public static String sectionOpen() {
        return open(TAG_DIV, "class", "ramadda-section");
    }

    
    public static String sectionOpen(String label) {
        return sectionOpen(label, false);
    }

    
    public static Appendable titleSectionOpen(Appendable sb, String label)
            throws Exception {
        sectionOpen(sb, null, false);
        sectionTitle(sb, label);

        return sb;
    }

    
    public static String sectionOpen(String label, boolean line) {
        try {
            StringBuilder sb = new StringBuilder();
            sectionOpen(sb, label, line);

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    
    public static String hrow(List cols) throws Exception {
        Appendable sb = new StringBuilder();
        hrow(sb, cols);

        return sb.toString();
    }


    
    public static Appendable hrow(Appendable sb, List cols) throws Exception {
        for (Object o : cols) {
            sb.append(div(o.toString(),
                          attrs("style",
                                "display:inline-block;vertical-align:top;")));
        }

        return sb;
    }

    
    public static Appendable hrow(Appendable sb, String... cols)
            throws Exception {
        for (String o : cols) {
            sb.append(div(o.toString(),
                          attrs("style",
                                "display:inline-block;vertical-align:top;")));
        }

        return sb;
    }

    
    public static String hrow(String... cols) throws Exception {
        StringBuilder sb = new StringBuilder();
        hrow(sb, cols);

        return sb.toString();
    }


    
    public static Appendable sectionOpen(Appendable sb, String label,
                                         boolean line)
            throws Exception {
        open(sb, TAG_DIV, "class", line
                                   ? "ramadda-section"
                                   : "ramadda-section ramadda-section-noline");
        sectionHeader(sb, label);

        return sb;
    }



    
    public static Appendable sectionHeader(Appendable sb, String label)
            throws Exception {
        if (Utils.stringDefined(label)) {
            div(sb, label, cssClass("ramadda-page-heading"));
        }

        return sb;
    }


    
    public static Appendable sectionTitle(Appendable sb, String label)
            throws Exception {
        if (Utils.stringDefined(label)) {
            div(sb, label, cssClass("ramadda-page-title"));
        }

        return sb;
    }



    
    public static String sectionClose() {
        return close(TAG_DIV);
    }


    
    public static String note(String s) {
        return div(div(s, cssClass("ramadda-note")),
                   cssClass("ramadda-note-outer"));
    }


    
    public static String div(String content) {
        return div(content, "");
    }

    
    public static String div(String content, String extra) {
        return tag(TAG_DIV, extra, content);
    }

    public static Appendable inlineBlock(Appendable sb, String content, String extra) {
        tag(sb,TAG_INLINE_BLOCK, extra, content);
	return sb;
    }

    public static String inlineBlock(String content, String extra) {
        return tag(TAG_INLINE_BLOCK, extra, content);
    }    



    
    public static Appendable div(Appendable sb, String content,
                                 String extra) {
        tag(sb, TAG_DIV, extra, content);

        return sb;
    }


    
    public static String td(String content) {
        return td(content, "");
    }

    
    public static String td(String content, String extra) {
        return tag(TAG_TD, extra, content);
    }

    public static String tr(String content) {
	return tr(content,"");
    }

    public static String tr(String content, String extra) {
        return tag(TAG_TR, extra, content);
    }

    
    public static Appendable td(Appendable sb, String content) {
        return td(sb, content, "");
    }

    
    public static Appendable td(Appendable sb, String content, String extra) {
        tag(sb, TAG_TD, extra, content);

        return sb;
    }



    
    public static String th(String content) {
        return th(content, "");
    }

    
    public static String th(String content, String extra) {
        return tag(TAG_TH, extra, content);
    }

    
    public static Appendable th(Appendable sb, String content) {
        return th(sb, content, "");
    }

    
    public static Appendable th(Appendable sb, String content, String extra) {
        tag(sb, TAG_TH, extra, content);

        return sb;
    }



    
    public static String h1(String content) {
        return tag(TAG_H1, "", content);
    }

    
    public static String h2(String content) {
        return tag(TAG_H2, "", content);
    }

    
    public static String h3(String content) {
        return tag(TAG_H3, "", content);
    }


    
    public static String ul() {
        return open(TAG_UL, "");
    }



    
    public static String p(String content) {
        return tag(TAG_P, "", content);
    }



    
    public static String pre(String content) {
        return pre(content, "");
    }

    
    public static void pre(Appendable sb, String content) throws IOException {
        tag(sb, TAG_PRE, "", content);
    }


    
    public static String pre(String content, String attrs) {
        return tag(TAG_PRE, attrs, content);
    }



    
    public static String url(String path, List args) {
        return url(path, Utils.toStringArray(args));
    }


    
    public static String url(String path, String... args) {
        return url(path, args, true);
    }


    
    public static Appendable url(Appendable sb, String path, String... args) {
        url(sb, path, args, true);

        return sb;
    }

    
    public static String url(String path, String[] args, boolean encodeArgs) {
        if (args.length == 0) {
            return path;
        }
        StringBuilder sb = new StringBuilder();
        url(sb, path, args, encodeArgs);

        return sb.toString();
    }

    
    public static String makeDim(String size, String...dflt) {
        if (size == null) {
            return null;
        }
        if ( !size.matches("^[0-9\\.+-]+$")) {
            return size;
        }
        if (dflt.length>0 && dflt[0] != null) {
            return size + dflt[0];
        }

        return size + "px";
    }


    
    public static void url(Appendable sb, String path, String[] args,
                           boolean encodeArgs) {
        try {
            sb.append(path);
            if (args.length == 0) {
                return;
            }
            boolean addAmpersand = false;
            if (path.indexOf("?") >= 0) {
                if ( !path.endsWith("?")) {
                    addAmpersand = true;
                }
            } else {
                sb.append("?");
            }

            for (int i = 0; i < args.length; i += 2) {
                if (addAmpersand) {
                    sb.append("&");
                }
                try {
                    arg(sb, args[i], args[i + 1], encodeArgs);
                } catch (Exception exc) {
                    if (i < args.length) {
                        System.err.println("error encoding arg(1):"
                                           + args[i + 1] + " " + exc);
                    }
                    exc.printStackTrace();

                    throw new RuntimeException(exc);
                }
                addAmpersand = true;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    
    public static String args(String... args) {
        return args(args, true);
    }

    
    public static String args(String[] args, boolean encodeArgs) {
        List<String> a = new ArrayList<String>();
        for (int i = 0; i < args.length; i += 2) {
            a.add(arg(args[i], args[i + 1], encodeArgs));
        }

        return StringUtil.join("&", a);
    }


    
    public static String args(List<String> args, boolean encodeArgs) {
        List<String> a = new ArrayList<String>();
        for (int i = 0; i < args.size(); i += 2) {
            a.add(arg(args.get(i), args.get(i + 1), encodeArgs));
        }

        return StringUtil.join("&", a);
    }


    
    public static String args(Hashtable args) {
        List<String> a = new ArrayList<String>();
        for (java.util.Enumeration keys =
                args.keys(); keys.hasMoreElements(); ) {
            Object key   = keys.nextElement();
            Object value = args.get(key);
            if (value instanceof List) {
                for (Object v : (List) value) {
                    a.add(arg(key.toString(), v.toString()));
                }
            } else {
                a.add(arg(key.toString(), value.toString()));
            }
        }

        return StringUtil.join("&", a);
    }


    
    public static String arg(String name, String value) {
        return arg(name, value, true);
    }

    
    public static String arg(Object name, Object value, boolean encodeArg) {
        Appendable sb = new StringBuilder();
        arg(sb, name, value, encodeArg);

        return sb.toString();
    }


    
    public static void arg(Appendable sb, Object name, Object value,
                           boolean encodeArg) {
        try {
            sb.append(sanitizeArg(name.toString()));
            sb.append("=");
            sb.append((encodeArg
                       ? java.net.URLEncoder.encode(value.toString(), "UTF-8")
                       : value.toString()));
        } catch (Exception exc) {
            System.err.println("error encoding arg(2):" + value + " " + exc);
            exc.printStackTrace();
        }
    }




    
    public static String row(String row) {
        return tag(TAG_TR, "", row);
    }

    
    public static void row(Appendable sb, String row) {
        tag(sb, TAG_TR, "", row);
    }

    
    public static void row(Appendable sb, String row, String extra) {
        tag(sb, TAG_TR, extra, row);
    }



    
    public static String row(String row, String extra) {
        return tag(TAG_TR, extra, row);
    }

    
    public static String rowTop(String row) {
        return tag(TAG_TR, attr(ATTR_VALIGN, VALUE_TOP), row);
    }

    
    public static String rowBottom(String row) {
        return tag(TAG_TR, attr(ATTR_VALIGN, VALUE_BOTTOM), row);
    }


    
    public static String cols(String... cols) {
        StringBuilder sb = new StringBuilder();
        cols(sb, cols);

        return sb.toString();
    }


    
    public static void cols(Appendable sb, String... cols) {
        try {
            for (int i = 0; i < cols.length; i++) {
                sb.append(tag(TAG_TD, "", cols[i]));
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    
    public static void thead(Appendable sb, Object... cols) throws Exception {
        sb.append("<thead>");
        sb.append("<tr>");
        for (int i = 0; i < cols.length; i++) {
            sb.append(tag(TAG_TH, "", cols[i].toString()));
        }
        sb.append("</tr>");
        sb.append("</thead>");
    }




    
    public static String headerCols(Object[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            sb.append(cols(HtmlUtils.b(columns[i].toString())));
        }

        return sb.toString();
    }



    
    public static String cols(Object[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            sb.append(cols(columns[i].toString()));
        }

        return sb.toString();
    }



    
    public static String makeLatLonInput(String id, String arg,
                                         String value) {
        return makeLatLonInput(id, arg, value, null, "");
    }


    
    public static String makeLatLonInput(String id, String arg, String value,
                                         String tip, String extra) {
        return input(arg, value,
                     id(id) + style("margin:0px;") + attrs(ATTR_SIZE, "5")
                     + id(arg) + extra + ((tip != null)
                                          ? title(tip)
                                          : ""));
    }


    
    public static String makeLatLonBox(String baseId, String baseName,
                                       String southValue, String northValue,
                                       String eastValue, String westValue) {


        return makeLatLonBox(baseId, baseName + SUFFIX_SOUTH,
                             baseName + SUFFIX_NORTH, baseName + SUFFIX_EAST,
                             baseName + SUFFIX_WEST, southValue, northValue,
                             eastValue, westValue);
    }

    
    public static String makeLatLonBox(String baseId, String southArg,
                                       String northArg, String eastArg,
                                       String westArg, String southValue,
                                       String northValue, String eastValue,
                                       String westValue) {
        return "<table border=0 cellspacing=0 cellpadding=0><tr><td colspan=\"2\" align=\"center\">"
               + makeLatLonInput(baseId + SUFFIX_NORTH, northArg, northValue,
                   "North", " data-dir=north ") + "</td></tr>" + "<tr><td>"
                       + makeLatLonInput(baseId + SUFFIX_WEST, westArg,
                           westValue, "West",
                               " data-dir=west ") + "</td><td>"
                                   + makeLatLonInput(baseId + SUFFIX_EAST,
                                       eastArg, eastValue, "East",
                                           " data-dir=east ") + "</tr>"
                                               + "<tr><td colspan=\"2\" align=\"center\">"
                                                   + makeLatLonInput(baseId
                                                       + SUFFIX_SOUTH, southArg, southValue, "South", " data-dir=south ") + "</table>";
    }

    
    public static String makeLatLonBox(String baseId, String baseName,
                                       double south, double north,
                                       double east, double west) {
        return makeLatLonBox(baseId, baseName, toString(south),
                             toString(north), toString(east), toString(west));
    }

    
    private static String toString(double v) {
        if (v == v) {
            return "" + v;
        }

        return "";
    }

    
    public static String makeAreaLabel(double south, double north,
                                       double east, double west) {
        return table("<tr><td colspan=\"2\" align=\"center\">"
                     + toString(north) + "</td></tr>" + "<tr><td>"
                     + toString(west) + "</td><td>" + toString(east)
                     + "</tr>" + "<tr><td colspan=\"2\" align=\"center\">"
                     + toString(south));
    }


    
    public static String checkbox(String name) {
        return checkbox(name, VALUE_TRUE, false);
    }

    
    public static String checkbox(String name, String value) {
        return checkbox(name, value, false);
    }

    
    public static String radio(String name, String value, boolean checked) {
        return radio(name, value, checked, "");
    }

    
    public static String radio(String name, String value, boolean checked,
                               String attrs) {
        return tag(TAG_INPUT,
                   attrs
                   + attrs( /*ATTR_CLASS, CLASS_RADIO,*/ATTR_TYPE,
                       TYPE_RADIO, ATTR_NAME, name, ATTR_VALUE,
                       value) + (checked
                                 ? " checked "
                                 : ""));
    }

    
    public static String labeledRadio(String name, String value,
                                      boolean checked, String label) {
	String radio = tag(TAG_INPUT, attrs(ATTR_TYPE, TYPE_RADIO, ATTR_NAME, name, ATTR_VALUE,
					    value) + (checked
						      ? " checked "
						      : ""));
        return tag("label","",radio+ "&nbsp;"+ label);
    }


    
    public static String checkbox(String name, String value,
                                  boolean checked) {
        return checkbox(name, value, checked, "");
    }


    
    public static String labeledCheckbox(String name, String value,
                                         boolean checked, String label) {
        return labeledCheckbox(name, value, checked, "", label);
    }


    public static String labeledCheckbox(String name, String value,
                                         boolean checked, String attrs,
                                         String label) {
	StringBuilder sb = new StringBuilder();
	labeledCheckbox(sb,name,value,checked,attrs,label);
	return sb.toString();
    }


    
    public static void labeledCheckbox(StringBuilder sb, String name, String value,
				       boolean checked, String attrs,
				       String label) {
        String id = null;
        if (attrs == null) {
            attrs = "";
        }
        for (String pattern : new String[] { "id *= *\"([^\"]+)\"",
                                             "id *= *'([^']+)'" }) {
            id = StringUtil.findPattern(attrs, "(?i)" + pattern);
            if (id != null) {
                break;
            }
        }
        if (id == null) {
            id    = getUniqueId("cbx");
            attrs += " " + HtmlUtils.id(id);
        }

        sb.append(checkbox(name, value, checked, attrs));
	tag(sb, "label",
	    style("margin-left:5px;")+
	    cssClass("ramadda-clickable")
	    + attr("for", id), label);
    }

    
    public static String checkbox(String name, String value, boolean checked,
                                  String extra) {
        StringBuilder sb = new StringBuilder();
        checkbox(sb, name, value, checked, extra);

        return sb.toString();
    }


    
    public static void checkbox(Appendable sb, String name, String value,
                                boolean checked, String extra) {
        try {
            dangleOpen(sb, TAG_INPUT);
            sb.append(" ");
            if (extra.length() > 0) {
                sb.append(extra);
            }

            attrs(sb, ATTR_TYPE, TYPE_CHECKBOX, ATTR_NAME, name, ATTR_VALUE,
                  value);
            if (checked) {
                sb.append(" checked='checked' ");
            }
            sb.append(">");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    
    public static String formHelp(String html) {
	return span(html,clazz("ramadda-form-help"));

    }
    public static String formHelp(String html,boolean inForm) {	
	html = formHelp(html);
	if(inForm) return formEntry("",html);
	return html;
    }


    public static String form(String url) {
        return form(url, "");
    }

    
    public static String form(String url, String extra) {
        return open(TAG_FORM, attr(ATTR_ACTION, url) + " " + extra);
    }


    
    public static void form(Appendable sb, String url) throws Exception {
        open(sb, TAG_FORM, attr(ATTR_ACTION, url));
    }

    
    public static String formPost(String url) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url));
    }

    
    public static String formPost(String url, String extra) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url) + " "
                    + extra);
    }


    
    public static String uploadForm(String url, String extra) {
        return open(TAG_FORM,
                    " accept-charset=\"UTF-8\" "
                    + attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url,
                            ATTR_ENCTYPE, VALUE_MULTIPART) + " " + extra);
    }


    public static String href(String url) {
	return open("a",attrs("href",url));
    }


    
    public static String href(String url, String label) {
        return tag(TAG_A, attrs(ATTR_HREF, url), label);
    }

    
    public static void href(Appendable sb, String url, String label) {
        tag(sb, TAG_A, attrs(ATTR_HREF, url), label);
    }

    
    public static String href(String url, String label, String extra) {
        return tag(TAG_A, Utils.concatString(attrs(ATTR_HREF, url), extra),
                   label);
    }


    
    public static void href(Appendable sb, String url, String label,
                            String extra) {
        tag(sb, TAG_A, Utils.concatString(attrs(ATTR_HREF, url), extra),
            label);
    }


    
    public static String submitImage(String img, String name) {
        return submitImage(img, name, "", "");
    }


    
    public static String submitImage(String img, String name, String alt,
                                     String extra) {
        if (isFontAwesome(img)) {
            return open("button",
                        "type='submit' " + extra
                        + attr(ATTR_TITLE, alt)) + getIconImage(img)
                            + "</button>";
        }

        return tag(TAG_INPUT,
                   extra
                   + attrs(ATTR_NAME, name, ATTR_BORDER, "0", ATTR_SRC, img,
                           ATTR_VALUE, name) + attrs(ATTR_CLASS,
                               CLASS_SUBMITIMAGE, ATTR_TITLE, alt, ATTR_ALT,
                               alt, ATTR_TYPE, TYPE_IMAGE));
    }



    
    public static String submit(String label, String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_TYPE, TYPE_SUBMIT, ATTR_VALUE,
                         label, ATTR_CLASS, CLASS_SUBMIT));
    }

    
    public static String submit(String label, String name, String extra) {
        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_TYPE, TYPE_SUBMIT, ATTR_VALUE,
                         label, ATTR_CLASS, CLASS_SUBMIT) + extra);
    }

    
    public static String submit(String label) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_SUBMIT, ATTR_TYPE, TYPE_SUBMIT,
                         ATTR_VALUE, label));
    }

    
    public static String textArea(String name, String value, int rows,
                                  int columns) {
        return textArea(name, value, rows, columns, "");
    }

    
    public static String textArea(String name, String value, int rows,
                                  String attr) {
        return textArea(name, value, rows, 0, attr);
    }


    
    public static String textArea(String name, String value, int rows,
                                  int columns, String extra) {
        String width = (columns > 0)
                       ? attr(ATTR_COLS, "" + columns)
                       : style("width:100%");

        value = value.replaceAll("&", "&amp;");

        return tag(TAG_TEXTAREA,
                   attrs(ATTR_NAME, name/*, ATTR_CLASS, CLASS_TEXTAREA*/)
                   + attrs(ATTR_ROWS, "" + rows) + width + extra, value);
    }

    
    public static String password(String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_PASSWORD, ATTR_TYPE,
                         TYPE_PASSWORD, ATTR_NAME, name));
    }

    
    public static String password(String name, String value, String extra) {
        return tag(TAG_INPUT,
                   extra
                   + attrs(ATTR_VALUE, value, ATTR_CLASS, CLASS_PASSWORD,
                           ATTR_TYPE, TYPE_PASSWORD, ATTR_NAME, name));
    }




    
    public static String input(String name) {
        return input(name, null, "");
    }

    
    public static String input(String name, Object value) {
        return input(name, value, "");
    }

    
    public static String input(String name, Object value, int size) {
        return input(name, value, attrs(ATTR_SIZE, "" + size));
    }

    
    public static String input(String name, Object value, int size,
                               String extra) {
        return input(name, value, attrs(ATTR_SIZE, "" + size) + " " + extra);
    }

    
    public static String input(String name, Object value, String extra) {
        if ((extra == null) || (extra.length() == 0)) {
            return tag(TAG_INPUT,
                       attrs(ATTR_CLASS, CLASS_INPUT, ATTR_NAME, name,
                             ATTR_VALUE, ((value == null)
                                          ? ""
                                          : value.toString())) + " " + extra);
        }
        if (extra.indexOf("class=") >= 0) {
            return tag(TAG_INPUT,
                       attrs(ATTR_NAME, name, ATTR_VALUE, ((value == null)
                    ? ""
                    : value.toString())) + " " + extra);

        }

        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_CLASS, CLASS_INPUT,
                         ATTR_VALUE, ((value == null)
                                      ? ""
                                      : value.toString())) + " " + extra);
    }


    
    public static String disabledInput(String name, Object value,
                                       String extra) {
        String classAttr = "";
        if (extra.indexOf("class=") < 0) {
            classAttr = cssClass(CLASS_DISABLEDINPUT);
        }

        return tag(TAG_INPUT,
                   " " + ATTR_READONLY + " "
                   + attrs(ATTR_NAME, name, ATTR_VALUE, ((value == null)
                ? ""
                : value.toString())) + " " + extra + classAttr);
    }


    
    public static String fileInput(String name, String extra) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_FILEINPUT, ATTR_TYPE, TYPE_FILE,
                         ATTR_NAME, name) + " " + extra + " " + SIZE_70);
    }

    
    public static String select(String name, List values) {
        return select(name, values, null);
    }

    
    public static String select(String name, List values, String selected) {
        return select(name, values, selected, Integer.MAX_VALUE);
    }

    
    public static String select(String name, List values, String selected,
                                String extra) {
        return select(name, values, selected, extra, Integer.MAX_VALUE);
    }


    
    public static String select(String name, List values,
                                List<String> selected, String extra) {
        return select(name, values, selected, extra, Integer.MAX_VALUE);
    }





    
    public static String select(String name, Object[] values,
                                String selected, int maxLength) {
        return select(name, Misc.toList(values), selected, maxLength);
    }


    
    public static String select(String name, List values, String selected,
                                int maxLength) {
        return select(name, values, selected, "", maxLength);
    }


    
    public static String select(String name, List values, String selected,
                                String extra, int maxLength) {
        List selectedList = null;
        //        if ((selected != null) && (selected.length() > 0)) {
        if (selected != null) {
            selectedList = Misc.newList(selected);
        }

        return select(name, values, selectedList, extra, maxLength);
    }

    
    public static class Selector {
        int margin = 3;
        int padding = 20;
        String label;
        String id;
        String icon;
        boolean isHeader = false;
        private String attr;
        private String tooltip;

        public Selector(String labelId) {
	    this(Utils.split(labelId,":").get(0),
		 Utils.split(labelId,":").get(1));
	}

        public Selector(String label, String id) {
            this(label, id, null, 0, false);
        }

        public Selector(String label, String id, String icon) {
            this(label, id, icon, 0, false);
        }

        public Selector(String label, String id, String icon, int margin) {
            this(label, id, icon, margin, false);
        }

        public Selector(String label, String id, String icon, int margin, boolean isHeader) {
            this(label, id, icon, margin, 20, isHeader);
        }


        public Selector(String label, String id, String icon, int margin, int padding, boolean isHeader) {
            this(label, id, null, icon, margin, padding, isHeader);
        }

        public Selector(String label, String id, String tooltip, String icon,
                        int margin, int padding, boolean isHeader) {
            this.label    = label;
            this.id       = id;
            this.tooltip  = tooltip;
            this.icon     = icon;
            this.margin   = margin;
            this.padding  = padding;
            this.isHeader = isHeader;
        }


	public static Selector findId(Object id, List l) {
	    for (int i = 0; i < l.size(); i++) {
		Selector tfo = (Selector) l.get(i);
		if (Misc.equals(id, tfo.getId())) {
		    return tfo;
		}
	    }
	    return null;
	}

	public boolean equals(Object o) {
	    if(!(o instanceof Selector)) return false;
	    return this.id.equals(((Selector)o).id);
	}

	public static boolean contains(List<Selector> l, Object v) {
	    String s= (v instanceof Selector)?((Selector)v).id:v.toString();
	    for(Selector sel: l)  {
		if(sel.id.equals(s)) return true;
	    }
	    return false;
	}
        
        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        
        public String toString() {
            return "id:" + id +" label:"+this.label+":";
        }

        
        public void setAttr(String s) {
            attr = s;
        }

        
        public String getId() {
            return id;
        }

        
        public String getLabel() {
            return label;
        }

        
        public String getIcon() {
            return icon;
        }

    }

    
    public static String select(String name, List values, List selected,
                                String extra, int maxLength) {
        try {
            StringBuilder sb = new StringBuilder();
            select(sb, name, values, selected, extra, maxLength);
            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    
    public static void select(Appendable sb, String name, List values,
                              List selected, String extra, int maxLength)
            throws Exception {

        String attrs = "";
        if (extra == null) {
            extra = "";
        }
	boolean decorated = false;
        if (extra.indexOf(ATTR_CLASS) >= 0) {
            attrs = attrs(ATTR_NAME, name);
        } else {
            String cssClass = null;
            if (extra.indexOf(ATTR_ROWS) >= 0) {
                cssClass = "ramadda-multiselect";
            } else if ((values != null) && !values.isEmpty()
                       && (values.get(0) instanceof HtmlUtils.Selector)) {
                cssClass = "ramadda-pulldown-with-icons";
		decorated = true;
            } else {
                cssClass = "ramadda-pulldown";
            }
            attrs = attrs(ATTR_NAME, name, ATTR_CLASS, cssClass);
        }
        sb.append(open(TAG_SELECT, attrs + extra));
        sb.append("\n");

        HashSet seenSelected = new HashSet();
        for (int i = 0; i < values.size(); i++) {
            Object obj = values.get(i);
            String value;
            String label;
            String tooltip   = null;
            String extraAttr = null;
            if (obj instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) obj;
                value = tfo.getId().toString();
                label = tfo.toString();
		tooltip = label +" ("+ value+")";

            } else if (obj instanceof Selector) {
                Selector selector = (Selector) obj;
                tooltip = selector.tooltip;
                value   = selector.id;
                label   = selector.label;
                if (selector.attr != null) {
                    extraAttr = selector.attr;
                }
                if (Utils.stringDefined(selector.icon)) {
                    String style = "";
                    if ( !selector.isHeader) {
                        style += "margin-left:" + selector.margin + "px;";
                    }
                    extraAttr = attrs("data-class", "ramadda-select-icon",
                                      "data-style", style, "img-src",  selector.icon);
                    if (selector.isHeader) {
                        extraAttr = attrs("isheader", "true", "label-class",
                                          "ramadda-select-header");
                    }
                } else if (selector.isHeader) {
		    //                    extraAttr = style("font-weight:bold;background: #ddd;padding:6px;");
		    extraAttr = attrs("isheader", "true", "label-class",
				      "ramadda-select-header");
                } else {
		    if(!decorated && selector.margin>0 ) {
                        String style = "margin-left:" + selector.margin + "px;";
			String prefix="";
			for(int j=0;j<selector.margin;j++) {
			    prefix+= "&nbsp;";
			}			    
			label = prefix+label;
		    }
		}
            } else {
                value = label = obj.toString();
            }
            if (label.equals("hr")) {
                sb.append(hr());
                continue;
            }
            sb.append("<option ");
	    //	    System.out.println(label+" " + extraAttr);

            if (extraAttr != null) {
                sb.append(" ");
                sb.append(extraAttr);
                sb.append(" ");
            }

            sb.append(HtmlUtils.attr("title", (tooltip != null)
                    ? tooltip
                    : label));
            if (label.length() > maxLength) {
                label = label.substring(0, maxLength) + "...";
            }

            if ((selected != null)
                    && (selected.contains(value) || selected.contains(obj))) {
                if ( !seenSelected.contains(value)) {
                    sb.append(" selected ");
                    seenSelected.add(value);
                }
            }
	    attr(sb, ATTR_VALUE, value);
            sb.append(">");
            sb.append(label);
            sb.append("</option>");
	    //	    if(debug) System.err.println("SEL: value:" + value+" label:" + label);
        }
        sb.append("</select>");
        sb.append("\n");
    }

    
    public static String checkboxSelect(String name, List values,
                                        List selected, String boxStyle,
                                        String extra)
            throws Exception {
        try {
            StringBuilder sb = new StringBuilder();
            checkboxSelect(sb, name, values, selected, boxStyle, extra);

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    
    public static void checkboxSelect(Appendable sb, String name,
                                      List values, List selected,
                                      String boxStyle, String extra)
            throws Exception {
        sb.append(open(TAG_DIV, HtmlUtils.style(boxStyle) + HtmlUtils.attr("checkboxname",name)+extra));
        sb.append("\n");

        HashSet seenSelected = new HashSet();
        for (int i = 0; i < values.size(); i++) {
            String attrs = "";
            Object obj   = values.get(i);
            String value;
            String label;
            String tooltip   = null;
            String extraAttr = null;
            if (obj instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) obj;
                value = tfo.getId().toString();
                label = tfo.toString();
            } else if (obj instanceof Selector) {
                Selector selector = (Selector) obj;
                tooltip = selector.tooltip;
                value   = selector.id;
                label   = selector.label;
                if (selector.attr != null) {
                    extraAttr = selector.attr;
                }
                if (Utils.stringDefined(selector.icon)) {
                    String style = "";
                    if ( !selector.isHeader) {
                        style += "margin-left:" + selector.margin + "px;";
                    }
                    extraAttr = attrs("data-class", "ramadda-select-icon",
                                      "data-style", style, "img-src",
                                      selector.icon);
                    if (selector.isHeader) {
                        extraAttr = attrs("isheader", "true", "label-class",
                                          "ramadda-select-header");
                    }
                } else if (selector.isHeader) {
                    extraAttr = style(
                        "font-weight:bold;background: #ddd;padding:6px;");
                }
            } else {
                value = label = obj.toString();
            }
            if (label.equals("hr")) {
                sb.append(hr());
                continue;
            }


            boolean isSelected = false;
            if ((selected != null)
                    && (selected.contains(value) || selected.contains(obj))) {
                if ( !seenSelected.contains(value)) {
                    isSelected = true;
                }
            }

            if (tooltip != null) {
                attrs += HtmlUtils.attr("title", tooltip);
            }
            sb.append(div(labeledCheckbox(name, value, isSelected, "",
                                          label), attrs));
        }
        sb.append("</div>");
        sb.append("\n");
    }




    
    public static String colorSelect(String name, String selected) {
        StringBuilder sb = new StringBuilder();
        sb.append(open(TAG_SELECT,
                       attrs(ATTR_NAME, name, ATTR_CLASS, CLASS_SELECT)));
        String value;
        value = "none";
        sb.append(tag(TAG_OPTION, attrs(ATTR_TITLE, value, ATTR_VALUE, ""),
                      value));

        for (int i = 0; i < COLORS.length; i++) {
            Color  c     = COLORS[i];
            String label = COLORNAMES[i];
            value = StringUtil.toHexString(c);
            value = value.replace("#", "");
            String selectedAttr = "";
            if (Misc.equals(value, selected)) {
                selectedAttr = attrs(ATTR_SELECTED, VALUE_SELECTED);
            }
            String textColor = "";
            if (c.equals(Color.black)) {
                textColor = "color:#FFFFFF;";
            }
            sb.append(tag(TAG_OPTION,
                          selectedAttr
                          + attrs(ATTR_TITLE, value, ATTR_VALUE, value,
                                  ATTR_STYLE,
                                  "background-color:" + value + ";"
                                  + textColor), label));
        }
        sb.append(close(TAG_SELECT));

        return sb.toString();
    }


    
    public static String inset(Object html, int top, int left, int bottom,
                               int right) {
        return span(html.toString(), style(insetStyle(top, left, bottom, right)));
    }


    
    public static String insetDiv(Object html, int top, int left, int bottom,
                                  int right) {
        return div(html.toString(), style(insetStyle(top, left, bottom, right)));
    }

    
    public static String openInset(int top, int left, int bottom, int right) {
        return open("div", style(insetStyle(top, left, bottom, right)));
    }


    
    public static String insetStyle(int top, int left, int bottom,
                                    int right) {
        return ((top == 0)
                ? ""
                : "margin-top:" + top + "px;") + ((left == 0)
                ? ""
                : "margin-left:" + left + "px;") + ((bottom == 0)
                ? ""
                : "margin-bottom:" + bottom + "px;") + ((right == 0)
                ? ""
                : "margin-right:" + right + "px;");
    }


    
    public static String beginInset(int top, int left, int bottom,
                                    int right) {
        return open(TAG_DIV, style(((top == 0)
                                    ? ""
                                    : "margin-top:" + top
                                      + "px;") + ((left == 0)
                ? ""
                : "margin-left:" + left + "px;") + ((bottom == 0)
                ? ""
                : "margin-bottom:" + bottom + "px;") + ((right == 0)
                ? ""
                : "margin-right:" + right + "px;")));
    }

    
    public static String endInset() {
        return close(TAG_DIV);
    }


    
    public static String inset(String html, int space) {
        return div(html, style("margin:" + space + "px;"));
    }



    
    public static String insetLeft(String html, int space) {
        return div(html, style("margin-left:" + space + "px;"));
    }

    
    public static String colspan(String s, int cols) {
        return tag(TAG_TD, attr(ATTR_COLSPAN, "" + cols), s);
    }

    
    public static String formTableTop(String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(formTable());

        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntryTop(cols[i], cols[i + 1]));
        }
        sb.append(formTableClose());

        return sb.toString();
    }

    
    public static String formTable(String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(formTable());
        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntry(cols[i], cols[i + 1]));
        }
        sb.append(formTableClose());

        return sb.toString();
    }


    
    public static String formEntryTop(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntryTop(cols[i], cols[i + 1]));
        }

        return sb.toString();
    }


    
    public static String leftRight(String left, String right) {
        return leftRight(left, right, "");
    }

    
    public static String leftRight(String left, String right, String attrs) {
        return tag(TAG_TABLE,
                   attrs(ATTR_CLASS, "left_right_table", ATTR_WIDTH, "100%",
                         ATTR_CELLPADDING, "0", ATTR_CELLSPACING,
                         "0") + attrs, row(col(left)
                         + col(right,
                               attr(ATTR_ALIGN,
                                    VALUE_RIGHT)), attr(ATTR_VALIGN,
                                        VALUE_TOP)));
    }


    
    public static String leftRightBottom(String left, String right,
                                         String attrs) {
        return tag(TAG_TABLE,
                   attrs(ATTR_WIDTH, "100%", ATTR_CELLPADDING, "0",
                         ATTR_CELLSPACING,
                         "0") + attrs, row(col(left)
                         + col(right,
                               attr(ATTR_ALIGN,
                                    VALUE_RIGHT)), attr(ATTR_VALIGN,
                                        VALUE_BOTTOM)));
    }

    
    public static String table(String contents) {
        return table(contents, 0, 0);
    }

    
    public static String table(String contents, int padding, int spacing) {
        return table(contents,
                     attrs(ATTR_CELLPADDING, "" + padding, ATTR_CELLSPACING,
                           "" + spacing));
    }

    
    public static String table(String contents, String attrs) {
        return tag(TAG_TABLE, attrs, contents);
    }

    
    public static String table(Object[] columns) {
        return table(columns, "");
    }


    
    public static String table(Object[] columns, String attrs) {
        return table(row(cols(columns), ""), attrs);
    }


    
    public static String table(Object[] columns, int spacing) {
        return table(row(cols(columns), "" /*attr(ATTR_VALIGN, VALUE_TOP)*/),
                     attrs(ATTR_CELLSPACING, "" + spacing));
    }


    
    public static StringBuilder table(List columns, int numCols,
                                      String attributes) {
        if (attributes == null) {
            attributes = attrs(ATTR_CELLPADDING, "0", ATTR_CELLSPACING, "0");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(open(TAG_TABLE, attributes));
        int cols  = 0;
        int width = (int) (100.0 / (double) numCols);
        for (int i = 0; i < columns.size(); i++) {
            if (cols == 0) {
                if (i >= 1) {
                    sb.append(close(TAG_TR));
                }
                sb.append(open(TAG_TR, "valign=top"));
            }
            sb.append(col(columns.get(i).toString(), "width=" + width + "%"));
            cols++;
            if (cols >= numCols) {
                cols = 0;
            }
        }
        sb.append(close(TAG_TR));
        sb.append(close(TAG_TABLE));

        return sb;
    }


    
    public static String openInset() {
        return open(TAG_DIV, cssClass("inset"));
    }

    
    public static String closeInset() {
        return close(TAG_DIV);
    }



    
    public static String formTable() {
        return formTable((String) null);
    }

    
    public static String formTable(boolean fullWidth) {
        return formTable((String) null, fullWidth);
    }

    
    public static String formTable(String clazz) {
        return formTable(clazz, false);
    }

    
    public static String formTable(String clazz, boolean fullWidth) {
        return open(TAG_TABLE, (fullWidth
                                ? (style("width", "100%")
                                   + attr("width", "100%"))
                                : "") + cssClass(" formtable "
                                + ((clazz != null)
                                   ? clazz
                                   : "") + (fullWidth
                                            ? " formtable-fullwidth "
                                            : "")) + attrs(ATTR_CELLPADDING,
                                            "0", ATTR_CELLSPACING, "0"));
    }




    
    public static String formTableClose() {
        return close(TAG_TABLE);
    }

    
    public static void formTableClose(Appendable sb) {
        close(sb, TAG_TABLE);
    }

    
    public static String formClose() {
        return close(TAG_FORM);
    }


    
    public static String formEntry(String left, String right) {
        StringBuilder sb = new StringBuilder();
        formEntry(sb, left, right);

        return sb.toString();
    }

    
    public static void formEntry(Appendable sb, String left, String right) {
        try {
            sb.append(tag(TAG_TR, "",
                    tag(TAG_TD,
                        attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                            CLASS_FORMLABEL), left) + tag(TAG_TD,
                                attrs(ATTR_CLASS, CLASS_FORMCONTENTS),
                                    right)) + "\n");
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }


    }

    
    public static void formEntry(Appendable sb, String left) {
        try {
            sb.append(tag(TAG_TR, "",
                          tag(TAG_TD,
                              attrs("colspan", "2", ATTR_CLASS,
                                    CLASS_FORMCONTENTS), left)));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    
    public static void formEntries(Appendable sb, Object... args)
            throws Exception {
        for (int i = 0; i < args.length; i += 2) {
            sb.append(formEntry(args[i].toString(), args[i + 1].toString()));
        }

    }



    
    public static String formEntry(String left, String right,
                                   int rightColSpan) {
        return tag(TAG_TR, "",
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                             CLASS_FORMLABEL), left) + tag(TAG_TD,
                                 attr("colspan", "" + rightColSpan), right));

    }


    
    public static String formEntryTop(String left, String right,
                                      int rightColSpan) {
        return tag(TAG_TR, "",
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                             CLASS_FORMLABEL_TOP), left) + tag(TAG_TD,
                                 attr("colspan", "" + rightColSpan), right));

    }

    
    public static String formEntry(String left, String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(tag(TAG_TD,
                      attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                            CLASS_FORMLABEL), left));
        String clazz = attrs(ATTR_CLASS, CLASS_FORMCONTENTS);
        for (String col : cols) {
            sb.append(tag(TAG_TD, clazz, col));
        }

        return tag(TAG_TR, "", sb.toString());
    }

    public static String formEntryTop(String left, String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(tag(TAG_TD,
                      attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                            CLASS_FORMLABEL), left));
        String clazz = attrs(ATTR_CLASS, CLASS_FORMCONTENTS);
        for (String col : cols) {
            sb.append(tag(TAG_TD, clazz, col));
        }

        return tag(TAG_TR, "valign=top", sb.toString());
    }


    
    public static String formEntryTop(String left, String right) {
        return formEntryTop(left, right, "", true);
    }


    
    public static String formEntryTop(String left, String right,
                                      String trExtra, boolean dummy) {
        left = div(left, cssClass(CLASS_FORMLABEL_TOP));
        String label = tag(TAG_TD,
                           attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                                 CLASS_FORMLABEL), left);

        // attrs(ATTR_VALIGN, VALUE_TOP)
        return tag(TAG_TR, trExtra, label + tag(TAG_TD, "", right));
    }

    
    public static String formEntryTop(String label, String col1,
                                      String col2) {
        return tag(
            TAG_TR, attrs(ATTR_VALIGN, VALUE_TOP),
            col(
            label,
            attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                CLASS_FORMLABEL_TOP)) + "\n"
                    + col(col1,
                        "" /*attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS, CLASS_FORMLABEL_TOP)*/) + "\n" + col(col2));
    }


    
    public static String attr(String name, String value) {
        StringBuilder sb = new StringBuilder();
        attr(sb, name, value);

        return sb.toString();
    }

    
    public static void attr(Appendable sb, String name, String value) {
        try {
            if (value == null) {
                return;
            }
            sb.append(" ");
            sb.append(name);
            sb.append("=");
            quote(sb, value);
            sb.append(" ");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    
    public static String attrs(String... args) {
        StringBuilder sb = new StringBuilder();
        attrs(sb, args);

        return sb.toString();
    }

    
    public static void attrs(Appendable sb, String... args) {
        if (args.length == 1) {
            try {
                sb.append(args[0]);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            return;
        }
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].length() > 0) {
                attr(sb, args[i], args[i + 1]);
            }
        }
    }


    
    public static String onMouseOver(String call) {
        return attrs(ATTR_ONMOUSEOVER, call);
    }

    
    public static String onMouseMove(String call) {
        return attrs(ATTR_ONMOUSEMOVE, call);
    }


    
    public static String onMouseOut(String call) {
        return attrs(ATTR_ONMOUSEOUT, call);
    }

    
    public static String onMouseUp(String call) {
        return attrs(ATTR_ONMOUSEUP, call);
    }

    
    public static String onMouseDown(String call) {
        return attrs(ATTR_ONMOUSEDOWN, call);
    }


    
    public static void onMouseOut(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEOUT, call);
    }


    
    public static void onMouseDown(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEDOWN, call);
    }


    
    public static void onMouseUp(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEUP, call);
    }


    
    public static void onMouseOver(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEOVER, call);
    }


    
    public static void onMouseClick(Appendable sb, String call) {
        try {
            call = call.replaceAll("\"", "&quot;").replaceAll("\n", " ");
            attrs(sb, ATTR_ONCLICK, call);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    
    public static String onMouseClick(String call) {
        call = call.replaceAll("\"", "&quot;").replaceAll("\n", " ");

        return attrs(ATTR_ONCLICK, call);
    }



    
    public static String mouseClickHref(String call, String label) {
        return mouseClickHref(call, label, "");
    }

    
    public static String mouseClickHref(String call, String label,
                                        String extra) {
        String result = tag(TAG_A,
			    //			    attrs(ATTR_HREF, "javascript:void(0);")
                            onMouseClick(call) + extra, label);
        return result;
    }


    
    public static void mouseClickHref(Appendable sb, String call,
                                      String label, String extra)
            throws Exception {
        tag(sb, TAG_A,
	    //            attrs(ATTR_HREF, "#") + 
	    onMouseClick(call)
            + extra, label);
    }



    
    public static String jsLink(String events, String content) {
        return jsLink(events, content, "");
    }


    
    public static String jsLink(String events, String content, String extra) {
        return tag(TAG_A,
                   attrs(ATTR_HREF, "javascript:noop();") + " " + events
                   + " " + extra, content);
    }


    
    public static String anchorName(String name) {
        return tag(TAG_A, attrs(ATTR_NAME, name), "");
    }


    
    public static String script(String s) {
        StringBuilder js = new StringBuilder();
        script(js, s);
        return js.toString();
    }

    
    public static void script(Appendable js, String s) {
        s = s.trim();
        if (s.length() == 0) {
            return;
        }
        try {
	    js.append("\n");
            js.append(tag(TAG_SCRIPT, attrs(ATTR_TYPE, "text/JavaScript"), s));
	    js.append("\n");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }


    
    public static void commentJS(Appendable js, String... args) {
        try {
            js.append("\n");
            if (args.length == 1) {
                Utils.append(js, "//", args[0]);
            } else {
                js.append("/*");
                for (String s : args) {
                    Utils.append(js, "*", s, "\n");
                }
                js.append("*/");
            }
            js.append("\n");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }






    
    public static String call(String function, String... args) {
        StringBuilder sb = new StringBuilder(function);
        call(sb, function, args);
        return sb.toString();
    }

    
    public static String call(Appendable sb, String function,
                              String... args) {
        try {
            sb.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(args[i]);
            }
            sb.append(");");

            return sb.toString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }



    
    public static String callln(String function, String args) {
        return Utils.concatString(function, "(", args, ");\n");
    }


    
    public static void callln(Appendable sb, String function, String args) {
        Utils.concatBuff(sb, function, "(", args, ");\n");
    }

    
    public static String importJS(String jsUrl) {
        StringBuilder sb = new StringBuilder("\n");
        importJS(sb, jsUrl);

        return sb.toString();
    }

    
    public static void importJS(Appendable sb, String ...urls) {
	for(String jsUrl: urls) {
	    tag(sb, TAG_SCRIPT,
		attrs(ATTR_SRC, jsUrl, ATTR_TYPE, "text/JavaScript",ATTR_REL,"nofollow"), "");
	}
    }

    
    public static void importJS(Appendable sb, String jsUrl,
                                String integrity) {
        if (integrity == null) {
            importJS(sb, jsUrl);

            return;
        }
        tag(sb, TAG_SCRIPT,
            attrs(ATTR_SRC, jsUrl, ATTR_TYPE, "text/JavaScript",
                  "referrerpolicy", "no-referrer", "integrity", integrity,
                  "crossorigin", "anonymous"), "");
    }


    
    public static String cssLink(String url) {
        return tag(TAG_LINK,
                   attrs(ATTR_HREF, url, ATTR_REL, "stylesheet", ATTR_TYPE,
                         "text/css"));
    }

    
    public static void cssLink(Appendable sb, String ...urls) throws IOException {
	for(String url:urls) {
	    tag(sb, TAG_LINK,
		attrs(ATTR_HREF, url, ATTR_REL, "stylesheet", ATTR_TYPE,
		      "text/css"));
	}
    }


    
    public static void cssPreloadLink(Appendable sb, String url)
            throws IOException {
        String template =
            "<link rel='preload' href='file.css' as='style' onload=\"this.onload=null;this.rel='stylesheet'\">\n";
        //\n<noscript><link rel='stylesheet' href='file.css'></noscript>\n";
        sb.append(template.replace("file.css", url));
    }



    
    public static String cssBlock(String css) {
        return importCss(css);
    }

    
    public static String importCss(String css) {
        return tag(TAG_STYLE, " type='text/css' ", css);
    }


    
    private static String blockHideImageUrl;

    
    private static String blockShowImageUrl;


    
    private static String inlineHideImageUrl;

    
    private static String inlineShowImageUrl;

    
    public static void setBlockHideShowImage(String hideImg, String showImg) {
        if (blockHideImageUrl == null) {
            blockHideImageUrl = hideImg;
            blockShowImageUrl = showImg;
        }
    }



    
    public static void setInlineHideShowImage(String hideImg,
            String showImg) {
        if (inlineHideImageUrl == null) {
            inlineHideImageUrl = hideImg;
            inlineShowImageUrl = showImg;
        }
    }




    
    public static String makeShowHideBlock(String label, String content, boolean visible) {
        return makeShowHideBlock(
            label, content, visible,
            cssClass("toggleblocklabel ramadda-clickable"));
    }

    
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra) {
        return HtmlUtils.makeShowHideBlock(label, content, visible,
                                           headerExtra,
                                           HtmlUtils.cssClass(CLASS_BLOCK),
                                           blockHideImageUrl,
                                           blockShowImageUrl);
    }





    
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra) {
        return HtmlUtils.makeShowHideBlock(label, content, visible,
                                           headerExtra, blockExtra,
                                           blockHideImageUrl,
                                           blockShowImageUrl);
    }





    
    public static int blockCnt = 10000;


    
    public static String getUniqueId(String prefix) {
	blockCnt++;
	if(blockCnt<0) blockCnt=10000;
        return prefix + blockCnt;
    }


    
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra,
					   String hideImg,
                                           String showImg) {
	StringBuilder sb = new StringBuilder();
	String link = makeShowHideBlock(sb, label, content, visible, headerExtra,
					blockExtra, hideImg, showImg);
	return link+sb;
    }

    public static String makeShowHideBlock(StringBuilder sb,
					   String label,
					   String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra,
					   String hideImg,
                                           String showImg) {	

	if(hideImg==null) hideImg = blockHideImageUrl;
	if(showImg==null) showImg = blockShowImageUrl;	
        String        id  = "block_" + (blockCnt++);
        String        img = "";
        if (Utils.stringDefined(showImg)) {
            img = span(img(visible
			   ? hideImg
			   : showImg, "",
			   attrs("align","bottom")), 
		       id(id + "img"));
	    img =span(img,attrs("class","ramadda-clickable ramadda-toggle-link"));
        }
	String imageId = id + "img";
        String mouseEvent = onMouseClick(
					 call("toggleBlockVisibility",
					      squote(id),
					      squote(imageId),
					      squote(hideImg),
					      squote(showImg)));
        String link = img + space(1) + label;
        sb.append(open("div",blockExtra));
        sb.append("<div " + clazz("hideshowblock")
                  + id(id)
                  + style("display:block;visibility:visible")
                  + ">");
        if ( !visible) {
            script(sb, call("HtmlUtils.hide", HtmlUtils.squote(id)));
        }

        sb.append(content.toString());
        sb.append(close(TAG_DIV));
        sb.append(close(TAG_DIV));
        return div(link, headerExtra + mouseEvent
		   +attrs("block-id",id,"block-image-id",imageId));

    }


    
    public static String[] getToggle(String label, boolean visible) {
        return getToggle(label, visible, blockHideImageUrl,
                         blockShowImageUrl);
    }



    
    public static String[] getToggle(String label, boolean visible,
                                     String hideImg, String showImg) {
        //TODO
        String id   = "block_" + (blockCnt++);
        String icon = visible
                      ? hideImg
                      : showImg;
        String img;
        if (isFontAwesome(icon)) {
            img = faIcon(icon, HtmlUtils.id(id + "img"));
        } else {
            img = HtmlUtils.img(icon, "", HtmlUtils.id(id + "img"));
        }
        String link =
            HtmlUtils.jsLink(HtmlUtils.onMouseClick("toggleBlockVisibility('"
                + id + "','" + id + "img','" + hideImg + "','" + showImg
                + "')"), img, HtmlUtils.cssClass("toggleblocklabellink"));

        if (label.length() > 0) {
            link = link + " " + label;
        }

        String initJS = "";
        if ( !visible) {
            initJS = HtmlUtils.call("hide", HtmlUtils.squote(id));
        }

        return new String[] { id, link, initJS };
    }


    

    public static void addFormChangeListener(Appendable sb,String formId)  throws Exception {
	script(sb,call("HtmlUtils.checkInputChange", quote(formId)));
    }

    public static String makeToggleInline(String label, String content,
                                          boolean visible) {

        StringBuilder sb = new StringBuilder();
        makeToggleInline(sb, label, content, visible);

        return sb.toString();
    }

    
    public static void makeToggleInline(Appendable sb, String label,
                                        String content, boolean visible,String...extraAttrs) {

        try {
            String hideImg = inlineHideImageUrl;
            String showImg = inlineShowImageUrl;
            if (hideImg == null) {
                hideImg = blockHideImageUrl;
            }
            if (showImg == null) {
                showImg = blockShowImageUrl;
            }
            String id  = getUniqueId("block_");
            String img = "";
            if ((showImg != null) && (showImg.length() > 0)) {
                img = HtmlUtils.img(visible
                                    ? hideImg
                                    : showImg, "",
                                    " id='" + id
                                    + "img' ") + HtmlUtils.space(1);
            }
            String link = HtmlUtils.jsLink(
                              HtmlUtils.onMouseClick(
                                  Utils.concatString(
                                      "toggleInlineVisibility('", id, "','",
                                      id, "img','", hideImg, "','", showImg,
                                      "')")), img + label,
                                          HtmlUtils.cssClass(
                                              "toggleblocklabellink"));

            //        sb.append(RepositoryManager.tableSubHeader(link));
	    if(extraAttrs.length>0) {
		link = HtmlUtils.span(link,HtmlUtils.attrs(extraAttrs));
	    }
            sb.append(link);
            open(sb, TAG_SPAN, "class", "hideshowblock", "id", id, "style",
                 "display:inline;visibility:visible");
            if ( !visible) {
                HtmlUtils.script(sb,
                                 HtmlUtils.call("HtmlUtils.hide",
                                     HtmlUtils.squote(id)));
            }
            sb.append(content);
            sb.append(close(TAG_SPAN));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }




    
    public static String makeShowHideBlock(String clickHtml, String label,
                                           String content, boolean visible) {
	StringBuilder contents = new StringBuilder();
	String link = makeShowHideBlock(contents,clickHtml, label, content,visible);
	return link+contents;
    }

    public static String makeShowHideBlock(StringBuilder contents,
					   String clickHtml, String label,
					   String content, boolean visible) {	
	String        id = "block_" + (blockCnt++);
        String mouseEvent = onMouseClick("toggleBlockVisibility('"
                                + id + "','" + id + "img','" + "" + "','"
                                + "" + "')");
        String link = HtmlUtils.jsLink(
                          mouseEvent, clickHtml,
                          clazz("toggleblocklabellink")) + label;
        open(contents, TAG_SPAN, "class", "hideshowblock", "id", id, "style",
             "display:block;visibility:visible");
        if ( !visible) {
            HtmlUtils.script(contents,call("HtmlUtils.hide", HtmlUtils.squote(id)));
        }

        contents.append(content.toString());
        contents.append(close(TAG_SPAN));

        return link;
    }


    
    public static String jsMakeArgs(boolean andSquote, String... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (andSquote) {
                s = s.replaceAll("'", "\\\\'");
                s = squote(s);
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(s);
        }

        return sb.toString();
    }






    
    public static String getString(boolean v) {
        return "" + v;
    }


    
    public static String getString2(boolean v) {
        return Boolean.toString(v);
    }


    
    public static void main(String[] args) throws Exception {
        System.err.println(sanitizeString("https://10000cities.org/repository/entry/show?entryid=abefb1bc-e8f5-468c-abba-2985b38cc877&output=streetview&heading=0 Hello there and another one https://10000cities.org/repository/entry/show?entryid=abefb1bc-e8f5-468c-abba-2985b38cc877&output=streetview&heading=0", true));
	


        if (true) {
            return;
        }



        debug1 = true;
        parseHtmlProperties("multi=2  \n   z= \"1\" template=\"x\ny\"");

        if (true) {
            return;
        }

        System.err.println(parseHtmlProperties("single"));
        System.err.println(parseHtmlProperties("single1 single2"));
        System.err.println(parseHtmlProperties("flag1=foo flag2=bar"));
        System.err.println(parseHtmlProperties("flag1=foo flag2=\"bar\" "));
        System.err.println(
            parseHtmlProperties("title=foo full --- name=bar xxx=\"\" "));
        if (true) {
            return;
        }


        List<HtmlUtils.Link> links = HtmlUtils.extractLinks(new URL(args[0]),
                                         (args.length > 1)
                                         ? args[1]
                                         : null);
        for (HtmlUtils.Link link : links) {
            System.err.println("link:" + link.url);
        }
        if (true) {
            return;
        }

        int           cnt = 10000000;
        boolean       v   = true;
        StringBuilder sb  = new StringBuilder();
        for (int j = 0; j < 10; j++) {

            long t1 = System.currentTimeMillis();
            for (int i = 0; i < cnt; i++) {
                //                sb.setLength(0);
                //                sb.append("hello there");
                sb = new StringBuilder("hello there");
            }
            long t2 = System.currentTimeMillis();
            System.err.println("Time:" + (t2 - t1));
        }
    }

    
    public static final String sizeAttr(int size) {
        return attr("size", "" + size);
    }


    
    public static void makeAccordion(Appendable sb, String title,
                                     String contents)
            throws Exception {

        makeAccordion(sb, title, contents, null, null);
    }

    
    public static void makeAccordion(Appendable sb, String title,
                                     String contents, String wrapperClass,
                                     String headerClass)
            throws Exception {
        makeAccordion(sb, title, contents, true, wrapperClass, headerClass);
    }

    
    public static void makeAccordion(Appendable sb, String title,
                                     String contents, boolean collapse,
                                     String wrapperClass, String headerClass)
            throws Exception {


        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();
        titles.add(title);
        tabs.add(contents);
        makeAccordion(sb, titles, tabs, collapse, wrapperClass, headerClass);
    }

    
    public static void makeAccordion(Appendable sb, List titles, List contents)
            throws Exception {
        makeAccordion(sb, titles, contents, false, null, null);
    }

    public static void makeAccordion(Appendable sb, Object[] titles, Object[]contents)
            throws Exception {
        makeAccordion(sb, Utils.arrayToList(titles), Utils.arrayToList(contents), false, null, null);
    }
    
    
    public static void makeAccordion(Appendable sb, List titles,
                                     List contents, boolean collapse)
            throws Exception {
        makeAccordion(sb, titles, contents, collapse, null, null);
    }



    
    public static void makeAccordion(Appendable sb, List titles,
                                     List contents, boolean collapse,
                                     String wrapperClass, String headerClass)
            throws Exception {

        String accordionId = "accordion_" + (blockCnt++);
        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                 HtmlUtils.cssClass(((wrapperClass != null)
                ? wrapperClass
                : "") + " ui-accordion ui-widget ui-helper-reset") + HtmlUtils
                    .id(accordionId)));
        for (int i = 0; i < titles.size(); i++) {
            String title   = titles.get(i).toString();
            String content = contents.get(i).toString();
            sb.append(HtmlUtils.open(HtmlUtils.TAG_H3,
                                     HtmlUtils.cssClass(((headerClass != null)
                    ? headerClass + " "
                    : "") + " ui-accordion-header ui-helper-reset ui-state-active ui-corner-top")));
            sb.append("<a href=\"#\">");
            sb.append(title);
            sb.append("</a></h3>");
            String contentsId = "accordion_contents_" + (blockCnt++);
            content = HtmlUtils.div(
                content,
                HtmlUtils.id(contentsId)
                + HtmlUtils.cssClass("ramadda-accordion-contents"));
            sb.append(HtmlUtils.div(content));
        }
        sb.append("</div>");
        String args = "{collapsible: true";
        if (collapse) {
            args += ", active: false}";
        } else {
            args += ", active: 0}";
        }

        HtmlUtils.script(sb,
                         "HtmlUtils.makeAccordion(\"#" + accordionId + "\" "
                         + "," + args + ");\n");
    }


    public static void makeTabs(Appendable sb, List<NamedBuffer>buffers) throws Exception {
	List<String> titles = new ArrayList<String>();
	List<StringBuilder> contents = new ArrayList<StringBuilder>();
	for(NamedBuffer buff: buffers) {
	    titles.add(buff.getName());
	    contents.add(buff.getBuffer());
	}
	makeTabs(sb,titles, contents);
    }


    public static void makeTabs(Appendable sb, List titles,
                                     List contents)
            throws Exception {
	String uid = getUniqueId("tab");
	open(sb,"div",attrs("id",uid));
	open(sb,"ul","");
	int cnt = 1;
	for(Object title: titles) {
	    tag(sb,"li","",href("#"+uid+"-"+(cnt++),title.toString()));
	}
	close(sb,"ul");
	cnt=1;
	for(Object c: contents) {
	    open(sb,"div",attrs("id",uid+"-"+(cnt++)));
	    sb.append(c.toString());
	    close(sb,"div");
	}
	close(sb,"div");
	String js = "jQuery(function(){\njQuery('#" + uid
                            + "').tabs({activate: HtmlUtil.tabLoaded})});\n";
	sb.append(script(js));
    }


    
    public static void tooltip(Appendable sb, String icon, String msg)
            throws Exception {
        String id     = getUniqueId("tooltipblock_");
        String opener = getUniqueId("tooltipopener_");
        sb.append(img(icon, "Help",
                      attr(ATTR_ID, opener)
                      + cssClass("ramadda-tooltip-icon")));
        tag(sb, TAG_DIV,
            cssClass("ramadda-tooltip-contents") + attr(ATTR_ID, id), msg);
        HtmlUtils.script(sb,
                         HtmlUtils.call("HtmlUtils.tooltipInit",
                                        HtmlUtils.squote(opener),
                                        HtmlUtils.squote(id)));
    }


    
    public static String comment(String s) {
        Appendable sb = Utils.makeAppendable();
        comment(sb, s);

        return sb.toString();
    }

    
    public static void comment(Appendable sb, String s) {
        try {
            s = s.replaceAll("\n", " ");
            Utils.concatBuff(sb, "\n<!-- ", s, " -->\n");
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }


    
    public static Color decodeColor(String value, Color dflt) {
        if (value == null) {
            return dflt;
        }
        if (value.equals("transparent")) {
            return new Color(1f, 0f, 0f, 0.0f);
        }
        value = value.replaceAll("rgb *\\(", "").replaceAll("\\)", "");
        //        System.err.println("v:"+value);
        value = value.trim();
        if (value.equals("null")) {
            return null;
        }
        String s       = value;
        String lookFor = ",";
        int    i1      = s.indexOf(lookFor);
        if (i1 < 0) {
            lookFor = " ";
            i1      = s.indexOf(lookFor);
        }
        if (i1 > 0) {
            String red = s.substring(0, i1);
            s = s.substring(i1 + 1).trim();
            int i2 = s.indexOf(lookFor);
            if (i2 > 0) {
                String green = s.substring(0, i2);
                String blue  = s.substring(i2 + 1);
                try {
                    return new Color(Integer.decode(red).intValue(),
                                     Integer.decode(green).intValue(),
                                     Integer.decode(blue).intValue());
                } catch (Exception exc) {
                    System.err.println("Bad color:" + value);
                }
            }
        }

        try {
            return new Color(Integer.decode(s).intValue());
        } catch (Exception e) {
            s = s.toLowerCase();

            return Utils.decodeColor(s, dflt);
        }
    }


    
    public static List<Link> extractLinks(URL url, String linkPattern)
            throws Exception {
        if ( !Utils.stringDefined(linkPattern)) {
            linkPattern = null;
        }
        if (url.getProtocol().equals("ftp")) {
            return extractLinksFtp(url, linkPattern);
        }
        String html = Utils.readUrl(url.toString());

        return extractLinks(url, html, linkPattern);
    }

    
    public static List<Link> extractLinks(URL url, String html,
                                          String linkPattern)
            throws Exception {
        return extractLinks(url, html, linkPattern, false);
    }


    
    public static List<Link> extractLinks(URL url, String html,
                                          String linkPattern, boolean images)
            throws Exception {
        HashSet    seen    = new HashSet();
        List<Link> links   = new ArrayList<Link>();
        String     pattern = images
                             ? "(?s)(?i)<\\s*img [^>]*?src\\s*=\\s*(\"|')([^\"'>]+)(\"|')[^>]*?>"
                             : "(?s)(?i)<\\s*a[^>]*?\\s*href\\s*=\\s*(\"|')([^\"'>]+)(\"|')[^>]*?>(.*?)</a";

        html = html.replaceAll("\t", " ");
        //<a target="_blank" title="/gov/data/GISDLData/Footprints.kmz" href="/gov/data/GISDLData/Footprints.kmz">KMZ</a>
        Matcher matcher = Pattern.compile(pattern).matcher(html);
        //      System.err.println("pattern:" + linkPattern);
        while (matcher.find()) {
            String href = matcher.group(2);
            href = href.replaceAll(" ", "");
            String label = images
                           ? ""
                           : matcher.group(4);
            label = StringUtil.stripTags(label).trim();
            label = label.replace("&nbsp;", " ");
            try {
                String tmp = href.replace("\\", "/").replaceAll("#.*", "");
                if (tmp.toLowerCase().startsWith("javascript")) {
                    continue;
                }
                URL newUrl = new URL(url, tmp);
                if (seen.contains(newUrl)) {
                    continue;
                }
                seen.add(newUrl);

                label = label.replaceAll("\\s+", " ");
                Link link = new Link(href, newUrl, label);
                if ( !link.matches(linkPattern)) {
                    //                  System.err.println("\tHREF:" + newUrl +" " + label +" not match");
                    continue;
                }
                if (href.toLowerCase().startsWith("javascript:")) {
                    continue;
                }
                links.add(link);
            } catch (Exception exc) {
                System.err.println("Error:" + exc);
                exc.printStackTrace();
            }
        }

        return links;
    }


    
    public static List<Link> extractLinksFtp(URL url, String linkPattern)
            throws Exception {
        FTPClient ftpClient = null;
        try {
            ftpClient = Utils.makeFTPClient(url);
            if (ftpClient == null) {
                return null;
            }
            List<Link> links = new ArrayList<Link>();
            FTPFile[]  files = ftpClient.listFiles(url.getPath());
            for (int i = 0; i < files.length; i++) {
                if ( !files[i].isFile()) {
                    continue;
                }
                String href  = files[i].getName();
                String label = href;
                if (linkPattern != null) {
                    if ( !(href.matches(linkPattern)
                            || label.matches(linkPattern))) {
                        continue;
                    }
                }
                URL newUrl = new URL(url, href);
                links.add(new Link(href, newUrl, label, files[i].getSize()));
            }

            return links;
        } finally {
            Utils.closeConnection(ftpClient);
        }
    }





    
    public static class Link {

        
        private String link;

        
        private URL url;

        
        private String label;

        
        private long size = -1;

        
        public Link(String link, URL url, String label, long size) {
            this(link, url, label);
            this.size = size;
        }

        
        public Link(String link, URL url, String label) {
            this.link  = link;
            this.url   = url;
            this.label = label;
        }

        
        public boolean matches(String pattern) {
            if ( !Utils.stringDefined(pattern)) {
                return true;
            }
            if (link.matches(pattern) || label.matches(pattern)
                    || url.toString().matches(pattern)) {
                return true;
            }

            return false;
        }

        
        public void setUrl(URL value) {
            url = value;
        }

        
        public String getLink() {
            return link;
        }


        
        public URL getUrl() {
            return url;
        }

        
        @Override
        public int hashCode() {
            return url.hashCode();
        }

        
        @Override
        public boolean equals(Object o) {
            if ( !(o instanceof Link)) {
                System.err.println("not a link");

                return false;
            }
            Link that = (Link) o;

            //      System.err.println("link:" + url +"  other:" + that.getUrl() +" " +url.equals(that.getUrl()));
            return url.equals(that.getUrl());
        }

        
        public void setLabel(String value) {
            label = value;
        }

        
        public String getLabel() {
            return label;
        }

        
        public String getHref() {
	    return getHref(false);
	}

        public String getHref(boolean target) {
	    if(!target)return HtmlUtils.href(this.url.toString(), this.label);
	    return HtmlUtils.href(this.url.toString(), this.label,attrs("target","link"));
        }

        
        public void setSize(long value) {
            size = value;
        }

        
        public long getSize() {
            return size;
        }

        
        public String toString() {
            return url + " " + label;
        }

    }

    public static boolean isAudio(String file) {
        String ext  = IOUtil.getFileExtension(file);
	if(ext==null) return false;
	ext = ext.toLowerCase();
        if (ext.equals(".ogg") || ext.equals(".oga") || ext.equals(".wav") || ext.equals(".m4a") ||
	    ext.equals(".mp4")) {
	    return true;
        }
	return false;
    }

    public static boolean isVideo(String file) {
        String ext  = IOUtil.getFileExtension(file);
	if(ext==null) return false;
	ext = ext.toLowerCase();
	ext = ext.replaceAll("^\\.","");
	if(ext.matches("(m4v|mp4|m4v|mov)")) {
	    return true;
	}
	return false;
    }

    public static boolean isPdf(String file) {
        String ext  = IOUtil.getFileExtension(file);
	if(ext==null) return false;
	ext = ext.toLowerCase();
        if (ext.equals(".pdf")) {
	    return true;
        }
	return false;
    }



    public static String getAudioEmbed(String url) {
        String html =
            "<audio controls preload=\"none\" style=\"width:480px;\">\n <source src=\"${url}\" type=\"${mime}\" />\n <p>Your browser does not support HTML5 audio.</p>\n </audio>";

        String mime = "audio/wav";
        String ext  = IOUtil.getFileExtension(url);
        if (ext.equals(".ogg")) {
            mime = "audio/ogg";
        } else if (ext.equals(".oga")) {
            mime = "audio/ogg";
        } else if (ext.equals(".wav")) {
            mime = "audio/wav";
        } else if (ext.equals(".m4a")) {
            mime = "audio/mp4";
        } else if (ext.equals(".mp4")) {
            mime = "audio/mp4";
        } else {
	    return "Unknown audio:"  + url;
	}

        html = html.replace("${url}", url);
        html = html.replace("${mime}", mime);

        return html;
    }


    public static String getMediaEmbed(String mediaUrl, String width, String height) {
	String _mediaUrl = mediaUrl.toLowerCase();
	String mediaId = HtmlUtils.getUniqueId("media_");
	String _path=_mediaUrl;
	if(_path.indexOf("?")>=0) {
	    _path = _path.substring(0,_path.indexOf("?"));
	}
	String player = null;
	//	Utils.add(attrs, "mediaId", JU.quote(mediaId));
	if (_path.endsWith(".mp3") ||
	    _path.endsWith(".m4a") ||
	    _path.endsWith(".webm") ||
	    _path.endsWith("ogg") ||
	    _path.endsWith("wav")) {
	    player = tag("audio", attrs(new String[] {
			"controls", "", "id", mediaId, "style",
			css("height", makeDim(AUDIO_HEIGHT, "px"), "width",
			       makeDim(width, "px"))
		    }), tag("source", attrs(new String[] { "src", mediaUrl,
								 "type",
								 "audio/mpeg" }), "Your browser does not support the audio tag."));
	    //Utils.add(attrs, "media", JU.quote("media"));
	} else if (_path.endsWith(".m4v") ||
		   _path.endsWith(".mp4")) {
	    player = tag("video", attrs(new String[] {
			"id", mediaId, "controls", "", "preload", "metadata",
			"height", height, "width", width
		    }), tag("source", attrs(new String[] { "src", mediaUrl,
								 "type", "video/mp4" })));
	    //	    Utils.add(attrs, "media", JU.quote("media"));
	} else if (_mediaUrl.endsWith(".mov")
		   || _path.endsWith(".mov")) {
	    /*
	      player = tag("embed",  attrs("src",mediaUrl,
	      "width", width,
	      "height", height,
	      "controller","true",
	      "autoplay","false",
	      "loop","false"));
	    */
	    player = HtmlUtils.tag("video", HtmlUtils.attrs(new String[] {
			"id", mediaId, HtmlUtils.ATTR_SRC, mediaUrl,
			HtmlUtils.ATTR_CLASS, "ramadda-video-embed",
			HtmlUtils.ATTR_WIDTH, width, HtmlUtils.ATTR_HEIGHT,
			height,
		    }) + " controls ", HtmlUtils.tag("source",
						     HtmlUtils.attrs(new String[] { HtmlUtils.ATTR_SRC,
										    mediaUrl })));
	    //	    Utils.add(attrs, "media", JU.quote("media"));
	} else {
	    return null;
	}
	return player;
    }				       


    public static String getPdfEmbed(String url,Hashtable props) {

        StringBuilder sb = new StringBuilder();
	String page  = Utils.getProperty(props,"page",null);
	if(page!=null) {
	    url+="#";
	    url+="page=" + page;
	}

	sb.append(open("iframe",attrs("src",url,
					    "class","ramadda-iframe-pdf",
					    "type","application/pdf",
					    "style",
					    Utils.getProperty(props,"style","border:1px solid #ccc;"),
					    "frameborder",
					    Utils.getProperty(props,"frameBorder","0"),
					    "scrolling",
					    Utils.getProperty(props,"scrolling","auto"),
					    "width",
					    Utils.getProperty(props,"width","90%"),
					    "height",
					    Utils.getProperty(props,"height","1000px")
					    )));

	sb.append(close("iframe"));
        return sb.toString();
    }



    //unescape

    
    public static final String unescapeHtml3(final String input) {
        StringWriter writer = null;
        int          len    = input.length();
        int          i      = 1;
        int          st     = 0;
        while (true) {
            // look for '&'
            while ((i < len) && (input.charAt(i - 1) != '&')) {
                i++;
            }
            if (i >= len) {
                break;
            }

            // found '&', look for ';'
            int j = i;
            while ((j < len) && (j < i + MAX_ESCAPE + 1)
                    && (input.charAt(j) != ';')) {
                j++;
            }
            if ((j == len) || (j < i + MIN_ESCAPE)
                    || (j == i + MAX_ESCAPE + 1)) {
                i++;

                continue;
            }

            // found escape 
            if (input.charAt(i) == '#') {
                // numeric escape
                int        k         = i + 1;
                int        radix     = 10;

                final char firstChar = input.charAt(k);
                if ((firstChar == 'x') || (firstChar == 'X')) {
                    k++;
                    radix = 16;
                }

                try {
                    int entityValue = Integer.parseInt(input.substring(k, j),
                                          radix);

                    if (writer == null) {
                        writer = new StringWriter(input.length());
                    }
                    writer.append(input.substring(st, i - 1));

                    if (entityValue > 0xFFFF) {
                        final char[] chrs = Character.toChars(entityValue);
                        writer.write(chrs[0]);
                        writer.write(chrs[1]);
                    } else {
                        writer.write(entityValue);
                    }

                } catch (NumberFormatException ex) {
                    i++;

                    continue;
                }
            } else {
                // named escape
                CharSequence value = lookupMap.get(input.substring(i, j));
                if (value == null) {
                    i++;

                    continue;
                }

                if (writer == null) {
                    writer = new StringWriter(input.length());
                }
                writer.append(input.substring(st, i - 1));

                writer.append(value);
            }

            // skip escape
            st = j + 1;
            i  = st;
        }

        if (writer != null) {
            writer.append(input.substring(st, len));

            return writer.toString();
        }

        return input;
    }

    
    private static final String[][] ESCAPES = {
        { "\"", "quot" }, { "&", "amp" }, { "<", "lt" }, { ">", "gt" },
        { "\u00A0", "nbsp" }, { "\u00A1", "iexcl" }, { "\u00A2", "cent" },
        { "\u00A3", "pound" }, { "\u00A4", "curren" }, { "\u00A5", "yen" },
        { "\u00A6", "brvbar" }, { "\u00A7", "sect" }, { "\u00A8", "uml" },
        { "\u00A9", "copy" }, { "\u00AA", "ordf" }, { "\u00AB", "laquo" },
        { "\u00AC", "not" }, { "\u00AD", "shy" }, { "\u00AE", "reg" },
        { "\u00AF", "macr" }, { "\u00B0", "deg" }, { "\u00B1", "plusmn" },
        { "\u00B2", "sup2" }, { "\u00B3", "sup3" }, { "\u00B4", "acute" },
        { "\u00B5", "micro" }, { "\u00B6", "para" }, { "\u00B7", "middot" },
        { "\u00B8", "cedil" }, { "\u00B9", "sup1" }, { "\u00BA", "ordm" },
        { "\u00BB", "raquo" }, { "\u00BC", "frac14" }, { "\u00BD", "frac12" },
        { "\u00BE", "frac34" }, { "\u00BF", "iquest" },
        { "\u00C0", "Agrave" }, { "\u00C1", "Aacute" }, { "\u00C2", "Acirc" },
        { "\u00C3", "Atilde" }, { "\u00C4", "Auml" }, { "\u00C5", "Aring" },
        { "\u00C6", "AElig" }, { "\u00C7", "Ccedil" }, { "\u00C8", "Egrave" },
        { "\u00C9", "Eacute" }, { "\u00CA", "Ecirc" }, { "\u00CB", "Euml" },
        { "\u00CC", "Igrave" }, { "\u00CD", "Iacute" }, { "\u00CE", "Icirc" },
        { "\u00CF", "Iuml" }, { "\u00D0", "ETH" }, { "\u00D1", "Ntilde" },
        { "\u00D2", "Ograve" }, { "\u00D3", "Oacute" }, { "\u00D4", "Ocirc" },
        { "\u00D5", "Otilde" }, { "\u00D6", "Ouml" }, { "\u00D7", "times" },
        { "\u00D8", "Oslash" }, { "\u00D9", "Ugrave" },
        { "\u00DA", "Uacute" }, { "\u00DB", "Ucirc" }, { "\u00DC", "Uuml" },
        { "\u00DD", "Yacute" }, { "\u00DE", "THORN" }, { "\u00DF", "szlig" },
        { "\u00E0", "agrave" }, { "\u00E1", "aacute" }, { "\u00E2", "acirc" },
        { "\u00E3", "atilde" }, { "\u00E4", "auml" }, { "\u00E5", "aring" },
        { "\u00E6", "aelig" }, { "\u00E7", "ccedil" }, { "\u00E8", "egrave" },
        { "\u00E9", "eacute" }, { "\u00EA", "ecirc" }, { "\u00EB", "euml" },
        { "\u00EC", "igrave" }, { "\u00ED", "iacute" }, { "\u00EE", "icirc" },
        { "\u00EF", "iuml" }, { "\u00F0", "eth" }, { "\u00F1", "ntilde" },
        { "\u00F2", "ograve" }, { "\u00F3", "oacute" }, { "\u00F4", "ocirc" },
        { "\u00F5", "otilde" }, { "\u00F6", "ouml" }, { "\u00F7", "divide" },
        { "\u00F8", "oslash" }, { "\u00F9", "ugrave" },
        { "\u00FA", "uacute" }, { "\u00FB", "ucirc" }, { "\u00FC", "uuml" },
        { "\u00FD", "yacute" }, { "\u00FE", "thorn" }, { "\u00FF", "yuml" },
    };

    
    private static final int MIN_ESCAPE = 2;

    
    private static final int MAX_ESCAPE = 6;

    
    private static final HashMap<String, CharSequence> lookupMap;
    static {
        lookupMap = new HashMap<String, CharSequence>();
        for (final CharSequence[] seq : ESCAPES) {
            lookupMap.put(seq[1].toString(), seq[0]);
        }
    }


    public static String[] strictSanitizeStrings(String...strings) {
	String[] result = new String[strings.length];
	for(int i=0;i<strings.length;i++)
	    result[i] = strictSanitizeString(strings[i]);
	return result;
    }


    
    public static String strictSanitizeString(String s) {    
	if(s==null) return s;
	s = sanitizeString(s);
        s = s.replaceAll("(?i)(script)", "_$1_");
        s = s.replaceAll("(?i)(src)(" + Utils.WHITESPACE_CHARCLASS + "*=)",
                         "_$1_$2");
        s = s.replaceAll("(?i)(onclick)(" + Utils.WHITESPACE_CHARCLASS
                         + "*=)", "_$1_$2");

        return s;
    }    



    
    public static String sanitizeString(String s) {
	return sanitizeString(s,false);
    }

    public static String sanitizeString(String s, boolean removeUrlArgs) {	


        if (s == null) {
            return null;
        }
        s = s.replaceAll("(?i)<span *>", "SPANOPEN");
        s = s.replaceAll("(?i)</span *>", "SPANCLOSE");	
        s = s.replaceAll("(?i)<pre *>", "PREOPEN");
        s = s.replaceAll("(?i)</pre *>", "PRECLOSE");

        s = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        s = s.replaceAll("SPANOPEN", "<span>");
        s = s.replaceAll("SPANCLOSE", "</span>");	
        s = s.replaceAll("PREOPEN", "<pre>");
        s = s.replaceAll("PRECLOSE", "</pre>");

	if(removeUrlArgs) {
	    s = s.replaceAll("\\?[^ \"]+","---");
	}
        return s;
    }


    
    public static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(urlEncodeSpace(s), "UTF-8");
        } catch (Exception exc) {
            System.err.println("error encoding arg(3):" + s + " " + exc);
            exc.printStackTrace();

            return "";
        }
    }


    
    public static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception exc) {
            System.err.println("error decoding:" + s + " " + exc);
            exc.printStackTrace();

            return "";
        }
    }



    
    public static String urlEncodeSpace(String s) {
        return s.replaceAll(" ", "+");
    }


    
    public static String urlEncodeExceptSpace(String s) {
        try {
            s = s.replace(" ", "_SPACE_");
            s = java.net.URLEncoder.encode(s, "UTF-8");
            s = s.replace("_SPACE_", " ");

            return s;
        } catch (Exception exc) {
            System.err.println("error encoding arg(4):" + s + " " + exc);
            exc.printStackTrace();

            return "";
        }
    }



    
    public static String entityEncode(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); ++i) {
            char ch = input.charAt(i);
            if (((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))
                    || ((ch >= '0') && (ch <= '9'))) {
                sb.append(ch);
            } else {
                sb.append("&#" + (int) ch + ";");
            }
        }

        return sb.toString();
    }

    
    public static String entityDecode(String input) {
        return StringEscapeUtils.unescapeHtml3(input);
    }


    
    public static String sanitizeArg(String s) {
        if (s == null) {
            return null;
        }

        return sanitizeString(s).replaceAll("\"", "_");
    }


    
    public static String[] makePopupLink(String link, NamedValue... args) {
        String compId         = "menu_" + HtmlUtils.blockCnt++;
        String linkId         = "menulink_" + HtmlUtils.blockCnt++;
        String linkAttributes = "";
        List<String> attrs = (List<String>) Utils.makeListFromValues("contentId",
                                 HtmlUtils.squote(compId), "anchor",
                                 HtmlUtils.squote(linkId));
        boolean seenAnimate = false;
        String  extraCode   = "";
        for (NamedValue v : args) {
            if (v.getValue() == null) {
                continue;
            }
            if (v.getName().equals("linkAttributes")) {
                linkAttributes = v.getValue().toString();
                continue;
            }
            if (v.getName().equals("extraCode")) {
                extraCode = v.getValue().toString();
                continue;
            }
            Object o = v.getValue();
            if (o == null) {
                continue;
            }
            if (v.getName().equals("animate")) {
                seenAnimate = true;
            }
            attrs.add(v.getName());
            if (o instanceof String) {
                if ( !o.equals("true") && !o.equals("false")) {
                    //Escape and single quotes
                    String s = o.toString();
                    s = s.replaceAll("'", "\\\\'");
                    o = HtmlUtils.squote(s);
                }
            }
            attrs.add(o.toString());
        }

        if ( !seenAnimate) {
            attrs.add("animate");
            attrs.add("true");
        }

        String callArgs = JsonUtil.map(attrs);
        String call     = "HtmlUtils.makeDialog(" + callArgs + ");";
        String onClick  = HtmlUtils.onMouseClick(call + extraCode);
        String href = HtmlUtils.div(link,
                                    HtmlUtils.cssClass("ramadda-popup-link")
                                    + linkAttributes + onClick
                                    + HtmlUtils.id(linkId));

        return new String[] { compId, href };
    }


    
    public static String makePopup(Appendable popup, String link,
                                   String menuContents, NamedValue... args) {
        try {
            String[] tuple  = makePopupLink(link, args);
            String   compId = tuple[0];
            String   href   = tuple[1];
            boolean inPlace = Misc.equals(NamedValue.getValue("inPlace",
                                  args) + "", "true");
            String contents;
            if (inPlace) {
                contents = HtmlUtils.div(menuContents,
                                         HtmlUtils.id(compId)
                                         + HtmlUtils.attr("style",
                                             "display:none;"));
            } else {
                contents = makePopupDiv(menuContents, compId, false, null);
            }
            if (popup != null) {
                popup.append(contents);

                return href;
            }

            return href + contents;
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }



    public static String makeCssClass(String s) {
	if(s==null) return s;
	String delim = "-";
	s=  s.replaceAll("\\s+\\s",delim);
	s=  s.replace(".",delim);	
	return s;
    }

    
    public static String makePopupDiv(String contents, String compId,
                                      boolean makeClose, String header) {
        StringBuilder menu = new StringBuilder();
        if (makeClose) {
            String cLink =
                HtmlUtils.jsLink(
                    HtmlUtils.onMouseClick(
                        "HtmlUtils.hidePopupObject(event);"), getIconImage(
                        ICON_CLOSE, "title", "Close", "class",
                        "ramadda-popup-close"), "");
            if (header != null) {
                contents = HtmlUtils.table(HtmlUtils.row("<td width=5%>"
                        + cLink + "</td><td>" + header + "</td>")) + contents;
            } else {
                //                contents =HtmlUtils.div(cLink,"class=ramadda-popup-header") + contents;
            }
        }

        menu.append(
            HtmlUtils.div(
                contents,
                HtmlUtils.id(compId)
                + HtmlUtils.attr("style", "display:none;")
                + HtmlUtils.cssClass(CSS_CLASS_POPUP_CONTENTS)));


        return menu.toString();
    }



    
    public static String makeFlipCard(String front, String back,
                                      String flipCardAttrs,
                                      String frontAttrs, String backAttrs) {
        return "<div class='ramadda-flip-card ' " + flipCardAttrs
               + "> <div class='ramadda-flip-card-inner'>"
               + "<div class='ramadda-flip-card-front' " + frontAttrs + ">"
               + front + "</div><div class='ramadda-flip-card-back' "
               + backAttrs + ">" + back + "</div></div></div>";
    }
    
    public static void addPageSearch(Appendable buff, String sel1, String sel2, String label) {
	addPageSearch(buff,sel1,sel2,label,null);
    }


    public static void addPageSearch(Appendable buff, String sel1, String sel2, String label,List<String>args) {
	try {
	    buff.append("<center>");
	    String opts = args==null?"{}":JsonUtil.map(args);
	    HtmlUtils.script(buff,HtmlUtils.call("HtmlUtils.initPageSearch",
						 HtmlUtils.squote(sel1), 
						 sel2==null?"null":HtmlUtils.squote(sel2),
						 label==null?"null":HtmlUtils.squote(label),
						 "false",
						 opts));
	    buff.append("</center>");
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }



}

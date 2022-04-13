/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/
// Copyright (c) 2008-2021 Geode Systems LLC
// SPDX-License-Identifier: Apache-2.0

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




/**
 */

@SuppressWarnings("unchecked")
public class HtmlUtils implements HtmlUtilsConstants {

    /** _more_ */
    public static final String SPACE = "&nbsp;";

    /** _more_ */
    public static final String SPACE2 = "&nbsp;&nbsp;";

    /** _more_ */
    public static final String SPACE3 = "&nbsp;&nbsp;&nbsp;";

    /** _more_ */
    public static final String ICON_CLOSE = "fas fa-window-close";

    /**  */
    public static final String CSS_CLASS_POPUP_CONTENTS =
        "ramadda-popup-contents";

    /**  */
    public static final String CSS_CLASS_POPUP = "ramadda-popup";


    /**
     * _more_
     *
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String comma(String... s) {
        return StringUtil.join(",", s);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     */
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




    /**
     * _more_
     *
     * @param comp _more_
     *
     * @return _more_
     */
    public static String open(String comp) {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(comp);
        sb.append(">");

        return sb.toString();
    }




    /**
     * _more_
     *
     * @param comp _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String open(String comp, String... attrs) {
        StringBuilder sb = new StringBuilder();
        open(sb, comp, attrs);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param tag _more_
     * @param attrs _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param sb _more_
     * @param comp _more_
     */
    public static void dangleOpen(Appendable sb, String comp) {
        try {
            sb.append("<");
            sb.append(comp);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    /**
     * _more_
     *
     * @param sb _more_
     */
    public static void dangleClose(Appendable sb) {
        try {
            sb.append(">");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    /**
     * _more_
     *
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String close(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String comp : args) {
            close(sb, comp);
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param args _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param comp _more_
     *
     * @return _more_
     */
    public static String tag(String comp) {
        StringBuilder sb = new StringBuilder();
        tag(sb, comp);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param tag _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param comp _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String tag(String comp, String attrs) {
        StringBuilder sb = new StringBuilder();
        tag(sb, comp, attrs);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param comp _more_
     * @param attrs _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param tag _more_
     * @param attrs _more_
     * @param inner _more_
     *
     * @return _more_
     */
    public static String tag(String tag, String attrs, String inner) {
        StringBuilder sb = new StringBuilder();
        tag(sb, tag, attrs, inner);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param tag _more_
     * @param attrs _more_
     * @param inner _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param sb _more_
     * @param tag _more_
     * @param inner _more_
     * @param attrs _more_
     *
     * @return _more_
     */
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


    /**
     *
     * @param name _more_
     * @param value _more_
      * @return _more_
     */
    public static String hiddenBase64(String name, Object value) {
        String s = value.toString();

        return hidden(name, Utils.encodeBase64(s), "");
    }


    /**
     * _more_
     *
     * @param value _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String hidden(String name, Object value) {
        return hidden(name, value, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String hidden(String name, Object value, String extra) {
        StringBuilder sb = new StringBuilder();
        hidden(sb, name, value, extra);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static Appendable hidden(Appendable sb, String name, Object value,
                                    String extra) {
        tag(sb, TAG_INPUT,
            extra
            + attrs(ATTR_TYPE, TYPE_HIDDEN, ATTR_NAME, name, ATTR_VALUE,
                    "" + value, ATTR_CLASS, CLASS_HIDDENINPUT));

        return sb;
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String hbox(Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr valign=top>");
        for (Object s : args) {
            sb.append("<td>" + s + "</td>");
        }
        sb.append("</tr></table>");

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public static void br(Appendable sb) throws Exception {
        sb.append("<br>");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static String br() {
        return "<br>";
    }

    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public static String br(String line) {
        return line + open(TAG_BR);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String hr() {
        return open(TAG_HR);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String p() {
        return "<p>";
    }

    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String nobr(String inner) {
        return tag(TAG_NOBR, "", inner);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param inner _more_
     *
     * @throws Exception _more_
     */
    public static void b(Appendable sb, String inner) throws Exception {
        tag(sb, TAG_B, inner);
    }


    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String b(String inner) {
        return tag(TAG_B, "", inner);
    }

    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String italics(String inner) {
        return tag(TAG_I, "", inner);
    }

    /**
     * _more_
     *
     * @param inner _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String li(String inner, String extra) {
        return tag(TAG_LI, extra, inner);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param inner _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable centerBlock(Appendable sb, String inner)
            throws Exception {
        sb.append(
            "<center><div style='display:inline-block;text-align:left;'>");
        sb.append(inner);
        sb.append("</div></center>");

        return sb;
    }


    /**
     * _more_
     *
     * @param inner _more_
     *
     * @return _more_
     */
    public static String center(String inner) {
        return tag(TAG_CENTER, "", inner);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param inner _more_
     *
     * @throws Exception _more_
     */
    public static void center(Appendable sb, String inner) throws Exception {
        tag(sb, TAG_CENTER, "", inner);
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String pad(String s) {
        StringBuilder sb = new StringBuilder();
        pad(sb, s);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String padLeft(String s) {
        StringBuilder sb = new StringBuilder();
        padLeft(sb, s);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @return _more_
     */
    public static Appendable padLeft(Appendable sb, String s) {
        try {
            sb.append(space(1));
            sb.append(s);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String padRight(String s) {
        StringBuilder sb = new StringBuilder();
        padRight(sb, s);

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     * @return _more_
     */
    public static Appendable padRight(Appendable sb, String s) {
        try {
            sb.append(s);
            sb.append(space(1));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return sb;
    }


    /**
     *
     * @param label _more_
     *
     * @return _more_
     */
    public static String backButton(String label) {
        String link = href("javascript:history.back()", (label != null)
                ? label
                : "Back");

        return button(link);
    }


    /**
     * _more_
     *
     * @param html _more_
     *
     * @return _more_
     */
    public static String button(String html) {
        return span(html, cssClass("ramadda-button"));
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String buttons(String... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(buttonSpace());
            }
            sb.append(args[i]);
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String buttons(List<String> args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(buttonSpace());
            }
            sb.append(args.get(i));
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static String buttonSpace() {
        return space(2);
    }

    /**
     * _more_
     *
     * @param cnt _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        quote(sb, s);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param s _more_
     * @param sb _more_
     *
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String squote(String s) {
        StringBuilder sb = new StringBuilder();
        squote(sb, s);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     *
     *
     * @return _more_
     */
    public static Appendable squote(Appendable sb, String s) {
        try {
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

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public static String img(String path) {
        return img(path, "");
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param title _more_
     *
     * @return _more_
     */
    public static String img(String path, String title) {
        return img(path, title, "");
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String image(String path, String... args) {
        return img(path, null, attrs(args));
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param title _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String img(String path, String title, String extra) {
        return image(path, title, extra, null);
    }



    /**
     * _more_
     *
     * @param path _more_
     * @param title _more_
     * @param extra _more_
     * @param inner _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param icon _more_
     *
     * @return _more_
     */
    public static boolean isFontAwesome(String icon) {
        if (icon == null) {
            return false;
        }

        return icon.startsWith("fa-") || icon.startsWith("fas ")
               || icon.startsWith("fab ");
    }



    /**
     * _more_
     *
     * @param icon _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String faIcon(String icon, String... args) {
        String clazz = (icon.trim().indexOf(" ") >= 0)
                       ? icon
                       : "fas " + icon;

        return span(tag("i", " class='" + clazz + "' " + attrs(args), ""),
                    "");
    }

    /**
     *
     * @param icon _more_
     * @param extraClass _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String faIconClass(String icon, String extraClass,
                                     String... args) {
        String clazz = (icon.trim().indexOf(" ") >= 0)
                       ? icon
                       : "fas " + icon;

        return span(tag("i",
                        " class='" + clazz + " " + extraClass + "' "
                        + attrs(args), ""), "");
    }



    /**
     * _more_
     *
     * @param url _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String getIconImage(String url, String... args) {
        if (isFontAwesome(url)) {
            return faIcon(url, args);
        } else {
            return image(url, args);
        }
    }



    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String cssClass(String c) {
        return attr(ATTR_CLASS, c);
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String clazz(String c) {
        return attr(ATTR_CLASS, c);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param c _more_
     */
    public static void clazz(Appendable sb, String c) {
        attr(sb, ATTR_CLASS, c);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param c _more_
     *
     * @return _more_
     */
    public static Appendable cssClass(Appendable sb, String c) {
        attr(sb, ATTR_CLASS, c);

        return sb;
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String title(String c) {
        return attr(ATTR_TITLE, c);
    }

    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String id(String c) {
        return attr(ATTR_ID, c);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param c _more_
     *
     * @return _more_
     */
    public static Appendable id(Appendable sb, String c) {
        attr(sb, ATTR_ID, c);

        return sb;
    }

    /**
     * _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String style(String... args) {
        if (args.length == 1) {
            return attr(ATTR_STYLE, args[0]);
        }

        return attr(ATTR_STYLE, css(args));
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param v1 _more_
     *
     * @return _more_
     */
    public static String bold(String v1) {
        return tag(TAG_B, "", v1);
    }

    /**
     * _more_
     *
     * @param v1 _more_
     *
     * @return _more_
     */
    public static String col(String v1) {
        return col(v1, "");
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param v1 _more_
     *
     * @return _more_
     */
    public static Appendable col(Appendable sb, String v1) {
        col(sb, v1, "");

        return sb;
    }

    /**
     * _more_
     *
     * @param v1 _more_
     * @param attr _more_
     *
     * @return _more_
     */
    public static String col(String v1, String attr) {
        return tag(TAG_TD, " " + attr + " ", v1);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param v1 _more_
     * @param attr _more_
     *
     * @return _more_
     */
    public static Appendable col(Appendable sb, String v1, String attr) {
        tag(sb, TAG_TD, " " + attr + " ", v1);

        return sb;
    }




    /**
     * _more_
     *
     * @param v1 _more_
     *
     * @return _more_
     */
    public static String colRight(String v1) {
        return tag(TAG_TD, " " + attr(ATTR_ALIGN, "right") + " ", v1);
    }


    /**
     * _more_
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String highlightable(String c) {
        return span(c, cssClass("ramadda-highlightable"));
    }


    /**
     * _more_
     *
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String span(String content, String extra) {
        return tag(TAG_SPAN, extra, content);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static Appendable span(Appendable sb, String content,
                                  String extra) {
        tag(sb, TAG_SPAN, extra, content);

        return sb;
    }


    /** _more_ */
    public static boolean debug1 = false;

    /** _more_ */
    public static boolean debug2 = false;

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
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




    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String section(String content) {
        return section(content, null);
    }


    /**
     * _more_
     *
     * @param content _more_
     * @param title _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @return _more_
     */
    public static String sectionOpen() {
        return open(TAG_DIV, "class", "ramadda-section");
    }

    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public static String sectionOpen(String label) {
        return sectionOpen(label, false);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable titleSectionOpen(Appendable sb, String label)
            throws Exception {
        sectionOpen(sb, null, false);
        sectionTitle(sb, label);

        return sb;
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param line _more_
     *
     * @return _more_
     */
    public static String sectionOpen(String label, boolean line) {
        try {
            StringBuilder sb = new StringBuilder();
            sectionOpen(sb, label, line);

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String hrow(List cols) throws Exception {
        Appendable sb = new StringBuilder();
        hrow(sb, cols);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param cols _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable hrow(Appendable sb, List cols) throws Exception {
        for (Object o : cols) {
            sb.append(div(o.toString(),
                          attrs("style",
                                "display:inline-block;vertical-align:top;")));
        }

        return sb;
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param cols _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable hrow(Appendable sb, String... cols)
            throws Exception {
        for (String o : cols) {
            sb.append(div(o.toString(),
                          attrs("style",
                                "display:inline-block;vertical-align:top;")));
        }

        return sb;
    }

    /**
     *
     * @param cols _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static String hrow(String... cols) throws Exception {
        StringBuilder sb = new StringBuilder();
        hrow(sb, cols);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     * @param line _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable sectionOpen(Appendable sb, String label,
                                         boolean line)
            throws Exception {
        open(sb, TAG_DIV, "class", line
                                   ? "ramadda-section"
                                   : "ramadda-section ramadda-section-noline");
        sectionHeader(sb, label);

        return sb;
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable sectionHeader(Appendable sb, String label)
            throws Exception {
        if (Utils.stringDefined(label)) {
            div(sb, label, cssClass("ramadda-page-heading"));
        }

        return sb;
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Appendable sectionTitle(Appendable sb, String label)
            throws Exception {
        if (Utils.stringDefined(label)) {
            div(sb, label, cssClass("ramadda-page-title"));
        }

        return sb;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public static String sectionClose() {
        return close(TAG_DIV);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String note(String s) {
        return div(div(s, cssClass("ramadda-note")),
                   cssClass("ramadda-note-outer"));
    }


    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String div(String content) {
        return div(content, "");
    }

    /**
     * _more_
     *
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String div(String content, String extra) {
        return tag(TAG_DIV, extra, content);
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static Appendable div(Appendable sb, String content,
                                 String extra) {
        tag(sb, TAG_DIV, extra, content);

        return sb;
    }


    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String td(String content) {
        return td(content, "");
    }

    /**
     * _more_
     *
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String td(String content, String extra) {
        return tag(TAG_TD, extra, content);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     *
     * @return _more_
     */
    public static Appendable td(Appendable sb, String content) {
        return td(sb, content, "");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static Appendable td(Appendable sb, String content, String extra) {
        tag(sb, TAG_TD, extra, content);

        return sb;
    }



    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String th(String content) {
        return th(content, "");
    }

    /**
     * _more_
     *
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String th(String content, String extra) {
        return tag(TAG_TH, extra, content);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     *
     * @return _more_
     */
    public static Appendable th(Appendable sb, String content) {
        return th(sb, content, "");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static Appendable th(Appendable sb, String content, String extra) {
        tag(sb, TAG_TH, extra, content);

        return sb;
    }



    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String h1(String content) {
        return tag(TAG_H1, "", content);
    }

    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String h2(String content) {
        return tag(TAG_H2, "", content);
    }

    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String h3(String content) {
        return tag(TAG_H3, "", content);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public static String ul() {
        return open(TAG_UL, "");
    }



    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String p(String content) {
        return tag(TAG_P, "", content);
    }



    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    public static String pre(String content) {
        return pre(content, "");
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param content _more_
     *
     * @throws IOException _more_
     */
    public static void pre(Appendable sb, String content) throws IOException {
        tag(sb, TAG_PRE, "", content);
    }


    /**
     * _more_
     *
     * @param content _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String pre(String content, String attrs) {
        return tag(TAG_PRE, attrs, content);
    }



    /**
     * _more_
     *
     * @param path _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String url(String path, List args) {
        return url(path, Utils.toStringArray(args));
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String url(String path, String... args) {
        return url(path, args, true);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param path _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static Appendable url(Appendable sb, String path, String... args) {
        url(sb, path, args, true);

        return sb;
    }

    /**
     * _more_
     *
     * @param path _more_
     * @param args _more_
     * @param encodeArgs _more_
     *
     * @return _more_
     */
    public static String url(String path, String[] args, boolean encodeArgs) {
        if (args.length == 0) {
            return path;
        }
        StringBuilder sb = new StringBuilder();
        url(sb, path, args, encodeArgs);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param size _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String makeDim(String size, String dflt) {
        if (size == null) {
            return null;
        }
        if ( !size.matches("^[0-9\\.+-]+$")) {
            return size;
        }
        if (dflt != null) {
            return size + dflt;
        }

        return size + "px";
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param path _more_
     * @param args _more_
     * @param encodeArgs _more_
     */
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


    /**
     * _more_
     *
     * @param args _more_
     *
     * @return _more_
     */
    public static String args(String... args) {
        return args(args, true);
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param encodeArgs _more_
     *
     * @return _more_
     */
    public static String args(String[] args, boolean encodeArgs) {
        List<String> a = new ArrayList<String>();
        for (int i = 0; i < args.length; i += 2) {
            a.add(arg(args[i], args[i + 1], encodeArgs));
        }

        return StringUtil.join("&", a);
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param encodeArgs _more_
     *
     * @return _more_
     */
    public static String args(List<String> args, boolean encodeArgs) {
        List<String> a = new ArrayList<String>();
        for (int i = 0; i < args.size(); i += 2) {
            a.add(arg(args.get(i), args.get(i + 1), encodeArgs));
        }

        return StringUtil.join("&", a);
    }


    /**
     * Make the url argument string from the set of given args.
     * If the value of a given arg is a list then add multiple key=value pairs
     *
     * @param args url arguments
     *
     * @return URL argument  string
     */
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


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String arg(String name, String value) {
        return arg(name, value, true);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param encodeArg _more_
     *
     * @return _more_
     */
    public static String arg(Object name, Object value, boolean encodeArg) {
        Appendable sb = new StringBuilder();
        arg(sb, name, value, encodeArg);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param name _more_
     * @param value _more_
     * @param encodeArg _more_
     */
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




    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public static String row(String row) {
        return tag(TAG_TR, "", row);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param row _more_
     */
    public static void row(Appendable sb, String row) {
        tag(sb, TAG_TR, "", row);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param row _more_
     * @param extra _more_
     */
    public static void row(Appendable sb, String row, String extra) {
        tag(sb, TAG_TR, extra, row);
    }



    /**
     * _more_
     *
     * @param row _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String row(String row, String extra) {
        return tag(TAG_TR, extra, row);
    }

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public static String rowTop(String row) {
        return tag(TAG_TR, attr(ATTR_VALIGN, VALUE_TOP), row);
    }

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     */
    public static String rowBottom(String row) {
        return tag(TAG_TR, attr(ATTR_VALIGN, VALUE_BOTTOM), row);
    }


    /**
     * Wrap the args in TD tags
     *
     *
     * @param cols one or more columns
     *
     * @return wrapped columns
     */
    public static String cols(String... cols) {
        StringBuilder sb = new StringBuilder();
        cols(sb, cols);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param cols _more_
     */
    public static void cols(Appendable sb, String... cols) {
        try {
            for (int i = 0; i < cols.length; i++) {
                sb.append(tag(TAG_TD, "", cols[i]));
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param cols _more_
     *
     * @throws Exception _more_
     */
    public static void thead(Appendable sb, Object... cols) throws Exception {
        sb.append("<thead>");
        sb.append("<tr>");
        for (int i = 0; i < cols.length; i++) {
            sb.append(tag(TAG_TH, "", cols[i].toString()));
        }
        sb.append("</tr>");
        sb.append("</thead>");
    }




    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static String headerCols(Object[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            sb.append(cols(HtmlUtils.b(columns[i].toString())));
        }

        return sb.toString();
    }



    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static String cols(Object[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            sb.append(cols(columns[i].toString()));
        }

        return sb.toString();
    }



    /**
     * _more_
     *
     *
     * @param id _more_
     * @param arg _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String makeLatLonInput(String id, String arg,
                                         String value) {
        return makeLatLonInput(id, arg, value, null, "");
    }


    /**
     * _more_
     *
     *
     * @param id _more_
     * @param arg _more_
     * @param value _more_
     * @param tip _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String makeLatLonInput(String id, String arg, String value,
                                         String tip, String extra) {
        return input(arg, value,
                     id(id) + style("margin:0px;") + attrs(ATTR_SIZE, "5")
                     + id(arg) + extra + ((tip != null)
                                          ? title(tip)
                                          : ""));
    }


    /**
     * _more_
     *
     *
     * @param baseId _more_
     * @param baseName _more_
     * @param southValue _more_
     * @param northValue _more_
     * @param eastValue _more_
     * @param westValue _more_
     *
     * @return _more_
     */
    public static String makeLatLonBox(String baseId, String baseName,
                                       String southValue, String northValue,
                                       String eastValue, String westValue) {


        return makeLatLonBox(baseId, baseName + SUFFIX_SOUTH,
                             baseName + SUFFIX_NORTH, baseName + SUFFIX_EAST,
                             baseName + SUFFIX_WEST, southValue, northValue,
                             eastValue, westValue);
    }

    /**
     * _more_
     *
     *
     * @param baseId _more_
     * @param southArg _more_
     * @param northArg _more_
     * @param eastArg _more_
     * @param westArg _more_
     * @param southValue _more_
     * @param northValue _more_
     * @param eastValue _more_
     * @param westValue _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     *
     * @param baseId _more_
     * @param baseName _more_
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public static String makeLatLonBox(String baseId, String baseName,
                                       double south, double north,
                                       double east, double west) {
        return makeLatLonBox(baseId, baseName, toString(south),
                             toString(north), toString(east), toString(west));
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private static String toString(double v) {
        if (v == v) {
            return "" + v;
        }

        return "";
    }

    /**
     * _more_
     *
     * @param south _more_
     * @param north _more_
     * @param east _more_
     * @param west _more_
     *
     * @return _more_
     */
    public static String makeAreaLabel(double south, double north,
                                       double east, double west) {
        return table("<tr><td colspan=\"2\" align=\"center\">"
                     + toString(north) + "</td></tr>" + "<tr><td>"
                     + toString(west) + "</td><td>" + toString(east)
                     + "</tr>" + "<tr><td colspan=\"2\" align=\"center\">"
                     + toString(south));
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String checkbox(String name) {
        return checkbox(name, VALUE_TRUE, false);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String checkbox(String name, String value) {
        return checkbox(name, value, false);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     *
     * @return _more_
     */
    public static String radio(String name, String value, boolean checked) {
        return radio(name, value, checked, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param attrs _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String labeledRadio(String name, String value,
                                      boolean checked, String label) {
        return Utils.concatString(tag(TAG_INPUT, attrs(  /*ATTR_CLASS, CLASS_RADIO,*/
            ATTR_TYPE, TYPE_RADIO, ATTR_NAME, name, ATTR_VALUE,
            value) + (checked
                      ? " checked "
                      : "")), "&nbsp;", label);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     *
     * @return _more_
     */
    public static String checkbox(String name, String value,
                                  boolean checked) {
        return checkbox(name, value, checked, "");
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String labeledCheckbox(String name, String value,
                                         boolean checked, String label) {
        return labeledCheckbox(name, value, checked, "", label);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param attrs _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String labeledCheckbox(String name, String value,
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

        return checkbox(name, value, checked, attrs)
               + /*space(1) +*/ tag("label",
                                    cssClass("ramadda-clickable")
                                    + attr("for", id), label);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String checkbox(String name, String value, boolean checked,
                                  String extra) {
        StringBuilder sb = new StringBuilder();
        checkbox(sb, name, value, checked, extra);

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param name _more_
     * @param value _more_
     * @param checked _more_
     * @param extra _more_
     */
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

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public static String form(String url) {
        return form(url, "");
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String form(String url, String extra) {
        return open(TAG_FORM, attr(ATTR_ACTION, url) + " " + extra);
    }


    /**
     *
     * @param sb _more_
     * @param url _more_
     *
     * @throws Exception _more_
     */
    public static void form(Appendable sb, String url) throws Exception {
        open(sb, TAG_FORM, attr(ATTR_ACTION, url));
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public static String formPost(String url) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url));
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String formPost(String url, String extra) {
        return open(TAG_FORM,
                    attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url) + " "
                    + extra);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String uploadForm(String url, String extra) {
        return open(TAG_FORM,
                    " accept-charset=\"UTF-8\" "
                    + attrs(ATTR_METHOD, VALUE_POST, ATTR_ACTION, url,
                            ATTR_ENCTYPE, VALUE_MULTIPART) + " " + extra);
    }



    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String href(String url, String label) {
        return tag(TAG_A, attrs(ATTR_HREF, url), label);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param url _more_
     * @param label _more_
     */
    public static void href(Appendable sb, String url, String label) {
        tag(sb, TAG_A, attrs(ATTR_HREF, url), label);
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String href(String url, String label, String extra) {
        return tag(TAG_A, Utils.concatString(attrs(ATTR_HREF, url), extra),
                   label);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param url _more_
     * @param label _more_
     * @param extra _more_
     */
    public static void href(Appendable sb, String url, String label,
                            String extra) {
        tag(sb, TAG_A, Utils.concatString(attrs(ATTR_HREF, url), extra),
            label);
    }


    /**
     * _more_
     *
     * @param img _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String submitImage(String img, String name) {
        return submitImage(img, name, "", "");
    }


    /**
     * _more_
     *
     * @param img _more_
     * @param name _more_
     * @param alt _more_
     * @param extra _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param label _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String submit(String label, String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_TYPE, TYPE_SUBMIT, ATTR_VALUE,
                         label, ATTR_CLASS, CLASS_SUBMIT));
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param name _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String submit(String label, String name, String extra) {
        return tag(TAG_INPUT,
                   attrs(ATTR_NAME, name, ATTR_TYPE, TYPE_SUBMIT, ATTR_VALUE,
                         label, ATTR_CLASS, CLASS_SUBMIT) + extra);
    }

    /**
     * _more_
     *
     * @param label _more_
     *
     * @return _more_
     */
    public static String submit(String label) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_SUBMIT, ATTR_TYPE, TYPE_SUBMIT,
                         ATTR_VALUE, label));
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param rows _more_
     * @param columns _more_
     *
     * @return _more_
     */
    public static String textArea(String name, String value, int rows,
                                  int columns) {
        return textArea(name, value, rows, columns, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param rows _more_
     * @param attr _more_
     *
     * @return _more_
     */
    public static String textArea(String name, String value, int rows,
                                  String attr) {
        return textArea(name, value, rows, 0, attr);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param rows _more_
     * @param columns _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String textArea(String name, String value, int rows,
                                  int columns, String extra) {
        String width = (columns > 0)
                       ? attr(ATTR_COLS, "" + columns)
                       : style("width:100%");

        value = value.replaceAll("&", "&amp;");

        return tag(TAG_TEXTAREA,
                   attrs(ATTR_NAME, name, ATTR_CLASS, CLASS_TEXTAREA)
                   + attrs(ATTR_ROWS, "" + rows) + width + extra, value);
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String password(String name) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_PASSWORD, ATTR_TYPE,
                         TYPE_PASSWORD, ATTR_NAME, name));
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String password(String name, String value, String extra) {
        return tag(TAG_INPUT,
                   extra
                   + attrs(ATTR_VALUE, value, ATTR_CLASS, CLASS_PASSWORD,
                           ATTR_TYPE, TYPE_PASSWORD, ATTR_NAME, name));
    }




    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String input(String name) {
        return input(name, null, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String input(String name, Object value) {
        return input(name, value, "");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param size _more_
     *
     * @return _more_
     */
    public static String input(String name, Object value, int size) {
        return input(name, value, attrs(ATTR_SIZE, "" + size));
    }

    /**
     * Create an input field
     *
     * @param name   the name of the input field
     * @param value  the value
     * @param size   size of the input field
     * @param extra  extra attributes
     *
     * @return  an input widget
     */
    public static String input(String name, Object value, int size,
                               String extra) {
        return input(name, value, attrs(ATTR_SIZE, "" + size) + " " + extra);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param extra _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param name _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String fileInput(String name, String extra) {
        return tag(TAG_INPUT,
                   attrs(ATTR_CLASS, CLASS_FILEINPUT, ATTR_TYPE, TYPE_FILE,
                         ATTR_NAME, name) + " " + extra + " " + SIZE_70);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     *
     * @return _more_
     */
    public static String select(String name, List values) {
        return select(name, values, null);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected) {
        return select(name, values, selected, Integer.MAX_VALUE);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected,
                                String extra) {
        return select(name, values, selected, extra, Integer.MAX_VALUE);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String select(String name, List values,
                                List<String> selected, String extra) {
        return select(name, values, selected, extra, Integer.MAX_VALUE);
    }





    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * ,     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, Object[] values,
                                String selected, int maxLength) {
        return select(name, Misc.toList(values), selected, maxLength);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected,
                                int maxLength) {
        return select(name, values, selected, "", maxLength);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String select(String name, List values, String selected,
                                String extra, int maxLength) {
        List selectedList = null;
        //        if ((selected != null) && (selected.length() > 0)) {
        if (selected != null) {
            selectedList = Misc.newList(selected);
        }

        return select(name, values, selectedList, extra, maxLength);
    }

    /**
     * Class Selector _more_
     *
     *
     */
    public static class Selector {

        /** _more_ */
        int margin = 3;

        /** _more_ */
        int padding = 20;

        /** _more_ */
        String label;

        /** _more_ */
        String id;

        /** _more_ */
        String icon;

        /** _more_ */
        boolean isHeader = false;

        /** _more_ */
        private String attr;

        /**  */
        private String tooltip;

        /**
         
         *
         * @param label _more_
         * @param id _more_
         */
        public Selector(String label, String id) {
            this(label, id, null, 3, false);
        }


        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param icon _more_
         */
        public Selector(String label, String id, String icon) {
            this(label, id, icon, 3, false);
        }


        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param icon _more_
         * @param margin _more_
         */
        public Selector(String label, String id, String icon, int margin) {
            this(label, id, icon, margin, false);
        }

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param icon _more_
         * @param margin _more_
         * @param isHeader _more_
         */
        public Selector(String label, String id, String icon, int margin,
                        boolean isHeader) {
            this(label, id, icon, margin, 20, isHeader);
        }

        /**
         * _more_
         *
         * @param label _more_
         * @param id _more_
         * @param icon _more_
         * @param margin _more_
         * @param padding _more_
         * @param isHeader _more_
         */
        public Selector(String label, String id, String icon, int margin,
                        int padding, boolean isHeader) {
            this(label, id, null, icon, margin, padding, isHeader);
        }

        /**
         
         *
         * @param label _more_
         * @param id _more_
         * @param tooltip _more_
         * @param icon _more_
         * @param margin _more_
         * @param padding _more_
         * @param isHeader _more_
         */
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


        /**
         *
         * @param tooltip _more_
         */
        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return this.label;
        }

        /**
         * _more_
         *
         * @param s _more_
         */
        public void setAttr(String s) {
            attr = s;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getId() {
            return id;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getLabel() {
            return label;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getIcon() {
            return icon;
        }

    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
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


    /**
     *
     * @param sb _more_
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     *
     * @throws Exception _more_
     */
    public static void select(Appendable sb, String name, List values,
                              List selected, String extra, int maxLength)
            throws Exception {

        String attrs = "";
        if (extra == null) {
            extra = "";
        }
        if (extra.indexOf(ATTR_CLASS) >= 0) {
            attrs = attrs(ATTR_NAME, name);
        } else {
            String cssClass = null;
            if (extra.indexOf(ATTR_ROWS) >= 0) {
                cssClass = "ramadda-multiselect";
            } else if ((values != null) && !values.isEmpty()
                       && (values.get(0) instanceof HtmlUtils.Selector)) {
                cssClass = "ramadda-pulldown-with-icons";
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


            sb.append("<option ");
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
            if ( !value.equals(label)) {
                attr(sb, ATTR_VALUE, value);
            }
            sb.append(">");
            sb.append(label);
            sb.append("</option>");
        }
        sb.append("</select>");
        sb.append("\n");
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     *
     * @return _more_
     */
    public static String checkboxSelect(String name,
                                      List values, List selected,
                                      String boxStyle, String extra) throws Exception {
        try {
            StringBuilder sb = new StringBuilder();
            checkboxSelect(sb, name, values, selected, boxStyle, extra);

            return sb.toString();
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     *
     * @param sb _more_
     * @param name _more_
     * @param values _more_
     * @param selected _more_
     * @param extra _more_
     * @param maxLength _more_
     * @param boxStyle _more_
     *
     * @throws Exception _more_
     */
    public static void checkboxSelect(Appendable sb, String name,
                                      List values, List selected,
                                      String boxStyle, String extra)
            throws Exception {
        sb.append(open(TAG_DIV, HtmlUtils.style(boxStyle), extra));
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




    /**
     * _more_
     *
     * @param name _more_
     * @param selected _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param html _more_
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String inset(String html, int top, int left, int bottom,
                               int right) {
        return span(html, style(insetStyle(top, left, bottom, right)));
    }


    /**
     * _more_
     *
     * @param html _more_
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String insetDiv(String html, int top, int left, int bottom,
                                  int right) {
        return div(html, style(insetStyle(top, left, bottom, right)));
    }

    /**
     * _more_
     *
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String openInset(int top, int left, int bottom, int right) {
        return open("div", style(insetStyle(top, left, bottom, right)));
    }


    /**
     * _more_
     *
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param top _more_
     * @param left _more_
     * @param bottom _more_
     * @param right _more_
     *
     * @return _more_
     */
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
                : "margin-right:" + top + "px;")));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String endInset() {
        return close(TAG_DIV);
    }


    /**
     * _more_
     *
     * @param html _more_
     * @param space _more_
     *
     * @return _more_
     */
    public static String inset(String html, int space) {
        return div(html, style("margin:" + space + "px;"));
    }



    /**
     * _more_
     *
     * @param html _more_
     * @param space _more_
     *
     * @return _more_
     */
    public static String insetLeft(String html, int space) {
        return div(html, style("margin-left:" + space + "px;"));
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param cols _more_
     *
     * @return _more_
     */
    public static String colspan(String s, int cols) {
        return tag(TAG_TD, attr(ATTR_COLSPAN, "" + cols), s);
    }

    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    public static String formTableTop(String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(formTable());

        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntryTop(cols[i], cols[i + 1]));
        }
        sb.append(formTableClose());

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    public static String formTable(String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append(formTable());
        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntry(cols[i], cols[i + 1]));
        }
        sb.append(formTableClose());

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i += 2) {
            sb.append(formEntryTop(cols[i], cols[i + 1]));
        }

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String leftRight(String left, String right) {
        return leftRight(left, right, "");
    }

    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     * @param attrs _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     * @param attrs _more_
     *
     * @return _more_
     */
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

    /**
     * _more_
     *
     * @param contents _more_
     *
     * @return _more_
     */
    public static String table(String contents) {
        return table(contents, 0, 0);
    }

    /**
     * _more_
     *
     * @param contents _more_
     * @param padding _more_
     * @param spacing _more_
     *
     * @return _more_
     */
    public static String table(String contents, int padding, int spacing) {
        return table(contents,
                     attrs(ATTR_CELLPADDING, "" + padding, ATTR_CELLSPACING,
                           "" + spacing));
    }

    /**
     * _more_
     *
     * @param contents _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String table(String contents, String attrs) {
        return tag(TAG_TABLE, attrs, contents);
    }

    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static String table(Object[] columns) {
        return table(columns, "");
    }


    /**
     * _more_
     *
     * @param columns _more_
     * @param attrs _more_
     *
     * @return _more_
     */
    public static String table(Object[] columns, String attrs) {
        return table(row(cols(columns), ""), attrs);
    }


    /**
     * _more_
     *
     * @param columns _more_
     * @param spacing _more_
     *
     * @return _more_
     */
    public static String table(Object[] columns, int spacing) {
        return table(row(cols(columns), "" /*attr(ATTR_VALIGN, VALUE_TOP)*/),
                     attrs(ATTR_CELLSPACING, "" + spacing));
    }


    /**
     * _more_
     *
     * @param columns _more_
     * @param numCols _more_
     * @param attributes _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @return _more_
     */
    public static String openInset() {
        return open(TAG_DIV, cssClass("inset"));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String closeInset() {
        return close(TAG_DIV);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public static String formTable() {
        return formTable((String) null);
    }

    /**
     *
     * @param fullWidth _more_
     *
     * @return _more_
     */
    public static String formTable(boolean fullWidth) {
        return formTable((String) null, fullWidth);
    }

    /**
     * _more_
     *
     * @param clazz _more_
     *
     * @return _more_
     */
    public static String formTable(String clazz) {
        return formTable(clazz, false);
    }

    /**
     *
     * @param clazz _more_
     * @param fullWidth _more_
     *
     * @return _more_
     */
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




    /**
     * _more_
     *
     * @return _more_
     */
    public static String formTableClose() {
        return close(TAG_TABLE);
    }

    /**
     * _more_
     *
     * @param sb _more_
     */
    public static void formTableClose(Appendable sb) {
        close(sb, TAG_TABLE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static String formClose() {
        return close(TAG_FORM);
    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String formEntry(String left, String right) {
        StringBuilder sb = new StringBuilder();
        formEntry(sb, left, right);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param left _more_
     * @param right _more_
     */
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

    /**
     *
     * @param sb _more_
     * @param left _more_
     */
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


    /**
     * _more_
     *
     * @param sb _more_
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void formEntries(Appendable sb, Object... args)
            throws Exception {
        for (int i = 0; i < args.length; i += 2) {
            sb.append(formEntry(args[i].toString(), args[i + 1].toString()));
        }

    }



    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     * @param rightColSpan _more_
     *
     * @return _more_
     */
    public static String formEntry(String left, String right,
                                   int rightColSpan) {
        return tag(TAG_TR, "",
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                             CLASS_FORMLABEL), left) + tag(TAG_TD,
                                 attr("colspan", "" + rightColSpan), right));

    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     * @param rightColSpan _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String left, String right,
                                      int rightColSpan) {
        return tag(TAG_TR, "",
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                             CLASS_FORMLABEL_TOP), left) + tag(TAG_TD,
                                 attr("colspan", "" + rightColSpan), right));

    }

    /**
     * _more_
     *
     * @param left _more_
     * @param cols _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String left, String right) {
        return formEntryTop(left, right, "", true);
    }


    /**
     * _more_
     *
     * @param left _more_
     * @param right _more_
     * @param trExtra _more_
     * @param dummy _more_
     *
     * @return _more_
     */
    public static String formEntryTop(String left, String right,
                                      String trExtra, boolean dummy) {
        left = div(left, cssClass(CLASS_FORMLABEL_TOP));
        String label = tag(TAG_TD,
                           attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                                 CLASS_FORMLABEL), left);

        // attrs(ATTR_VALIGN, VALUE_TOP)
        return tag(TAG_TR, trExtra, label + tag(TAG_TD, "", right));
    }

    /**
     * _more_
     *
     *
     * @param label _more_
     * @param col1 _more_
     * @param col2 _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attr(String name, String value) {
        StringBuilder sb = new StringBuilder();
        attr(sb, name, value);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     * @param sb _more_
     *
     */
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


    /**
     *  Return a String with n1=&quot;v1&quot n2=&quot;v2&quot.
     *
     * @param args _more_
     *  @return The attr string.
     */
    public static String attrs(String... args) {
        StringBuilder sb = new StringBuilder();
        attrs(sb, args);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param args _more_
     */
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


    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseOver(String call) {
        return attrs(ATTR_ONMOUSEOVER, call);
    }

    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseMove(String call) {
        return attrs(ATTR_ONMOUSEMOVE, call);
    }


    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseOut(String call) {
        return attrs(ATTR_ONMOUSEOUT, call);
    }

    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseUp(String call) {
        return attrs(ATTR_ONMOUSEUP, call);
    }

    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseDown(String call) {
        return attrs(ATTR_ONMOUSEDOWN, call);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param call _more_
     */
    public static void onMouseOut(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEOUT, call);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param call _more_
     */
    public static void onMouseDown(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEDOWN, call);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param call _more_
     */
    public static void onMouseUp(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEUP, call);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param call _more_
     */
    public static void onMouseOver(StringBuilder sb, String call) {
        attrs(sb, ATTR_ONMOUSEOVER, call);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param call _more_
     */
    public static void onMouseClick(Appendable sb, String call) {
        try {
            call = call.replaceAll("\"", "&quot;").replaceAll("\n", " ");
            attrs(sb, ATTR_ONCLICK, call);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param call _more_
     *
     * @return _more_
     */
    public static String onMouseClick(String call) {
        call = call.replaceAll("\"", "&quot;").replaceAll("\n", " ");

        return attrs(ATTR_ONCLICK, call);
    }



    /**
     * _more_
     *
     * @param call _more_
     * @param label _more_
     *
     * @return _more_
     */
    public static String mouseClickHref(String call, String label) {
        return mouseClickHref(call, label, "");
    }

    /**
     * _more_
     *
     * @param call _more_
     * @param label _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String mouseClickHref(String call, String label,
                                        String extra) {
        //        return "<a href=\"javascript:void(0)\" " +onMouseClick(call) +">" +label +"</a>";
        String result = tag(TAG_A,
                            attrs(ATTR_HREF, "javascript:void(0);")
                            + onMouseClick(call) + extra, label);
        //        System.err.println(result);

        return result;
    }


    /**
     *
     * @param sb _more_
     * @param call _more_
     * @param label _more_
     * @param extra _more_
     *
     * @throws Exception _more_
     */
    public static void mouseClickHref(Appendable sb, String call,
                                      String label, String extra)
            throws Exception {
        tag(sb, TAG_A,
            attrs(ATTR_HREF, "javascript:void(0);") + onMouseClick(call)
            + extra, label);
    }



    /**
     * _more_
     *
     * @param events _more_
     * @param content _more_
     *
     * @return _more_
     */
    public static String jsLink(String events, String content) {
        return jsLink(events, content, "");
    }


    /**
     * _more_
     *
     * @param events _more_
     * @param content _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String jsLink(String events, String content, String extra) {
        return tag(TAG_A,
                   attrs(ATTR_HREF, "javascript:noop();") + " " + events
                   + " " + extra, content);
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String anchorName(String name) {
        return tag(TAG_A, attrs(ATTR_NAME, name), "");
    }


    /**
     * Enclose JavaScript code in a JavaScript tag
     *
     * @param s  the JavaSript code
     *
     * @return  the script enclosed in a tag
     */
    public static String script(String s) {
        StringBuilder js = new StringBuilder();
        script(js, s);

        return js.toString();
    }

    /**
     * _more_
     *
     * @param js _more_
     * @param s _more_
     */
    public static void script(Appendable js, String s) {
        s = s.trim();
        if (s.length() == 0) {
            return;
        }
        try {
            js.append("\n");
            js.append(tag(TAG_SCRIPT, attrs(ATTR_TYPE, "text/JavaScript"),
                          "\n" + s + "\n"));
            js.append("\n");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }


    /**
     * _more_
     *
     * @param js _more_
     * @param args _more_
     */
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






    /**
     * _more_
     * as(ttr
     * @param function _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String call(String function, String... args) {
        StringBuilder sb = new StringBuilder(function);
        call(sb, function, args);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param function _more_
     * @param args _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param function _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String callln(String function, String args) {
        return Utils.concatString(function, "(", args, ");\n");
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param function _more_
     * @param args _more_
     */
    public static void callln(Appendable sb, String function, String args) {
        Utils.concatBuff(sb, function, "(", args, ");\n");
    }

    /**
     * _more_
     *
     * @param jsUrl _more_
     *
     * @return _more_
     */
    public static String importJS(String jsUrl) {
        StringBuilder sb = new StringBuilder("\n");
        importJS(sb, jsUrl);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param jsUrl _more_
     */
    public static void importJS(Appendable sb, String jsUrl) {
        tag(sb, TAG_SCRIPT,
            attrs(ATTR_SRC, jsUrl, ATTR_TYPE, "text/JavaScript"), "");
    }

    /**
     *
     * @param sb _more_
     * @param jsUrl _more_
     * @param integrity _more_
     */
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


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public static String cssLink(String url) {
        return tag(TAG_LINK,
                   attrs(ATTR_HREF, url, ATTR_REL, "stylesheet", ATTR_TYPE,
                         "text/css"));
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param url _more_
     *
     * @throws IOException _more_
     */
    public static void cssLink(Appendable sb, String url) throws IOException {
        tag(sb, TAG_LINK,
            attrs(ATTR_HREF, url, ATTR_REL, "stylesheet", ATTR_TYPE,
                  "text/css"));
    }


    /**
     *
     * @param sb _more_
     * @param url _more_
     *
     * @throws IOException _more_
     */
    public static void cssPreloadLink(Appendable sb, String url)
            throws IOException {
        String template =
            "<link rel='preload' href='file.css' as='style' onload=\"this.onload=null;this.rel='stylesheet'\">\n";
        //\n<noscript><link rel='stylesheet' href='file.css'></noscript>\n";
        sb.append(template.replace("file.css", url));
    }



    /**
     * _more_
     *
     * @param css _more_
     *
     * @return _more_
     */
    public static String cssBlock(String css) {
        return importCss(css);
    }

    /**
     * _more_
     *
     * @param css _more_
     *
     * @return _more_
     */
    public static String importCss(String css) {
        return tag(TAG_STYLE, " type='text/css' ", css);
    }


    /** _more_ */
    private static String blockHideImageUrl;

    /** _more_ */
    private static String blockShowImageUrl;


    /** _more_ */
    private static String inlineHideImageUrl;

    /** _more_ */
    private static String inlineShowImageUrl;

    /**
     * _more_
     *
     * @param hideImg _more_
     * @param showImg _more_
     */
    public static void setBlockHideShowImage(String hideImg, String showImg) {
        if (blockHideImageUrl == null) {
            blockHideImageUrl = hideImg;
            blockShowImageUrl = showImg;
        }
    }



    /**
     * _more_
     *
     * @param hideImg _more_
     * @param showImg _more_
     */
    public static void setInlineHideShowImage(String hideImg,
            String showImg) {
        if (inlineHideImageUrl == null) {
            inlineHideImageUrl = hideImg;
            inlineShowImageUrl = showImg;
        }
    }




    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible) {
        return makeShowHideBlock(
            label, content, visible,
            cssClass("toggleblocklabel ramadda-clickable"));
    }

    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra) {
        return HtmlUtils.makeShowHideBlock(label, content, visible,
                                           headerExtra,
                                           HtmlUtils.cssClass(CLASS_BLOCK),
                                           blockHideImageUrl,
                                           blockShowImageUrl);
    }





    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     * @param blockExtra _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra) {
        return HtmlUtils.makeShowHideBlock(label, content, visible,
                                           headerExtra, blockExtra,
                                           blockHideImageUrl,
                                           blockShowImageUrl);
    }





    /** _more_ */
    public static int blockCnt = 0;


    /**
     * _more_
     *
     * @param prefix _more_
     *
     * @return _more_
     */
    public static String getUniqueId(String prefix) {
        return prefix + (blockCnt++);
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     * @param blockExtra _more_
     * @param hideImg _more_
     * @param showImg _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String label, String content,
                                           boolean visible,
                                           String headerExtra,
                                           String blockExtra, String hideImg,
                                           String showImg) {
        String        id  = "block_" + (blockCnt++);
        StringBuilder sb  = new StringBuilder();
        String        img = "";
        //        System.err.println ("show image:" + showImg);
        if ((showImg != null) && (showImg.length() > 0)) {
            img = span(HtmlUtils.img(visible
                                     ? hideImg
                                     : showImg, "",
                                     " align=bottom"), HtmlUtils.id(id
                                     + "img"));
        }
        String mouseEvent = HtmlUtils.onMouseClick("toggleBlockVisibility('"
                                + id + "','" + id + "img','" + hideImg
                                + "','" + showImg + "')");
        String link = img + space(1) + label;
        sb.append("<div  " + blockExtra + ">");
        sb.append(HtmlUtils.div(link, headerExtra + mouseEvent));
        sb.append("<div " + HtmlUtils.cssClass("hideshowblock")
                  + HtmlUtils.id(id)
                  + HtmlUtils.style("display:block;visibility:visible")
                  + ">");
        if ( !visible) {
            HtmlUtils.script(sb,
                             HtmlUtils.call("hide", HtmlUtils.squote(id)));
        }

        sb.append(content.toString());
        sb.append(close(TAG_DIV));
        sb.append(close(TAG_DIV));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param label _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String[] getToggle(String label, boolean visible) {
        return getToggle(label, visible, blockHideImageUrl,
                         blockShowImageUrl);
    }



    /**
     * _more_
     *
     * @param label _more_
     * @param visible _more_
     * @param hideImg _more_
     * @param showImg _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param content _more_
     * @param contentSB _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String makeToggleBlock(String content,
                                         StringBuilder contentSB,
                                         boolean visible) {
        String        id  = getUniqueId("block_");
        StringBuilder sb  = contentSB;
        String        img = "";
        String js = HtmlUtils.onMouseClick(call("toggleBlockVisibility",
                        Utils.concatString(squote(id), ",",
                                           squote(id + "img"), ",",
                                           squote(""), ",", squote(""))));

        open(sb, TAG_DIV, HtmlUtils.cssClass("hideshowblock"),
             HtmlUtils.id(id),
             HtmlUtils.style("display:block;visibility:visible"));
        if ( !visible) {
            HtmlUtils.script(sb,
                             HtmlUtils.call("hide", HtmlUtils.squote(id)));
        }
        sb.append(content);
        sb.append(close(TAG_DIV));

        return js;
    }





    /**
     * _more_
     *
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String makeToggleInline(String label, String content,
                                          boolean visible) {

        StringBuilder sb = new StringBuilder();
        makeToggleInline(sb, label, content, visible);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     */
    public static void makeToggleInline(Appendable sb, String label,
                                        String content, boolean visible) {

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
            sb.append(link);
            open(sb, TAG_SPAN, "class", "hideshowblock", "id", id, "style",
                 "display:inline;visibility:visible");
            if ( !visible) {
                HtmlUtils.script(sb,
                                 HtmlUtils.call("hide",
                                     HtmlUtils.squote(id)));
            }
            sb.append(content);
            sb.append(close(TAG_SPAN));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }




    /**
     * _more_
     *
     * @param clickHtml _more_
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public static String makeShowHideBlock(String clickHtml, String label,
                                           String content, boolean visible) {
        String        id = "block_" + (blockCnt++);
        StringBuilder sb = new StringBuilder();
        String mouseEvent = HtmlUtils.onMouseClick("toggleBlockVisibility('"
                                + id + "','" + id + "img','" + "" + "','"
                                + "" + "')");
        String link = HtmlUtils.jsLink(
                          mouseEvent, clickHtml,
                          HtmlUtils.cssClass("toggleblocklabellink")) + label;
        sb.append(link);
        open(sb, TAG_SPAN, "class", "hideshowblock", "id", id, "style",
             "display:block;visibility:visible");
        if ( !visible) {
            HtmlUtils.script(sb,
                             HtmlUtils.call("hide", HtmlUtils.squote(id)));
        }

        sb.append(content.toString());
        sb.append(close(TAG_SPAN));

        return sb.toString();
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param andSquote _more_
     *
     * @return _more_
     */
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






    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String getString(boolean v) {
        return "" + v;
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String getString2(boolean v) {
        return Boolean.toString(v);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        System.err.println(labeledCheckbox("name", "value", false, null,
                                           "label"));
        System.err.println(labeledCheckbox("name", "value", false, "",
                                           "label"));
        System.err.println(labeledCheckbox("name", "value", false, "foo=bar",
                                           "label"));
        System.err.println(labeledCheckbox("name", "value", false, "foo=bar",
                                           "label"));
        System.err.println(labeledCheckbox("name", "value", false, "id='ID'",
                                           "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id  ='ID'", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id  =   'ID'", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id=   'ID'", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id=\"ID\"", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id=\"ID\"", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id  =\"ID\"", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id  =   \"ID\"", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id=   \"ID\"", "label"));
        System.err.println(labeledCheckbox("name", "value", false,
                                           "id=\"ID\"", "label"));


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

    /**
     * _more_
     *
     * @param size _more_
     *
     * @return _more_
     */
    public static final String sizeAttr(int size) {
        return attr("size", "" + size);
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param title _more_
     * @param contents _more_
     *
     * @throws Exception _more_
     */
    public static void makeAccordion(Appendable sb, String title,
                                     String contents)
            throws Exception {

        makeAccordion(sb, title, contents, null, null);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param title _more_
     * @param contents _more_
     * @param wrapperClass _more_
     * @param headerClass _more_
     *
     * @throws Exception _more_
     */
    public static void makeAccordion(Appendable sb, String title,
                                     String contents, String wrapperClass,
                                     String headerClass)
            throws Exception {
        makeAccordion(sb, title, contents, true, wrapperClass, headerClass);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param title _more_
     * @param contents _more_
     * @param collapse _more_
     * @param wrapperClass _more_
     * @param headerClass _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * Add an accordion of sections to the page
     *
     * @param sb        the StringBuilder/StringBuilder to append to
     * @param titles    the title for each section
     * @param contents  the contents of each section
     *
     * @throws Exception  some problem
     */
    public static void makeAccordion(Appendable sb, List titles,
                                     List contents)
            throws Exception {
        makeAccordion(sb, titles, contents, false, null, null);
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param titles _more_
     * @param contents _more_
     * @param collapse _more_
     *
     * @throws Exception _more_
     */
    public static void makeAccordion(Appendable sb, List titles,
                                     List contents, boolean collapse)
            throws Exception {
        makeAccordion(sb, titles, contents, collapse, null, null);
    }



    /**
     * Add an accordion of sections to the page
     *
     * @param sb        the StringBuilder/StringBuilder to append to
     * @param titles    the title for each section
     * @param contents  the contents of each section
     * @param collapse  set the sections to be collapsed initially
     * @param wrapperClass _more_
     * @param headerClass _more_
     *
     * @throws Exception  some problem
     */
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



    /**
     * _more_
     *
     * @param sb _more_
     * @param icon _more_
     * @param msg _more_
     *
     * @throws Exception _more_
     */
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


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String comment(String s) {
        Appendable sb = Utils.makeAppendable();
        comment(sb, s);

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param s _more_
     */
    public static void comment(Appendable sb, String s) {
        try {
            s = s.replaceAll("\n", " ");
            Utils.concatBuff(sb, "\n<!-- ", s, " -->\n");
        } catch (Exception exc) {
            throw new IllegalArgumentException(exc);
        }
    }


    /**
     * This takes the  given String and tries to convert it to a color.
     * The string may be a space or comma separated triple of RGB integer
     * values. It may be an integer or it may be a color name defined in
     * the COLORNAMES array
     *
     * @param value String value
     * @param dflt This is returned if the value cannot be converted
     * @return Color defined by the String value or the dflt
     */
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


    /**
     * _more_
     *
     * @param url _more_
     * @param linkPattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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

    /**
     * _more_
     *
     * @param url _more_
     * @param html _more_
     * @param linkPattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<Link> extractLinks(URL url, String html,
                                          String linkPattern)
            throws Exception {
        return extractLinks(url, html, linkPattern, false);
    }


    /**
     *
     * @param url _more_
     * @param html _more_
     * @param linkPattern _more_
     * @param images _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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


    /**
     * _more_
     *
     * @param url _more_
     * @param linkPattern _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
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





    /**
     * Class description
     *
     *
     * @version        $version$, Sat, Nov 29, '14
     * @author         Enter your name here...
     */
    public static class Link {

        /**  */
        private String link;

        /** _more_ */
        private URL url;

        /** _more_ */
        private String label;

        /** _more_ */
        private long size = -1;

        /**
         * _more_
         *
         *
         * @param link _more_
         * @param url _more_
         * @param label _more_
         * @param size _more_
         */
        public Link(String link, URL url, String label, long size) {
            this(link, url, label);
            this.size = size;
        }

        /**
         * _more_
         *
         *
         * @param link _more_
         * @param url _more_
         * @param label _more_
         */
        public Link(String link, URL url, String label) {
            this.link  = link;
            this.url   = url;
            this.label = label;
        }

        /**
         *
         * @param pattern _more_
         *
         * @return _more_
         */
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

        /**
         *  Set the Url property.
         *
         *  @param value The new value for Url
         */
        public void setUrl(URL value) {
            url = value;
        }

        /**
         *
         * @return _more_
         */
        public String getLink() {
            return link;
        }


        /**
         *  Get the Url property.
         *
         *  @return The Url
         */
        public URL getUrl() {
            return url;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        @Override
        public int hashCode() {
            return url.hashCode();
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
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

        /**
         *  Set the Label property.
         *
         *  @param value The new value for Label
         */
        public void setLabel(String value) {
            label = value;
        }

        /**
         *  Get the Label property.
         *
         *  @return The Label
         */
        public String getLabel() {
            return label;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getHref() {
            return HtmlUtils.href(this.url.toString(), this.label);
        }

        /**
         *  Set the Size property.
         *
         *  @param value The new value for Size
         */
        public void setSize(long value) {
            size = value;
        }

        /**
         *  Get the Size property.
         *
         *  @return The Size
         */
        public long getSize() {
            return size;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return url + " " + label;
        }

    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public static String getAudioEmbed(String url) {
        String html =
            "<audio controls preload=\"none\" style=\"width:480px;\">\n <source src=\"${url}\" type=\"${mime}\" />\n <p>Your browser does not support HTML5 audio.</p>\n </audio>";

        String mime = "audio/wav";
        String ext  = IOUtil.getFileExtension(url);
        if (ext.equals("ogg")) {
            mime = "audio/ogg";
        } else if (ext.equals("oga")) {
            mime = "audio/ogg";
        } else if (ext.equals("wav")) {
            mime = "audio/wav";
        } else if (ext.equals("m4a")) {
            mime = "audio/mp4";
        } else if (ext.equals("mp4")) {
            mime = "audio/mp4";
        }

        html = html.replace("${url}", url);
        html = html.replace("${mime}", mime);

        return html;
    }


    //unescape

    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     */
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

    /** _more_ */
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

    /** _more_ */
    private static final int MIN_ESCAPE = 2;

    /** _more_ */
    private static final int MAX_ESCAPE = 6;

    /** _more_ */
    private static final HashMap<String, CharSequence> lookupMap;
    static {
        lookupMap = new HashMap<String, CharSequence>();
        for (final CharSequence[] seq : ESCAPES) {
            lookupMap.put(seq[1].toString(), seq[0]);
        }
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String sanitizeString(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        return s;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(urlEncodeSpace(s), "UTF-8");
        } catch (Exception exc) {
            System.err.println("error encoding arg(3):" + s + " " + exc);
            exc.printStackTrace();

            return "";
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception exc) {
            System.err.println("error decoding:" + s + " " + exc);
            exc.printStackTrace();

            return "";
        }
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String urlEncodeSpace(String s) {
        return s.replaceAll(" ", "+");
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param input _more_
     *
     * @return _more_
     */
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

    /**
     *
     * @param input _more_
     *
     * @return _more_
     */
    public static String entityDecode(String input) {
        return StringEscapeUtils.unescapeHtml3(input);
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String sanitizeArg(String s) {
        if (s == null) {
            return null;
        }

        return sanitizeString(s).replaceAll("\"", "_");
    }


    /**
     *
     * @param link _more_
     * @param args _more_
     *
     * @return _more_
     */
    public static String[] makePopupLink(String link, NamedValue... args) {
        String compId         = "menu_" + HtmlUtils.blockCnt++;
        String linkId         = "menulink_" + HtmlUtils.blockCnt++;
        String linkAttributes = "";
        List<String> attrs = (List<String>) Utils.makeList("contentId",
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


    /**
     *
     * @param popup _more_
     * @param link _more_
     * @param menuContents _more_
     * @param args _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param contents _more_
     * @param compId _more_
     * @param makeClose _more_
     * @param header _more_
     *
     * @return _more_
     */
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



    /**
     *
     * @param front _more_
     * @param back _more_
     * @param flipCardAttrs _more_
     * @param frontAttrs _more_
     * @param backAttrs _more_
      * @return _more_
     */
    public static String makeFlipCard(String front, String back,
                                      String flipCardAttrs,
                                      String frontAttrs, String backAttrs) {
        return "<div class='ramadda-flip-card ' " + flipCardAttrs
               + "> <div class='ramadda-flip-card-inner'>"
               + "<div class='ramadda-flip-card-front' " + frontAttrs + ">"
               + front + "</div><div class='ramadda-flip-card-back' "
               + backAttrs + ">" + back + "</div></div></div>";
    }



}

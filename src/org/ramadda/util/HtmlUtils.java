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

// $Id: StringUtil.java,v 1.53 2007/06/01 17:02:44 jeffmc Exp $


package org.ramadda.util;


import org.apache.commons.net.ftp.*;

import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.awt.Color;

import java.io.IOException;

import java.lang.reflect.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.*;




/**
 */

public class HtmlUtils {

    /** Used to map named colors to color */
    public static final Color[] COLORS = {
        Color.blue, Color.black, Color.red, Color.gray, Color.lightGray,
        Color.white, Color.green, Color.orange, Color.cyan, Color.magenta,
        Color.pink, Color.yellow
    };

    /** Used to map named colors to color */
    public static final String[] COLORNAMES = {
        "blue", "black", "red", "gray", "light gray", "white", "green",
        "orange", "cyan", "magenta", "pink", "yellow"
    };


    //j-

    /** _more_ */
    public static final String HTTP_USER_AGENT = "User-Agent";

    /** _more_ */
    public static final String HTTP_CONTENT_LENGTH = "Content-Length";

    /** _more_ */
    public static final String HTTP_CONTENT_DESCRIPTION =
        "Content-Description";

    /** _more_ */
    public static final String HTTP_WWW_AUTHENTICATE = "WWW-Authenticate";

    /** _more_ */
    public static final String HTTP_SET_COOKIE = "Set-Cookie";


    /** _more_ */
    public static final String SUFFIX_NORTH = "_north";

    /** _more_ */
    public static final String SUFFIX_SOUTH = "_south";

    /** _more_ */
    public static final String SUFFIX_EAST = "_east";

    /** _more_ */
    public static final String SUFFIX_WEST = "_west";



    /** _more_ */
    public static final String SIZE_3 = "  size=\"3\" ";

    /** _more_ */
    public static final String SIZE_4 = "  size=\"4\" ";

    /** _more_ */
    public static final String SIZE_5 = "  size=\"5\" ";

    /** _more_ */
    public static final String SIZE_6 = "  size=\"6\" ";

    /** _more_ */
    public static final String SIZE_7 = "  size=\"7\" ";

    /** _more_ */
    public static final String SIZE_8 = "  size=\"8\" ";

    /** _more_ */
    public static final String SIZE_9 = "  size=\"9\" ";

    /** _more_ */
    public static final String SIZE_10 = "  size=\"10\" ";

    /** _more_ */
    public static final String SIZE_20 = "  size=\"20\" ";

    /** _more_ */
    public static final String SIZE_15 = "  size=\"15\" ";

    /** _more_ */
    public static final String SIZE_25 = "  size=\"25\" ";

    /** _more_ */
    public static final String SIZE_30 = "  size=\"30\" ";

    /** _more_ */
    public static final String SIZE_40 = "  size=\"40\" ";

    /** _more_ */
    public static final String SIZE_50 = "  size=\"50\" ";

    /** _more_ */
    public static final String SIZE_60 = "  size=\"60\" ";

    /** _more_ */
    public static final String SIZE_70 = "  size=\"70\" ";

    /** _more_ */
    public static final String SIZE_80 = "  size=\"80\" ";

    /** _more_ */
    public static final String SIZE_90 = "  size=\"90\" ";

    /** _more_ */
    public static final String ENTITY_NBSP = "&nbsp;";

    /** _more_ */
    public static final String ENTITY_NBSP2 = "&nbsp;&nbsp;";

    /** _more_ */
    public static final String ENTITY_NBSP3 = "&nbsp;&nbsp;&nbsp;";

    /** _more_ */
    public static final String ENTITY_NBSP4 = "&nbsp;&nbsp;&nbsp;&nbsp;";

    /** _more_ */
    public static final String ENTITY_GT = "&gt;";

    /** _more_ */
    public static final String ENTITY_LT = "&lt;";

    /** _more_ */
    public static final String TAG_A = "a";

    /** _more_ */
    public static final String TAG_B = "b";


    /** _more_ */
    public static final String TAG_BR = "br";

    /** _more_ */
    public static final String TAG_CENTER = "center";

    /** _more_ */
    public static final String TAG_DIV = "div";

    /** _more_ */
    public static final String TAG_EMBED = "embed";

    /** _more_ */
    public static final String TAG_OBJECT = "object";

    /** _more_ */
    public static final String TAG_FORM = "form";

    /** _more_ */
    public static final String TAG_HR = "hr";

    /** _more_ */
    public static final String TAG_H1 = "h1";

    /** _more_ */
    public static final String TAG_H2 = "h2";

    /** _more_ */
    public static final String TAG_H3 = "h3";

    /** _more_ */
    public static final String TAG_I = "i";

    /** _more_ */
    public static final String TAG_IMG = "img";

    /** _more_ */
    public static final String TAG_INPUT = "input";

    /** _more_ */
    public static final String TAG_IFRAME = "iframe";

    /** _more_ */
    public static final String TAG_LI = "li";

    /** _more_ */
    public static final String TAG_LINK = "link";



    /** _more_ */
    public static final String TAG_NOBR = "nobr";

    /** _more_ */
    public static final String TAG_OPTION = "option";

    /** _more_ */
    public static final String TAG_P = "p";

    /** _more_ */
    public static final String TAG_PRE = "pre";

    /** _more_ */
    public static final String TAG_SCRIPT = "script";

    /** _more_ */
    public static final String TAG_STYLE = "style";

    /** _more_ */
    public static final String TAG_SPAN = "span";

    /** _more_ */
    public static final String TAG_SELECT = "select";

    /** _more_ */
    public static final String TAG_TABLE = "table";

    /** _more_ */
    public static final String TAG_TD = "td";

    /** _more_ */
    public static final String TAG_TH = "th";

    /** _more_ */
    public static final String TAG_TR = "tr";

    /** _more_ */
    public static final String TAG_TEXTAREA = "textarea";

    /** _more_ */
    public static final String TAG_UL = "ul";


    /** _more_ */
    public static final String ATTR_ACTION = "action";

    /** _more_ */
    public static final String ATTR_ALIGN = "align";

    /** _more_ */
    public static final String ATTR_ALT = "alt";

    /** _more_ */
    public static final String ATTR_BORDER = "border";

    /** _more_ */
    public static final String ATTR_BGCOLOR = "bgcolor";

    /** _more_ */
    public static final String ATTR_CELLSPACING = "cellspacing";

    /** _more_ */
    public static final String ATTR_CELLPADDING = "cellpadding";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_COLS = "cols";

    /** _more_ */
    public static final String ATTR_COLSPAN = "colspan";

    /** _more_ */
    public static final String ATTR_ENCTYPE = "enctype";

    /** _more_ */
    public static final String ATTR_HREF = "href";

    /** _more_ */
    public static final String ATTR_HEIGHT = "height";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_METHOD = "method";

    /** _more_ */
    public static final String ATTR_MULTIPLE = "multiple";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_ONMOUSEMOVE = "onmousemove";

    /** _more_ */
    public static final String ATTR_ONMOUSEOVER = "onmouseover";

    /** _more_ */
    public static final String ATTR_ONMOUSEUP = "onmouseup";

    /** _more_ */
    public static final String ATTR_ONMOUSEOUT = "onmouseout";

    /** _more_ */
    public static final String ATTR_ONMOUSEDOWN = "onmousedown";

    /** _more_ */
    public static final String ATTR_ONCLICK = "onClick";

    /** _more_ */
    public static final String ATTR_ONCHANGE = "onchange";

    /** _more_ */
    public static final String ATTR_READONLY = "READONLY";

    /** _more_ */
    public static final String ATTR_REL = "rel";

    /** _more_ */
    public static final String ATTR_ROWS = "rows";

    /** _more_ */
    public static final String ATTR_SELECTED = "selected";

    /** _more_ */
    public static final String ATTR_SIZE = "size";

    /** _more_ */
    public static final String ATTR_SRC = "src";

    /** _more_ */
    public static final String ATTR_STYLE = "style";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_TARGET = "target";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_VALIGN = "valign";

    /** _more_ */
    public static final String ATTR_WIDTH = "width";

    /** _more_ */
    public static final String CLASS_BLOCK = "block";

    /** _more_ */
    public static final String CLASS_CHECKBOX = "checkbox";

    /** _more_ */
    public static final String CLASS_DISABLEDINPUT = "disabledinput";

    /** _more_ */
    public static final String CLASS_ERRORLABEL = "errorlabel";

    /** _more_ */
    public static final String CLASS_FILEINPUT = "fileinput";

    /** _more_ */

    public static final String CLASS_FORMLABEL = "formlabel";

    /** _more_ */
    public static final String CLASS_FORMCONTENTS = "formcontents";

    /** _more_ */
    public static final String CLASS_FORMLABEL_TOP = "formlabeltop";

    /** _more_ */
    public static final String CLASS_HIDDENINPUT = "hiddeninput";

    /** _more_ */
    public static final String CLASS_INPUT = "input";

    /** _more_ */
    public static final String CLASS_PASSWORD = "password";

    /** _more_ */
    public static final String CLASS_RADIO = "radio";

    /** _more_ */
    public static final String CLASS_SELECT = "select";

    /** _more_ */
    public static final String CLASS_SUBMIT = "submit";

    /** _more_ */
    public static final String CLASS_SUBMITIMAGE = "submitimage";

    /** _more_ */
    public static final String CLASS_TAB_CONTENT = "tab_content";

    /** _more_ */
    public static final String CLASS_TAB_CONTENTS = "tab_contents";

    /** _more_ */
    public static final String CLASS_TEXTAREA = "textarea";

    /** _more_ */
    public static final String STYLE_HIDDEN = "display:none;";


    /** _more_ */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_PASSWORD = "password";

    /** _more_ */
    public static final String TYPE_SUBMIT = "submit";

    /** _more_ */
    public static final String TYPE_IMAGE = "image";

    /** _more_ */
    public static final String TYPE_RADIO = "radio";

    /** _more_ */
    public static final String TYPE_INPUT = "input";

    /** _more_ */
    public static final String TYPE_TEXTAREA = "textarea";

    /** _more_ */
    public static final String TYPE_CHECKBOX = "checkbox";

    /** _more_ */
    public static final String TYPE_HIDDEN = "hidden";



    /** _more_ */
    public static final String VALUE_BOTTOM = "bottom";

    /** _more_ */
    public static final String VALUE_CENTER = "center";

    /** _more_ */
    public static final String VALUE_FALSE = "false";

    /** _more_ */
    public static final String VALUE_LEFT = "left";

    /** _more_ */
    public static final String VALUE_MULTIPART = "multipart/form-data";

    /** _more_ */
    public static final String VALUE_POST = "post";

    /** _more_ */
    public static final String VALUE_RIGHT = "right";

    /** _more_ */
    public static final String VALUE_SELECTED = "selected";

    /** _more_ */
    public static final String VALUE_TOP = "top";

    /** _more_ */
    public static final String VALUE_TRUE = "true";

    //j+


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
                sb.append("</");
                sb.append(comp);
                sb.append(">");
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
            open(sb, tag, attrs);
            sb.append(inner);
            close(sb, tag);
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
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public static String hbox(String s1, String s2) {
        return tag(TAG_TABLE,
                   attrs(ATTR_CELLSPACING, "0", ATTR_CELLPADDING, "0"),
                   HtmlUtils.rowTop(HtmlUtils.cols(s1, s2)));
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
            s = s.replaceAll("\"", "\\\"");
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
     * @param title _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String img(String path, String title, String extra) {
        if (title.length() > 0) {
            return tag(TAG_IMG,
                       attrs(ATTR_BORDER, "0", ATTR_SRC, path, ATTR_TITLE,
                             title, ATTR_ALT, title) + " " + extra);
        }
        String img = tag(TAG_IMG,
                         attrs(ATTR_BORDER, "0", ATTR_SRC, path) + " "
                         + extra);

        return img;
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
     *
     * @param c _more_
     *
     * @return _more_
     */
    public static String style(String c) {
        return attr(ATTR_STYLE, c);
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
    public static String th(String v1) {
        return th(v1, "");
    }

    /**
     * _more_
     *
     * @param v1 _more_
     * @param attr _more_
     *
     * @return _more_
     */
    public static String th(String v1, String attr) {
        return tag(TAG_TH, " " + attr + " ", v1);
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
            sb.append(name.toString());
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
        return makeLatLonInput(id, arg, value, null);
    }


    /**
     * _more_
     *
     *
     * @param id _more_
     * @param arg _more_
     * @param value _more_
     * @param tip _more_
     *
     * @return _more_
     */
    public static String makeLatLonInput(String id, String arg, String value,
                                         String tip) {
        return input(arg, value,
                     id(id) + style("margin:0px;") + attrs(ATTR_SIZE, "5")
                     + id(arg) + ((tip != null)
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
               + makeLatLonInput(
                   baseId + SUFFIX_NORTH, northArg, northValue,
                   "North") + "</td></tr>" + "<tr><td>"
                            + makeLatLonInput(
                                baseId + SUFFIX_WEST, westArg, westValue,
                                "West") + "</td><td>"
                                        + makeLatLonInput(
                                            baseId + SUFFIX_EAST, eastArg,
                                            eastValue, "East") + "</tr>"
                                                + "<tr><td colspan=\"2\" align=\"center\">"
                                                    + makeLatLonInput(
                                                        baseId + SUFFIX_SOUTH,
                                                            southArg,
                                                                southValue,
                                                                    "South") + "</table>";
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
        return radio(name, value, checked,"");
    }

    public static String radio(String name, String value, boolean checked, String attrs) {
        return tag(TAG_INPUT,
                   attrs + attrs( /*ATTR_CLASS, CLASS_RADIO,*/ATTR_TYPE, TYPE_RADIO,
                       ATTR_NAME, name, ATTR_VALUE, value) + (checked
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
        return checkbox(name, value, checked, attrs) + space(2) + label;
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
            if (extra.length() > 0) {
                sb.append(extra);
            }
            attrs(sb, ATTR_TYPE, TYPE_CHECKBOX, ATTR_NAME, name, ATTR_VALUE,
                  value);
            if (checked) {
                sb.append(" checked ");
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
     * @param columns _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public static String textArea(String name, String value, int rows,
                                  int columns, String extra) {
        return tag(TAG_TEXTAREA,
                   attrs(ATTR_NAME, name, ATTR_CLASS, CLASS_TEXTAREA)
                   + attrs(ATTR_ROWS, "" + rows, ATTR_COLS, "" + columns)
                   + extra, value);
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
     * @author IDV Development Team
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
            this.label    = label;
            this.id       = id;
            this.icon     = icon;
            this.margin   = margin;
            this.padding  = padding;
            this.isHeader = isHeader;
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
        StringBuilder sb = new StringBuilder();
        String        attrs;


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
            StringBuilder attrSB = new StringBuilder();
            Object        obj    = values.get(i);
            String        value;
            String        label;
            String        extraAttr = "";
            if (obj instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject) obj;
                value = tfo.getId().toString();
                label = tfo.toString();
            } else if (obj instanceof Selector) {
                Selector selector = (Selector) obj;
                value = selector.id;
                label = selector.label;
                if (selector.attr != null) {
                    attrSB.append(selector.attr);
                }
                if (selector.icon != null) {
                    /* Firefox only
                    extraAttr = style(
                        "margin:4px;margin-left:" + selector.margin
                        + "px;padding-left:" + selector.padding
                        + "px;padding-bottom:1px;padding-top:3px;background-repeat:no-repeat; background-image: url("
                        + selector.icon + ");");
                    */
                    extraAttr = attrs("data-class", "ramadda-select-icon",
                                      "data-style",
                                      "width:" + selector.padding
                                      + "px;margin-right:" + selector.margin
                                      + "px", "img-src", selector.icon);
                } else if (selector.isHeader) {
                    extraAttr = style(
                        "font-weight:bold;background: #ddd;padding:6px;");
                }
            } else {
                value = label = obj.toString();
            }

            String selectedAttr = "";
            if ((selected != null)
                    && (selected.contains(value) || selected.contains(obj))) {
                if ( !seenSelected.contains(value)) {
                    selectedAttr = attrs(ATTR_SELECTED, VALUE_SELECTED);
                    seenSelected.add(value);
                }
            }
            if (label.length() > maxLength) {
                label = "..." + label.substring(label.length() - maxLength);
            }
            if (label.equals("hr")) {
                sb.append(hr());

                continue;
            }

            attrSB.append(selectedAttr);
            attrSB.append(extraAttr);
            //attrs(attrSB, ATTR_TITLE, value, ATTR_VALUE, value);
            attrs(attrSB, ATTR_VALUE, value);
            sb.append(tag(TAG_OPTION, attrSB.toString(), label));
            sb.append("\n");
        }
        sb.append(close(TAG_SELECT));
        sb.append("\n");

        return sb.toString();
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
        int cols = 0;
        for (int i = 0; i < columns.size(); i++) {
            if (cols == 0) {
                if (i >= 1) {
                    sb.append(close(TAG_TR));
                }
                sb.append(open(TAG_TR));
            }
            sb.append(col(columns.get(i).toString()));
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
        return open(TAG_TABLE,
                    cssClass("formtable")
                    + attrs(ATTR_CELLPADDING, "0", ATTR_CELLSPACING, "0"));
    }



    /**
     * _more_
     *
     * @param extra _more_
     *
     * @return _more_
     */
    public static String formTable(String extra) {
        return open(TAG_TABLE,
                    attrs(ATTR_CELLPADDING, "5", ATTR_CELLSPACING, "5") + " "
                    + extra);
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
        return tag(TAG_TR, "",
                   tag(TAG_TD,
                       attrs(ATTR_ALIGN, VALUE_RIGHT, ATTR_CLASS,
                             CLASS_FORMLABEL), left) + tag(TAG_TD,
                                 attrs(ATTR_CLASS, CLASS_FORMCONTENTS),
                                 right)) + "\n";


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
        try {
            js.append("\n<nowiki>\n");
            js.append(tag(TAG_SCRIPT, attrs(ATTR_TYPE, "text/JavaScript"),
                          s));
            js.append("\n</nowiki>\n");
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
     *
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
        try {
            sb.append(tag(TAG_SCRIPT,
                          attrs(ATTR_SRC, jsUrl, ATTR_TYPE,
                                "text/JavaScript"), ""));
            sb.append("\n");
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
     * @throws Exception _more_
     */
    public static void cssLink(Appendable sb, String url) throws Exception {
        tag(sb, TAG_LINK,
            attrs(ATTR_HREF, url, ATTR_REL, "stylesheet", ATTR_TYPE,
                  "text/css"));
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
        return tag(TAG_STYLE, "", css);
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
        return makeShowHideBlock(label, content, visible,
                                 cssClass("toggleblocklabel"));
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
            img = HtmlUtils.img(visible
                                ? hideImg
                                : showImg, "",
                                           " align=bottom  " + " id='" + id
                                           + "img' ");
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
        String id  = "block_" + (blockCnt++);
        String img = HtmlUtils.img(visible
                                   ? hideImg
                                   : showImg, "", HtmlUtils.id(id + "img"));
        String link =
            HtmlUtils.jsLink(HtmlUtils.onMouseClick("toggleBlockVisibility('"
                + id + "','" + id + "img','" + hideImg + "','" + showImg
                + "')"), img /* + label*/,
                         HtmlUtils.cssClass("toggleblocklabellink"));

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
    public static void makeAccordian(Appendable sb, String title,
                                     String contents)
            throws Exception {

        makeAccordian(sb, title, contents, null, null);
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
    public static void makeAccordian(Appendable sb, String title,
                                     String contents, String wrapperClass,
                                     String headerClass)
            throws Exception {
        List<String> titles = new ArrayList<String>();
        List<String> tabs   = new ArrayList<String>();
        titles.add(title);
        tabs.add(contents);
        makeAccordian(sb, titles, tabs, true, wrapperClass, headerClass);
    }

    /**
     * Add an accordian of sections to the page
     *
     * @param sb        the StringBuilder/StringBuilder to append to
     * @param titles    the title for each section
     * @param contents  the contents of each section
     *
     * @throws Exception  some problem
     */
    public static void makeAccordian(Appendable sb, List<String> titles,
                                     List<String> contents)
            throws Exception {
        makeAccordian(sb, titles, contents, false, null, null);
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
    public static void makeAccordian(Appendable sb, List<String> titles,
                                     List<String> contents, boolean collapse)
            throws Exception {
        makeAccordian(sb, titles, contents, collapse, null, null);
    }



    /**
     * Add an accordian of sections to the page
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
    public static void makeAccordian(Appendable sb, List<String> titles,
                                     List<String> contents, boolean collapse,
                                     String wrapperClass, String headerClass)
            throws Exception {

        String accordianId = "accordion_" + (blockCnt++);
        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                 HtmlUtils.cssClass(((wrapperClass != null)
                ? wrapperClass
                : "") + " ui-accordion ui-widget ui-helper-reset") + HtmlUtils
                    .id(accordianId)));
        for (int i = 0; i < titles.size(); i++) {
            String title   = titles.get(i);
            String content = contents.get(i);
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
                + HtmlUtils.cssClass("ramadda-accordian-contents"));
            sb.append(HtmlUtils.div(content));
        }
        sb.append("</div>");

        String args =
            "{autoHeight: false, navigation: true, collapsible: true";
        if (collapse) {
            args += ", active: false}";
        } else {
            args += ", active: 0}";
        }

        HtmlUtils.script(sb,
                         "HtmlUtil.makeAccordian(\"#" + accordianId + "\" "
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
                         HtmlUtils.call("HtmlUtil.tooltipInit",
                                        HtmlUtils.squote(opener),
                                        HtmlUtils.squote(id)));
    }

    /**
     * _more_
     *
     * @param icon _more_
     * @param msg _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String tooltip(String icon, String msg) throws Exception {
        StringBuilder sb = new StringBuilder();
        tooltip(sb, icon, msg);

        return sb.toString();
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
            for (int i = 0; i < COLORNAMES.length; i++) {
                if (s.equals(COLORNAMES[i])) {
                    return COLORS[i];
                }
            }
        }

        return dflt;
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
        List<Link> links = new ArrayList<Link>();

        if (url.getProtocol().equals("ftp")) {
            return extractLinksFtp(url, linkPattern);
        }

        //        String html = IOUtil.readContents("test.html");
        String html = Utils.readUrl(url.toString());



        String pattern =
            "(?s)(?i)<\\s*a[^>]*?\\s*href\\s*=\\s*(\"|')([^\"'>]+)(\"|')[^>]*?>(.*?)</a>";

        html = html.replaceAll("\t", " ");
        //<a target="_blank" title="/gov/data/GISDLData/Footprints.kmz" href="/gov/data/GISDLData/Footprints.kmz">KMZ</a>
        Matcher matcher = Pattern.compile(pattern).matcher(html);
        while (matcher.find()) {
            String href = matcher.group(2);
            href = href.replaceAll(" ", "");
            String label = matcher.group(4);

            label = StringUtil.stripTags(label).trim();
            if (linkPattern != null) {
                if ( !(href.matches(linkPattern)
                        || label.matches(linkPattern))) {
                    continue;
                }
            }

            if (href.toLowerCase().startsWith("javascript:")) {
                continue;
            }
            try {
                URL newUrl = new URL(url, href);
                links.add(new Link(newUrl, label));
            } catch (Exception exc) {}
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
                links.add(new Link(newUrl, label, files[i].getSize()));
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

        /** _more_ */
        private URL url;

        /** _more_ */
        private String label;

        /** _more_ */
        private long size = -1;

        /**
         * _more_
         *
         * @param url _more_
         * @param label _more_
         * @param size _more_
         */
        public Link(URL url, String label, long size) {
            this(url, label);
            this.size = size;
        }

        /**
         * _more_
         *
         * @param url _more_
         * @param label _more_
         */
        public Link(URL url, String label) {
            this.url   = url;
            this.label = label;
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
         *  Get the Url property.
         *
         *  @return The Url
         */
        public URL getUrl() {
            return url;
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







}

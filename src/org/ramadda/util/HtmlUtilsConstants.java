/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;
import java.awt.Color;

public interface HtmlUtilsConstants {
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

    public static final String HTTP_USER_AGENT = "User-Agent";
    public static final String HTTP_CONTENT_LENGTH = "Content-Length";
    public static final String HTTP_CONTENT_RANGE="Content-Range";
    public static final String HTTP_ACCEPT_RANGES="Accept-Ranges";
    public static final String HTTP_CONTENT_DESCRIPTION =    "Content-Description";
    public static final String HTTP_WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String HTTP_SET_COOKIE = "Set-Cookie";
    public static final String SUFFIX_NORTH = "_north";
    public static final String SUFFIX_SOUTH = "_south";
    public static final String SUFFIX_EAST = "_east";
    public static final String SUFFIX_WEST = "_west";
    public static final String SIZE_3 = "  size=\"3\" ";
    public static final String SIZE_4 = "  size=\"4\" ";
    public static final String SIZE_5 = "  size=\"5\" ";
    public static final String SIZE_6 = "  size=\"6\" ";
    public static final String SIZE_7 = "  size=\"7\" ";
    public static final String SIZE_8 = "  size=\"8\" ";
    public static final String SIZE_9 = "  size=\"9\" ";
    public static final String SIZE_10 = "  size=\"10\" ";
    public static final String SIZE_20 = "  size=\"20\" ";
    public static final String SIZE_15 = "  size=\"15\" ";
    public static final String SIZE_25 = "  size=\"25\" ";
    public static final String SIZE_30 = "  size=\"30\" ";
    public static final String SIZE_40 = "  size=\"40\" ";
    public static final String SIZE_50 = "  size=\"50\" ";
    public static final String SIZE_60 = "  size=\"60\" ";
    public static final String SIZE_70 = "  size=\"70\" ";
    public static final String SIZE_80 = "  size=\"80\" ";
    public static final String SIZE_90 = "  size=\"90\" ";
    public static final String SIZE_100 = "  size=\"100\" ";    
    public static final String ENTITY_NBSP = "&nbsp;";
    public static final String ENTITY_NBSP2 = "&nbsp;&nbsp;";
    public static final String ENTITY_NBSP3 = "&nbsp;&nbsp;&nbsp;";
    public static final String ENTITY_NBSP4 = "&nbsp;&nbsp;&nbsp;&nbsp;";
    public static final String ENTITY_GT = "&gt;";
    public static final String ENTITY_LT = "&lt;";
    public static final String TAG_A = "a";
    public static final String TAG_B = "b";
    public static final String TAG_BR = "br";
    public static final String TAG_CENTER = "center";
    public static final String TAG_DIV = "div";
    public static final String TAG_INLINE_BLOCK = "inlineblock";    
    public static final String TAG_EMBED = "embed";
    public static final String TAG_OBJECT = "object";
    public static final String TAG_FORM = "form";
    public static final String TAG_HR = "hr";
    public static final String TAG_H1 = "h1";
    public static final String TAG_H2 = "h2";
    public static final String TAG_H3 = "h3";
    public static final String TAG_I = "i";
    public static final String TAG_IMG = "img";
    public static final String TAG_INPUT = "input";
    public static final String TAG_IFRAME = "iframe";
    public static final String TAG_LI = "li";
    public static final String TAG_LINK = "link";
    public static final String TAG_NOBR = "nobr";
    public static final String TAG_OPTION = "option";
    public static final String TAG_P = "p";
    public static final String TAG_PRE = "pre";
    public static final String TAG_SCRIPT = "script";
    public static final String TAG_STYLE = "style";
    public static final String TAG_SPAN = "span";
    public static final String TAG_SELECT = "select";
    public static final String TAG_TABLE = "table";
    public static final String TAG_TD = "td";
    public static final String TAG_TH = "th";
    public static final String TAG_TR = "tr";
    public static final String TAG_TEXTAREA = "textarea";
    public static final String TAG_UL = "ul";
    public static final String ATTR_ACTION = "action";
    public static final String ATTR_ALIGN = "align";
    public static final String ATTR_ALT = "alt";
    public static final String ATTR_BORDER = "border";
    public static final String ATTR_BGCOLOR = "bgcolor";
    public static final String ATTR_CELLSPACING = "cellspacing";
    public static final String ATTR_CELLPADDING = "cellpadding";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_COLS = "cols";
    public static final String ATTR_COLSPAN = "colspan";
    public static final String ATTR_ENCTYPE = "enctype";
    public static final String ATTR_HREF = "href";
    public static final String ATTR_HEIGHT = "height";
    public static final String ATTR_ID = "id";
    public static final String ATTR_METHOD = "method";
    public static final String ATTR_MULTIPLE = "multiple";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_ONMOUSEMOVE = "onmousemove";
    public static final String ATTR_ONMOUSEOVER = "onmouseover";
    public static final String ATTR_ONMOUSEUP = "onmouseup";
    public static final String ATTR_ONMOUSEOUT = "onmouseout";
    public static final String ATTR_ONMOUSEDOWN = "onmousedown";
    public static final String ATTR_ONCLICK = "onClick";
    public static final String ATTR_ONCHANGE = "onchange";
    public static final String ATTR_READONLY = "READONLY";
    public static final String ATTR_REL = "rel";
    public static final String ATTR_ROWS = "rows";
    public static final String ATTR_SELECTED = "selected";
    public static final String ATTR_SIZE = "size";
    public static final String ATTR_SRC = "src";
    public static final String ATTR_STYLE = "style";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_TARGET = "target";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_VALIGN = "valign";
    public static final String ATTR_WIDTH = "width";
    public static final String CLASS_BLOCK = "block";
    public static final String CLASS_CHECKBOX = "checkbox";
    public static final String CLASS_DISABLEDINPUT = "disabledinput";
    public static final String CLASS_ERRORLABEL = "errorlabel";
    public static final String CLASS_FILEINPUT = "fileinput";
    public static final String CLASS_FORMLABEL = "formlabel";
    public static final String CLASS_FORMCONTENTS = "formcontents";
    public static final String CLASS_FORMLABEL_TOP = "formlabeltop";
    public static final String CLASS_HIDDENINPUT = "hiddeninput";
    public static final String CLASS_INPUT = "input";
    public static final String CLASS_PASSWORD = "password";
    public static final String CLASS_RADIO = "radio";
    public static final String CLASS_SELECT = "select";
    public static final String CLASS_SUBMIT = "submit";
    public static final String CLASS_SUBMITIMAGE = "submitimage";
    public static final String CLASS_TAB_CONTENT = "tab_content";
    public static final String CLASS_TAB_CONTENTS = "tab_contents";
    public static final String CLASS_TEXTAREA = "textarea";
    public static final String STYLE_HIDDEN = "display:none;";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_PASSWORD = "password";
    public static final String TYPE_SUBMIT = "submit";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_RADIO = "radio";
    public static final String TYPE_INPUT = "input";
    public static final String TYPE_TEXTAREA = "textarea";
    public static final String TYPE_CHECKBOX = "checkbox";
    public static final String TYPE_HIDDEN = "hidden";
    public static final String VALUE_BOTTOM = "bottom";
    public static final String VALUE_CENTER = "center";
    public static final String VALUE_FALSE = "false";
    public static final String VALUE_LEFT = "left";
    public static final String VALUE_MULTIPART = "multipart/form-data";
    public static final String VALUE_POST = "post";
    public static final String VALUE_RIGHT = "right";
    public static final String VALUE_SELECTED = "selected";
    public static final String VALUE_TOP = "top";
    public static final String VALUE_TRUE = "true";
}

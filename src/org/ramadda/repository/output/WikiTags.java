/*
 * Copyright (c) 2008-2023 Geode Systems LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ramadda.repository.output;

import org.ramadda.repository.Constants;
import org.ramadda.repository.DateHandler;
import org.ramadda.repository.search.SpecialSearch;

import org.ramadda.util.Utils;
import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.List;

/**
 * Provides wiki text processing services
 */
@SuppressWarnings("unchecked")
public class WikiTags implements  OutputConstants,WikiConstants,Constants {
    /** list of import items for the text editor menu */
    //J--
    public static final WikiTagCategory[] WIKITAGS = {
        new WikiTagCategory("General",
                            new WikiTag(WIKI_TAG_INFORMATION, null, ATTR_DETAILS, "false",ATTR_SHOWTITLE,"false"),
                            new WikiTag(WIKI_TAG_NAME,null,"link","true"), 
                            new WikiTag(WIKI_TAG_DESCRIPTION),
                            new WikiTag(WIKI_TAG_RESOURCE, null, ATTR_TITLE,"",ATTR_SHOWICON,"true"),
                            new WikiTag(WIKI_TAG_ENTRYLINK, null, "link","",ATTR_TITLE,"",ATTR_SHOWICON,"true"), 			    
                            new WikiTag(WIKI_TAG_THIS,null),
                            new WikiTag(WIKI_TAG_ANCESTOR,null,"type","entry type"), 			    
                            new WikiTag(WIKI_TAG_LABEL, null, ATTR_TEXT,"",ATTR_ID,"arbitrary id to match with property"),
                            new WikiTag(WIKI_TAG_LINK, null, ATTR_TITLE,"","button","false"),
                            new WikiTag(WIKI_TAG_HTML,null,"showTitle","false"),
                            new WikiTag("multi", null, "_attrs", "attr1,attr2"),
                            new WikiTag(WIKI_TAG_SIMPLE, null, ATTR_TEXTPOSITION, POS_LEFT),
                            new WikiTag(WIKI_TAG_IMPORT, null, ATTR_ENTRY,"","showTitle","false"),
                            new WikiTag(WIKI_TAG_SHOW_AS, null, ATTR_ENTRY,"","type","entry type to display as","#target","target entry"),
                            new WikiTag(WIKI_TAG_EMBED, null, ATTR_ENTRY,"",ATTR_SKIP_LINES,"0",ATTR_MAX_LINES,"1000","style","",ATTR_FORCE,"false",ATTR_MAXHEIGHT,"300",ATTR_ANNOTATE,"true","raw","true","wikify","true"),
                            new WikiTag(WIKI_TAG_TAGS),
                            new WikiTag(WIKI_TAG_FIELD, null, "name", "")),
        new WikiTagCategory("Layout", 
                            new WikiTag(WIKI_TAG_TABLETREE, null, "simple","false","#maxHeight","500px"),
                            new WikiTag(WIKI_TAG_FULLTREE, null,"depth","10","addprefix","false","showroot","true","labelWidth","20", ATTR_SHOWICON,"true","types","group,feile,...."),
                            new WikiTag(WIKI_TAG_MENUTREE, null,"depth","10","addprefix","false","showroot","true","menuStyle","","labelWidth","20", ATTR_SHOWICON,"true","types","group,file,...."), 			    			    
                            new WikiTag(WIKI_TAG_LINKS, null),
                            new WikiTag(WIKI_TAG_LIST),
                            new WikiTag(WIKI_TAG_ENTRIES_TEMPLATE,null,"template","${name link=true}","before","",
					"after",""), 			    
                            new WikiTag(WIKI_TAG_TABS, null), 
                            new WikiTag(WIKI_TAG_GRID, null, 
                                        ATTR_TAG, WIKI_TAG_CARD, 
                                        "inner-height","200", 
					"width","200px",
                                        ATTR_COLUMNS, "3",
					ATTR_SHOWICON, "true",
					"includeChildren","false",
					"addTags","false",
					"showDisplayHeader","false",
					"captionPrefix","",
					"captionSuffix","",
					"#childrenWiki","wiki text to display children, e.g. {{tree details=false}}",
					"#weights","3,6,3",
                                        "showSnippet","false",
                                        "showSnippetHover","true",
                                        "showLink","false","showHeading","true"), 
                            new WikiTag(WIKI_TAG_MAP,
                                        null, ATTR_WIDTH, "100%", ATTR_HEIGHT, "80vh","listentries","true"), 
                            new WikiTag(WIKI_TAG_FRAMES, null, ATTR_WIDTH,"100%", ATTR_HEIGHT,"500","showIcon","true","#icon","/icons/dots/blue.png"), 
                            new WikiTag(WIKI_TAG_ACCORDION, null, ATTR_TAG, WIKI_TAG_HTML, ATTR_COLLAPSE, "false", "border", "0", ATTR_SHOWLINK, "true", ATTR_SHOWICON, "false",ATTR_TEXTPOSITION, POS_LEFT), 
                            //                            new WikiTag(WIKI_TAG_GRID), 
                            new WikiTag(WIKI_TAG_TABLE), 
                            new WikiTag(WIKI_TAG_ABSOPEN,null,"canEdit","true","imageEntry","","width","100%","#height","height"),
                            new WikiTag(WIKI_TAG_ABSCLOSE,null), 			    
                            new WikiTag(WIKI_TAG_RECENT, null, ATTR_DAYS, "3"), 
                            new WikiTag(WIKI_TAG_MULTI, null,"#_tag","","#_template",""), 
                            new WikiTag(WIKI_TAG_APPLY, null, APPLY_PREFIX
					+ "tag", WIKI_TAG_HTML, APPLY_PREFIX
					+ "layout", "table", APPLY_PREFIX
					+ "columns", "1", APPLY_PREFIX
					+ "header", "", APPLY_PREFIX
					+ "footer", "", APPLY_PREFIX
					+ "border", "0", APPLY_PREFIX
                                        + "bordercolor", "#000")),
        new WikiTagCategory("Images & Dates",
                            new WikiTag(WIKI_TAG_IMAGE,null,
                                        "#"+ATTR_SRC, "", ATTR_WIDTH,"100%", "#"+ATTR_ALIGN,"left|center|right"), 
                            new WikiTag(WIKI_TAG_GALLERY,null,
                                        ATTR_WIDTH, "-100", ATTR_COLUMNS, "3",
					ATTR_POPUP, "true", ATTR_THUMBNAIL, "false",
					"decorate","true","imageStyle","","padding","10px",
					ATTR_CAPTION, "Figure ${count}: ${name}",
					"#popupCaption",""), 
                            new WikiTag(WIKI_TAG_SLIDESHOW,"Slide Show",
					ATTR_TEXTPOSITION,"top",
					"linkTop","true",
					ATTR_WIDTH, "400",
					ATTR_HEIGHT,
					"270",
					"#textClass","note",
					"#textStyle","margin:8px;",
					"#showLink","true",
					"bordercolor","#efefef"),
                            new WikiTag(WIKI_TAG_PLAYER, "Image Player",
					"#currentImage","index or \"last\"",
					"#autoPlay","true",
					"#showButtons","false",
					"#boxesPosition","top|bottom|none",
					"#boxHeight","0.5em",
					"#showControls","false",
					"#smallButtons","true",
					"#compact","true",
					"#dateFormat","yyyy-MM-dd HH:mm z",
					"#lazyLoading","false",
					"#loopDelay","1000"),
                            new WikiTag(WIKI_TAG_FLIPCARDS, null, 
                                        "inner","300", 
					"width","300",
					"addTags","false",
					"frontStyle","",
					"backStyle",""),					

                            new WikiTag(WIKI_TAG_DATERANGE,"Date Range", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_FROM, "From Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_TO,"To Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CREATE,"Create Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CHANGE,"Change Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
			    new WikiTag(WIKI_TAG_CALENDAR, null, ATTR_DAY, "false"),
                            new WikiTag(WIKI_TAG_DATETABLE, null,"byType","false","showTime","false"),			    			                              new WikiTag(WIKI_TAG_TIMELINE, null, ATTR_HEIGHT, "150")),
        new WikiTagCategory("Misc",
                            new WikiTag("counter", null, "key", "key"),
                            new WikiTag("caption", null, "label", "","prefix","Image #:"),
                            new WikiTag(WIKI_TAG_QRCODE, null, "#url","","#width", "128","#height","128","#darkColor","red",
					"#lightColor","blue"),
                            new WikiTag(WIKI_TAG_ZIPFILE, null,"#height",""),
                            new WikiTag(WIKI_TAG_USER, null, "users","user1,user2","delimiter"," ","style","","showAvatar","true","showEmail","true"),
                            new WikiTag(WIKI_TAG_COMMENTS),
                            new WikiTag(WIKI_TAG_TAGCLOUD, null, "#type", "", "threshold","0"), 
                            new WikiTag(WIKI_TAG_PROPERTIES, null, "message","","metadata.types","",ATTR_METADATA_INCLUDE_TITLE,"true","separator","","decorate","false"),
                            new WikiTag(WIKI_TAG_DISPLAYPROPERTIES, null, "displayType","null"),

                            new WikiTag(WIKI_TAG_DATAPOLICIES, null, "message","","inherited","true","includePermissions","false"),
			    new WikiTag(WIKI_TAG_WIKITEXT,null,"showToolbar","false"),
                            new WikiTag(WIKI_TAG_BREADCRUMBS),
                            new WikiTag(WIKI_TAG_TOOLS),
                            new WikiTag(WIKI_TAG_TOOLBAR),
                            new WikiTag(WIKI_TAG_LAYOUT),
			    new WikiTag(WIKI_TAG_MENU,null,"title","","popup","true","ifUser","false"),
                            new WikiTag(WIKI_TAG_ENTRYID),
                            new WikiTag(WIKI_TAG_ALIAS,null,"name","alias","entry","entry id"),
                            new WikiTag(WIKI_TAG_SEARCH,null,
                                        ATTR_TYPE, "", 
                                        "#"+ATTR_FIELDS,"",
                                        ATTR_METADATA,"",
                                        ARG_MAX, "100",
                                        ARG_SEARCH_SHOWFORM, "false",
                                        SpecialSearch.ATTR_TABS,
                                        SpecialSearch.TAB_LIST),
                            new WikiTag(WIKI_TAG_UPLOAD,null, ATTR_TITLE,"Upload file", ATTR_SHOWICON,"false"), 
                            new WikiTag(WIKI_TAG_ROOT),
			    new WikiTag("loremipsum")),
    };
    //J++

    private static void attr(StringBuilder sb, String name, String value) {
        Utils.append(sb, " ", name, "=", "&quote;", value, "&quote;", " ");
    }    



    public static class WikiTag {
        /** _more_ */
        String label;

        /** _more_ */
        String tag;

        /** _more_ */
        String attrs;

        /** _more_ */
        List<String> attrsList = new ArrayList<String>();

        /**
         * _more_
         *
         * @param tag _more_
         */
        WikiTag(String tag) {
            this(tag, null);
        }



        /**
         * _more_
         *
         * @param tag _more_
         * @param label _more_
         * @param attrs _more_
         */
        WikiTag(String tag, String label, String... attrs) {
            this.tag = tag;
            if (label == null) {
                label = StringUtil.camelCase(tag);
            }
            this.label = label;
            for (String attr : attrs) {
                attrsList.add(attr);
            }
            if (attrs.length == 1) {
                this.attrs = attrs[0];
            } else {
                StringBuilder sb  = new StringBuilder();
                int           cnt = 0;
                for (int i = 0; i < attrs.length; i += 2) {
                    if (cnt > 80) {
                        sb.append("_newline_");
                        cnt = 0;
                    }
                    cnt += attrs[i].length() + attrs[i + 1].length();
                    attr(sb, attrs[i], attrs[i + 1]);
                }
                this.attrs = sb.toString();
            }
        }

    }

    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Mar 5, '14
     * @author         Enter your name here...
     */
    public static class WikiTagCategory {

        /** _more_ */
        String category;

        /** _more_ */
        WikiTag[] tags;

        /**
         * _more_
         *
         * @param c _more_
         * @param tagArgs _more_
         */
        WikiTagCategory(String c, WikiTag... tagArgs) {
            this.category = c;

            /**
             * Comparator comp = new Comparator() {
             *   public int compare(Object o1, Object o2) {
             *       return ((WikiTag) o1).tag.compareTo(((WikiTag) o2).tag);
             *   }
             * };
             * Arrays.sort(tagArgs, comp);
             */
            tags = tagArgs;
        }
    }


}

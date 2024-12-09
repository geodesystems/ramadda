/*
 * Copyright (c) 2008-2024 Geode Systems LLC
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
    public static final String ATTR_TT ="tt";

    /** list of import items for the text editor menu */
    //J--
    public static final WikiTagCategory[] WIKITAGS = {
        new WikiTagCategory("General",
                            new WikiTag(WIKI_TAG_NAME,null,ATTR_TT,"Entry name","link","true"), 
                            new WikiTag(WIKI_TAG_DESCRIPTION,null,ATTR_TT,"Entry description","wikify","true"),
                            new WikiTag(WIKI_TAG_SNIPPET,null,ATTR_TT,"Entry text snippet"),			    
                            new WikiTag(WIKI_TAG_ICON,null,ATTR_TT,"Entry Icon","#width","16px"),
                            new WikiTag(WIKI_TAG_INFORMATION, null, ATTR_TT,"General entry information",
					"showDetails","true",
					ATTR_SHOWTITLE,"false","#menus","file,edit,view,feeds,other,service","#menusTitle","Services"),
                            new WikiTag(WIKI_TAG_ARK,null,ATTR_TT,"Add the ARK ID if it is enabled","message","","template","<b>ARK ID:</b> ${ark}"), 
                            new WikiTag(WIKI_TAG_RESOURCE, null, ATTR_TT,"Link to entry file",ATTR_TITLE,"",ATTR_SHOWICON,"true","simple","false"),
                            new WikiTag(WIKI_TAG_MEDIA,"Media",ATTR_TT,"Embed the resource, e.g., image, video, audio, etc","width","100%","#full","true"), 
                            new WikiTag(WIKI_TAG_SOUNDCITE,"Sound Cite",ATTR_TT,"Embed a audio player link",
					"label","listen","#url","","#start","0"), 
                            new WikiTag(WIKI_TAG_ENTRYLINK,"Entry link", ATTR_TT,"Link to entry","link","",ATTR_TITLE,"",ATTR_SHOWICON,"true"), 			    
                            new WikiTag(WIKI_TAG_THIS,"Entry ID",ATTR_TT,"The entry ID"),
                            new WikiTag(WIKI_TAG_TYPENAME,null,ATTR_TT,"Entry type name"), 
                            new WikiTag(WIKI_TAG_CHILDREN_COUNT,"Children count",ATTR_TT,"Show the # of children",
					"template","${count}"),			    
                            new WikiTag(WIKI_TAG_EDITBUTTON,null,"label","Edit","#message","Show when cannot edit"),
                            new WikiTag(WIKI_TAG_NEW_ENTRY,null,"type","entry type to create",
					"#fromEntry","true",
					"#label","","#message","Show when cannot new"),
                            new WikiTag(WIKI_TAG_NEW_PROPERTY,null,"type","metadata type to create",
					"#label","","#message","Show when cannot new","addBreak","true",
					"style","margin-bottom:4px;"), 			    			    			    
                            new WikiTag(WIKI_TAG_ACCESS_STATUS,"Show access","fullAccess","false"),
                            new WikiTag(WIKI_TAG_ANCESTOR,"Ancestor ID",ATTR_TT,"ID of ancestor","#type","entry type"), 			    
                            new WikiTag(WIKI_TAG_LABEL, null, ATTR_TEXT,"",ATTR_ID,"arbitrary id to match with property"),
                            new WikiTag(WIKI_TAG_LINK, null, ATTR_TITLE,"","button","false"),
                            new WikiTag(WIKI_TAG_HTML,null,"showTitle","false"),
                            new WikiTag(WIKI_TAG_IMPORT, null, ATTR_TT,"Import display of another entry",
					ATTR_ENTRY,"","showTitle","false"),
                            new WikiTag(WIKI_TAG_MACRO, null, ATTR_TT,"Add entry macro",
					"name","macroname",
					ATTR_ENTRY,""),			    

                            new WikiTag("multi", null, "_attrs", "attr1,attr2"),
                            new WikiTag(WIKI_TAG_SIMPLE, null, ATTR_TEXTPOSITION, POS_LEFT),
                            new WikiTag(WIKI_TAG_SHOW_AS, null, ATTR_ENTRY,"","type","entry type to display as","#target","target entry"),
                            new WikiTag(WIKI_TAG_EMBED, null, ATTR_ENTRY,"",
					ATTR_SKIP_LINES,"0",
					ATTR_MAX_LINES,"1000",
					"style","",
					ATTR_FORCE,"false",
					ATTR_MAXHEIGHT,"800",
					"#"+ATTR_ANNOTATE,"true",
					"raw","true",
					"#wikify","true"),
                            new WikiTag(WIKI_TAG_EMBEDMS, "Embed ppt/doc/xls","entry","entry id"),
                            new WikiTag(WIKI_TAG_TAGS),
                            new WikiTag(WIKI_TAG_FIELD, null, "name", "",
					"fieldPrefix","","fieldSuffix","")),
        new WikiTagCategory("Layout", 
                            new WikiTag(WIKI_TAG_TABLETREE, "Entry table/tree",
					ATTR_TT,"Standard entry table with tree links",
					"message","",
					"simple","false",
					"#maxHeight","500px"),
                            new WikiTag(WIKI_TAG_FULLTREE, "Entry full tree",ATTR_TT,
					"Show a tree of entries",
					"depth","10","addprefix","false","showroot","true","labelWidth","20", ATTR_SHOWICON,"true","#types","group,file,...."),
                            new WikiTag(WIKI_TAG_MENUTREE, null,"depth","4","addprefix","false","showroot","true","menuStyle","","labelWidth","20", ATTR_SHOWICON,"true","types","group,file,...."), 			    			    
                            new WikiTag(WIKI_TAG_LINKS, null,
					"#target","link target"),
                            new WikiTag(WIKI_TAG_LIST),
			    new WikiTag(WIKI_TAG_NAMELIST,null,"showToggleAll","true"),
			    

                            new WikiTag(WIKI_TAG_ENTRIES_TEMPLATE,null,"template","${name link=true}","before","",
					"after",""), 			    
                            new WikiTag(WIKI_TAG_TABS, null,ATTR_TT,"Show entries in tabs"), 
                            new WikiTag(WIKI_TAG_GRID, "Entry grid",ATTR_TT,"Show a grid of entries", 
                                        ATTR_TAG, WIKI_TAG_CARD, 
                                        "inner-height","200", 
					"width","200px",
					"#boxHeight","180px",
					"#imageHeight","150px",					
					ATTR_SHOWICON, "true",
					"includeChildren","false",
					"addTags","false",
					"showDisplayHeader","false",
					"captionPrefix","",
					"captionSuffix","",
                                        "showSnippet","false",
                                        "showSnippetHover","true",
                                        "showLink","false",
					"showHeading","true",
					"showPlaceholderImage","true",
					"#useThumbnail","false",
					"#expand","true",
					"#childrenWiki","wiki text to display children, e.g. {{tree details=false}}",
					"#weights","4,4,4"), 
                            new WikiTag(WIKI_TAG_NAVBAR, null,
					"style","","linkStyle","","separator","|",
					"links","comma separated entry ids and/or links of the form /repository/search/form;Search,...",
					"#separator","",
					"#style","",
					"#linkStyle","",
					"#entryid.label","label instead of name",
					"#popup","true",
					"#left","true|false - if popup set the oriention",
					"#popupLinkStyle","",
					"#header","Header for popup",
					"#footer","Footer for popup"),
                            new WikiTag(WIKI_TAG_MAP,
                                        null, ATTR_WIDTH, "100%", ATTR_HEIGHT, "80vh",
					"listEntries","true",
					"#entryIcon","",
					"#skipEntries","true",
					"#marker1",
					"latitude:40,longitude:-105,color:red,radius:4,text:Some text",
					"#entriesListInMap","true",
					"#entriesListHeader","",
					"#hideIfNoLocations","false",
					"#showCircles","true",
					"#mapProps","fillColor:red,strokeWidth:1,radius:6"), 
                            new WikiTag(WIKI_TAG_FRAMES, null, ATTR_WIDTH,"100%",
					ATTR_HEIGHT,"800px","showIcon","true",
					"#leftWidth","2",
					"#rightWidth","10",
					"#template","page template",
					"#category.<entry id>","",
					"#icon","/icons/dots/blue.png"), 
                            new WikiTag(WIKI_TAG_ACCORDION, null, ATTR_TAG, WIKI_TAG_HTML, ATTR_COLLAPSE, "false", "border", "0", ATTR_SHOWLINK, "true", ATTR_SHOWICON, "false",ATTR_TEXTPOSITION, POS_LEFT), 
                            //                            new WikiTag(WIKI_TAG_GRID), 
                            new WikiTag(WIKI_TAG_TABLE, "Tables", 
					ATTR_TT,"Entry tables grouped by type"),
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
                                        "#"+ATTR_SRC, "",
					"#useThumbnail","true",
					ATTR_WIDTH,"100%",
					"#width","100px or screenshot",
					"#src","URL or ::* for thumbnail",
					"popup","true",
					"#"+ATTR_ALIGN,"left|center|right",
					"#cropHeight","50px","#position","top or 0 -40px"), 
                            new WikiTag(WIKI_TAG_GALLERY,null,
                                        "#width", "300px",
					ATTR_COLUMNS, "3",
					ATTR_POPUP, "true", ATTR_USE_THUMBNAIL, "false",
					"decorate","true","imageStyle","","padding","10px",
					ATTR_CAPTION, "Figure ${count}: ${name}",
					"#popupCaption",""),
                            new WikiTag(WIKI_TAG_READER,null,
                                        "#width", "500px"),
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
					"#serverImageWidth","set to number  to change the width of the image on the server for faster load times",
					"#lazyLoading","false",
					"#loopDelay","1000"),
                            new WikiTag(WIKI_TAG_FLIPCARDS, null, 
                                        "inner","300", 
					"width","300",
					"addTags","false",
					"frontStyle","",
					"backStyle",""),					

                            new WikiTag(WIKI_TAG_ZOOMIFY,"Zoomify Image",
					"#singleFile","true",
					"#maxZoomLevel","18",
					"#showRotationControl","false"), 

                            new WikiTag(WIKI_TAG_DATERANGE,"Date Range", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_FROM, "From Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT),
                            new WikiTag(WIKI_TAG_DATE_TO,"To Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CREATE,"Create Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
                            new WikiTag(WIKI_TAG_DATE_CHANGE,"Change Date", ATTR_FORMAT,DateHandler.DEFAULT_TIME_FORMAT), 
			    new WikiTag(WIKI_TAG_CALENDAR, null, ATTR_DAY, "false"),
                            new WikiTag(WIKI_TAG_DATETABLE, null,"byType","false","showTime","false"),			    			                              new WikiTag(WIKI_TAG_TIMELINE, null, ATTR_HEIGHT, "150")),
        new WikiTagCategory("Misc 1",
                            new WikiTag("share", "Social Media Share Bar",
					"#position","left|right",
					"#theme","circle|square",
					"#horizontal","true",
					"#here","true",
					"#channels", "facebook,twitter,linkedin,tumblr,digg,googleplus,reddit,pinterest,stumbleupon,email",
					"#animate","false",
					"#title","",
					"#url","",					
					"#desc",""),
                            new WikiTag("loginform","Login Form",
					ATTR_TT,"Add a login form",
					"onlyIfLoggedOut",   "true",
					"showUserLink",   "true",					
					"#userId","",
					"#formPrefix","Please login:",
					"#loggedInMessage",  ""), 			    
                            new WikiTag(WIKI_TAG_COPYABLE, null,
					ATTR_TT,"Add a copyable item","text","","addIcon","true"),
                            new WikiTag("counter", null, "key", "key"),
                            new WikiTag("caption", null, "label", "","prefix","Image #:"),
                            new WikiTag(WIKI_TAG_QRCODE, null, "#url","","#width", "128","#height","128","#darkColor","red",
					"#lightColor","blue"),
                            new WikiTag(WIKI_TAG_BARCODE, null, 
					"#field","entry field",
					"#value","or value",
					"#format","see https://github.com/lindell/JsBarcode/wiki/Options#format",
					"#width","1.2",
					"#height","30",
					"#displayValue","false",
					"#fontSize","18",
					"#lineColor","#000"),
                            new WikiTag(WIKI_TAG_ZIPFILE, null,"#height",""),
                            new WikiTag(WIKI_TAG_USER, null, "users","user1,user2","delimiter"," ","style","","showAvatar","true","showEmail","true"),
                            new WikiTag(WIKI_TAG_COMMENTS),
                            new WikiTag(WIKI_TAG_TAGCLOUD, null, "#type", "", "threshold","0"), 
                            new WikiTag(WIKI_TAG_PROPERTIES, null,
					"message","",
					"metadata.types","",
					"layout","tabs|linear|accordion",
					ATTR_METADATA_INCLUDE_TITLE,"true",
					"separator","",
					"decorate","false",
					"inherited","false"),
                            new WikiTag(WIKI_TAG_DISPLAYPROPERTIES, null, "displayType","null"),
                            new WikiTag(WIKI_TAG_DATA_STATUS, null),
                            new WikiTag(WIKI_TAG_USAGE, "Usage Descriptor",
					"descriptor","cc-by",
					"includeName","true",
					"showDescription","true",
					"decorate","true",
					"#iconWidth","60px",
					"#textBefore","extra text",
					"#textAfter","extra text",					
					"#style","",
					"#see_descriptors_at",
					"https://ramadda.org/repository/usagedescriptors",
					"#required",
					"If set then user must agree. Some unique id, e.g., agreed_license_1",
					"#requireMessage", "Message to show",
					"#requireSuffix","Message to show after descriptor",
					"#requireShowLicense", "false",
					"#requireRedirect","https://example.com",
					"#logName","true",
					"#requireOnlyAnonymous", "true"					
					),

                            new WikiTag(WIKI_TAG_DATAPOLICIES, null, "message","","inherited","true","includePermissions","false"),
			    new WikiTag(WIKI_TAG_WIKITEXT,null,"showToolbar","false")),
        new WikiTagCategory("Misc 2",
                            new WikiTag(WIKI_TAG_BREADCRUMBS),
                            new WikiTag(WIKI_TAG_TOOLS,"Tools",ATTR_TT,"Add tools for this entry",
					"title","Services",
					"includeIcon","true",
					"#types","service,file,view,feeds",
					"#pattern","pattern to match",
					"#message","Message when no tools"),
                            new WikiTag(WIKI_TAG_TOOLBAR),
                            new WikiTag(WIKI_TAG_LAYOUT),
			    new WikiTag(WIKI_TAG_MENU,null,"title","","popup","true","ifUser","false"),
                            new WikiTag(WIKI_TAG_ENTRYID),
                            new WikiTag(WIKI_TAG_ALIAS,null,"name","alias","entry","entry id"),
			    new WikiTag(WIKI_TAG_TYPECOUNT,"Entry Type Count",
					"types","comma separated list of types",
					"#except","comma separated list of types to exclude",
					"hideWhenZero","false",
					"template","${icon} ${label}<br>${count}",
					"addSearchLink","true",
					"animated","true",
					"style","vertical-align:top;margin-right:10px;min-height:3em;padding:5px;text-align:center;border:var(--basic-border);",
					"#label","",
					"#topCount","5",
					"#types","Use * if doing topCount"),

			    new WikiTag(WIKI_TAG_TYPE_SEARCH,"Type Search","type","",
					"showTitle","false",
					"#displayTypes","list,images,timeline,map,metadata",
					"#orderByTypes","relevant,name,createdate,date,size",
					"#showText","false",
					"#showName","false",
					"#showDescription","false",
					"#showName","false",
					"#showDate","false",
					"#showCreateDate","false",					
					"#showAncestor","false",
					"#showProviders","true",
					"#providers","this,type:ramadda",
					"#providersMultiple","true",
					"#toggleClose","true",
					"#textToggleClose","true",
					"#dateToggleClose","true",
					"#areaToggleClose","true",
					"#columnsToggleClose","true",
					"#showOutputs","false",
					"#outputs","csv,json,zip,export,extedit,copyurl",
					"#formHeight","70vh",
					"#entriesHeight","70vh"),
			    new WikiTag(WIKI_TAG_TYPE_SEARCH_LIST,"Type Search List",
					
					"showHeader","true",
					"showSearchField","true",
					"#providers","this,type:ramadda",
					"#focus","false",
					"#width","200px",
					"#height","400px",
					"#supers","comma separated list of super categories",
					"#cats","comma separated list of categories",
					"#types","comma separated list of types"),
                            new WikiTag(WIKI_TAG_SEARCH,null,
                                        ATTR_TYPE, "", 
                                        "#"+ATTR_FIELDS,"",
                                        ATTR_METADATA,"",
                                        ARG_MAX, "100",
                                        ARG_SEARCH_SHOWFORM, "false",
                                        SpecialSearch.ATTR_TABS,
                                        SpecialSearch.TAB_LIST),
                            new WikiTag(WIKI_TAG_UPLOAD,null, "#type","Some entry type",
					ATTR_TITLE,"Upload file", ATTR_SHOWICON,"false","showForm","false"), 
                            new WikiTag(WIKI_TAG_ROOT),
			    new WikiTag("loremipsum","Lorem Ipsum Text",ATTR_TT,"Filler text\nimg:lorem.png")),
    };
    //J++

    private static void attr(StringBuilder sb, String name, String value) {
        Utils.append(sb, " ", name, "=", "&quote;", value, "&quote;", " ");
    }    



    public static class WikiTag {
        String label;
        String tag;
        String attrs;
	String tt;
        List<String> attrsList = new ArrayList<String>();
        public WikiTag(String tag) {
            this(tag, null);
        }

        public   WikiTag(String tag, String label, String... attrs) {
	    boolean debug=false;
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
		    
		    if(attrs[i]!=null && attrs[i].equals(ATTR_TT)) {
			tt = attrs[i+1];
			if(debug) System.err.println("TT:" + tt);
			continue;
		    }

                    cnt += attrs[i].length() + attrs[i + 1].length();
		    if(debug) System.err.println("attr:" + attrs[i] + "=" + attrs[i+1]);
                    attr(sb, attrs[i], attrs[i + 1]);
                }
                this.attrs = sb.toString();
		if(debug) System.err.println("attrs:" + this.attrs);
            }
        }

    }


    public static class WikiTagCategory {
        String category;
        WikiTag[] tags;
        WikiTagCategory(String c, WikiTag... tagArgs) {
            this.category = c;
            tags = tagArgs;
        }
    }


}

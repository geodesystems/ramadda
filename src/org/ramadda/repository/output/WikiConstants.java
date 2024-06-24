/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.Constants;


/**
 */
public interface WikiConstants {

    public static final String ARG_REMOTE = "remote";



    /** _more_ */
    public static final String ID_ROOT = "root";

    public static final String ID_THIS = "this";
    public static final String PREFIX_THIS = ID_THIS+":";

    public static final String ID_ANCESTORS = "ancestors";
    public static final String PREFIX_ANCESTORS= ID_ANCESTORS+":";

    public static final String ID_ANCESTOR = "ancestor";
    public static final String PREFIX_ANCESTOR = ID_ANCESTOR+":";

    public static final String ID_LINKS = "links";
    public static final String PREFIX_LINKS= ID_LINKS+":";

    public static final String ID_LINK = "link";
    public static final String PREFIX_LINK= ID_LINK+":";

    public static final String ID_SIBLINGS = "siblings";
    public static final String PREFIX_SIBLINGS= ID_SIBLINGS+":";    


    public static final String ID_CHILD= "child";
    public static final String PREFIX_CHILD= ID_CHILD+":";

    public static final String ID_CHILDREN = "children";    
    public static final String PREFIX_CHILDREN= ID_CHILDREN+":";    


    public static final String ID_PARENT = "parent";
    public static final String PREFIX_PARENT = ID_PARENT+":";


    public static final String ID_GRANDPARENT = "grandparent";
    public static final String PREFIX_GRANDPARENT= ID_GRANDPARENT+":";


    public static final String ID_GRANDCHILDREN = "grandchildren";
    public static final String PREFIX_GRANDCHILDREN= ID_GRANDCHILDREN+":";    

    public static final String ID_GRANDCHILD = "grandchild";
    public static final String PREFIX_GRANDCHILD= ID_GRANDCHILD+":";

    /** _more_ */
    public static final String ID_GREATGRANDCHILDREN = "greatgrandchildren";
    public static final String PREFIX_GREATGRANDCHILDREN = ID_GREATGRANDCHILDREN+":";    


    /** _more_ */
    public static final String ID_REMOTE = "remote:";


    /** _more_ */
    public static final String ID_SEARCH = "search";


    public static final String PREFIX_ALIAS= "alias:";
    public static final String PREFIX_SEARCH= "search:";
    public static final String PREFIX_TYPE = "type:";
    public static final String PREFIX_ORDERBY = "orderby:";
    public static final String PREFIX_DESCENDENT = "descendent:";
    public static final String PREFIX_ASCENDING = "ascending:";


    /** wiki page type */
    public static String TYPE_WIKIPAGE = "wikipage";


    /** _more_ */
    public static final String ATTR_ALIGN = "align";

    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_COLLAPSE = "collapse";

    /** _more_ */
    public static final String ATTR_ANNOTATE = "annotate";

    /** _more_ */
    public static final String ATTR_BLOCK_SHOW = "block.show";

    /** _more_ */
    public static final String ATTR_BLOCK_OPEN = "block.open";

    /** _more_ */
    public static final String ATTR_BLOCK_TITLE = "block.title";

    /** _more_ */
    public static final String ATTR_BLOCK_POPUP = "block.popup";

    /** _more_ */
    public static final String ATTR_ROW_LABEL = "row.label";


    /** border attribute */
    public static final String ATTR_BORDER = "border";

    /** _more_ */
    public static final String ATTR_METADATA_TYPES = "metadata.types";

    /** _more_ */
    public static final String ATTR_METADATA_INCLUDE_TITLE = "includeTitle";

    /** _more_ */
    public static final String ATTR_PADDING = "padding";


    /** _more_ */
    public static final String ATTR_MARGIN = "margin";

    /** border color */
    public static final String ATTR_BORDERCOLOR = "bordercolor";

    /** _more_ */
    public static final String ATTR_INNERCLASS = "innerClass";



    /** _more_ */
    public static final String ATTR_COLORS = "colors";

    /** show the details attribute */
    public static final String ATTR_DETAILS = "details";

    /** _more_ */
    public static final String ATTR_MAPDETAILS = "mapDetails";

    /** _more_ */
    public static final String ATTR_DECORATE = "decorate";

    /** _more_ */
    public static final String ATTR_SKIP_LINES = "skipLines";

    /** _more_ */
    public static final String ATTR_MAX_LINES = "maxLines";

    /** _more_ */
    public static final String ATTR_FORCE = "force";



    /** maximum attribute */
    public static final String ATTR_MAX = "max";

    /** linkresource attribute */
    public static final String ATTR_LINKRESOURCE = "linkresource";

    /** listentries attribute */
    public static final String ATTR_LISTENTRIES = "listEntries";

    /** _more_ */
    public static final String ATTR_LAYER = "layer";

    /** listwidth attribute */
    public static final String ATTR_LISTWIDTH = "listwidth";

    /** link attribute */
    public static final String ATTR_LINK = "link";

    /** attribute in the tabs tag */
    public static final String ATTR_USEDESCRIPTION = "usedescription";

    /** attribute in the tabs tag */
    public static final String ATTR_SHOWLINK = "showLink";

    /** _more_ */
    public static final String ATTR_SHOWTITLE = "showTitle";

    /** _more_ */
    public static final String ATTR_SHOWMAP = "showMap";

    /** _more_ */
    public static final String ATTR_SHOWMENU = "showMenu";

    /** src attribute */
    public static final String ATTR_SRC = "src";

    /** _more_ */
    public static final String ATTR_LIST_PREFIX = "listPrefix";

    /** _more_ */
    public static final String ATTR_LIST_SUFFIX = "listSuffix";


    /** _more_ */
    public static final String ATTR_PREFIX = "prefix";

    /** _more_ */
    public static final String ATTR_SUFFIX = "suffix";


    /** _more_ */
    public static final String ATTR_IF = "if";



    /**  */
    public static final String ATTR_SHOWICON = "showIcon";

    /** _more_ */
    public static final String ATTR_SHOWDESCRIPTION = "showdescription";


    /** _more_ */
    public static final String ATTR_ICON = "icon";

    /** attribute in the tabs tag */
    public static final String ATTR_LINKLABEL = "linklabel";

    /** _more_ */
    public static final String ATTR_ENTRY = "entry";

    /** attribute in import tag */
    public static final String ATTR_ENTRIES = "entries";

    /** exclude attribute */
    public static final String ATTR_EXCLUDE = "exclude";

    /** first attribute */
    public static final String ATTR_FIRST = "first";

    /** _more_ */
    public static final String ATTR_FIELDS = "fields";

    /** _more_ */
    public static final String ATTR_METADATA = "metadata";

    /** _more_ */
    public static final String ATTR_LAST = "last";

    /** sort attribute */
    public static final String ATTR_SORT = "sort";

    /** sort order attribute */
    public static final String ATTR_SORT_ORDER = "sortorder";

    /** sort attribute */
    public static final String ATTR_SORT_BY = "sortby";

    /** sort order attribute */
    public static final String ATTR_SORT_DIR = "sortdir";


    /** the message attribute */
    public static final String ATTR_MESSAGE = "message";

    /** the associations attribute */
    public static final String ATTR_ASSOCIATIONS = "associations";

    /** attribute in import tag */
    public static final String ATTR_SHOWHIDE = "showhide";

    /** attribute in import tag */
    public static final String ATTR_OUTPUT = "output";

    /** attribute in import tag */
    public static final String ATTR_RANDOM = "random";

    /** attribute in import tag */
    public static final String ATTR_CHILDREN = "children";

    /** _more_ */
    public static final String ATTR_CONSTRAINSIZE = "constrainsize";

    /** attribute in import tag */
    public static final String ATTR_FORMAT = "format";

    /** attribute in import tag */
    public static final String ATTR_SHOWTOGGLE = "showtoggle";

    /** attribute in import tag */
    public static final String ATTR_OPEN = "open";

    /** attribute in import tag */
    public static final String ATTR_FILES = "files";

    /** attribute in import tag */
    public static final String ATTR_FOLDERS = "folders";


    /** images only attribute */
    public static final String ATTR_IMAGES = "images";



    /** thumbnail attribute */
    public static final String ATTR_USE_THUMBNAIL = "useThumbnail";

    /** caption attribute */
    public static final String ATTR_CAPTION = "caption";

    /** attribute in import tag */
    public static final String ATTR_SEPARATOR = "separator";

    /** attribute in import tag */
    public static final String ATTR_STYLE = "style";

    /** _more_ */
    public static final String ATTR_TAG = "tag";


    /** attribute in import tag */
    public static final String ATTR_TAGOPEN = "tagopen";

    /** attribute in import tag */
    public static final String ATTR_TAGCLOSE = "tagclose";


    /** attribute in import tag */
    public static final String ATTR_LAYOUT = "layout";

    /** the columns attribute */
    public static final String ATTR_COLUMNS = "columns";

    /** the fieldname attribute */
    public static final String ATTR_FIELDNAME = "name";

    /** attribute in import tag */
    public static final String LAYOUT_HORIZONTAL = "hor";

    /** attribute in import tag */
    public static final String LAYOUT_VERTICAL = "vert";

    /** attribute in import tag */
    public static final String ATTR_MENUS = "menus";

    /** attribute in import tag */
    public static final String ATTR_REQUEST = "request";

    /** _more_ */
    public static final String ATTR_POPUP = "popup";

    /** attribute in import tag */
    public static final String ATTR_POPUPCAPTION = "popupcaption";

    /** attribute in import tag */
    public static final String ATTR_LEVEL = "level";

    /** attribute in import tag */
    public static final String ATTR_COUNT = "count";

    /** attribute to wikify the content */
    public static final String ATTR_WIKIFY = "wikify";

    /** max image height attribute */
    public static final String ATTR_MAXIMAGEHEIGHT = "maximageheight";

    /** _more_ */
    public static final String ATTR_MAXHEIGHT = "maxheight";

    /** _more_ */
    public static final String ATTR_MINHEIGHT = "minheight";

    /** attribute in import tag */
    public static final String ATTR_DAY = "day";

    /** attribute in import tag */
    public static final String ATTR_DAYS = "days";


    /** _more_ */
    public static final String WIKI_TAG_GROUP_OLD = "displaygroup";



    /** _more_ */
    public static final String WIKI_TAG_GROUP = "group";

    /**  */
    public static final String WIKI_TAG_VERSION = "ramadda_version";

    public static final String WIKI_TAG_ABSOPEN = "absopen";
    public static final String WIKI_TAG_ABSCLOSE = "absclose";

    /**  */
    public static final String WIKI_TAG_MAKELABEL = "makelabel";

    /**  */
    public static final String WIKI_TAG_PAGETEMPLATE = "pagetemplate";

    public static final String WIKI_TAG_ENTRIES_TEMPLATE = "entries_template";    

    /** _more_ */
    public static final String WIKI_TAG_WIKITEXT = "wikitext";


    /** _more_ */
    public static final String WIKI_TAG_EMBED = "embed";
    public static final String WIKI_TAG_EMBEDMS = "embedms";

    public static final String WIKI_TAG_MEDIA = "media";

    /** _more_ */
    public static final String WIKI_TAG_ODOMETER = "odometer";

    /** _more_ */
    public static final String WIKI_TAG_CHART = "chart";

    /** _more_ */
    public static final String WIKI_TAG_DISPLAY = "display";

    /** wiki import */
    public static final String WIKI_TAG_IMPORT = "import";

    public static final String WIKI_TAG_MACRO = "macro";    

    public static final String WIKI_TAG_SHOW_AS = "show_as";    

    /** the field property */
    public static final String WIKI_TAG_FIELD = "field";

    /** _more_ */
    public static final String WIKI_TAG_ROOT = "root";

    /** the calendar property */
    public static final String WIKI_TAG_CALENDAR = "calendar";

    /** the calendar property */
    public static final String WIKI_TAG_DATETABLE = "datetable";    

    /** _more_ */
    public static final String WIKI_TAG_GRAPH = "graph";

    /** the timeline property */
    public static final String WIKI_TAG_TIMELINE = "timeline";

    /** wiki import */
    public static final String WIKI_TAG_DATE = "date";

    /** _more_ */
    public static final String WIKI_TAG_DATERANGE = "daterange";

    /** wiki import */
    public static final String WIKI_TAG_DATE_FROM = "fromdate";

    /** wiki import */
    public static final String WIKI_TAG_DATE_TO = "todate";

    /** _more_ */
    public static final String WIKI_TAG_DATE_CREATE = "createdate";

    /** _more_ */
    public static final String WIKI_TAG_DATE_CHANGE = "changedate";

    /** wiki import */
    public static final String WIKI_TAG_MENU = "menu";


    /**  */
    public static final String WIKI_TAG_MENUTREE = "menutree";


    /** _more_ */
    public static final String WIKI_TAG_SEARCH = "search";

    public static final String WIKI_TAG_TYPECOUNT = "typecount";    

    public static final String WIKI_TAG_TYPE_SEARCH = "typesearch";    

    public static final String WIKI_TAG_TYPE_SEARCH_LIST = "typesearchlist";    

    /** wiki import */
    public static final String WIKI_TAG_TREE = "tree";

    /**  */
    public static final String WIKI_TAG_TABLETREE = "tabletree";

    /** _more_ */
    public static final String WIKI_TAG_FULLTREE = "fulltree";

    /** _more_ */
    public static final String WIKI_TAG_TREEVIEW = "treeview";

    /** _more_ */
    public static final String WIKI_TAG_FRAMES = "frames";


    /**  */
    public static final String WIKI_TAG_CAPTION = "caption";


    /** the table property */
    public static final String WIKI_TAG_TABLE = "table";


    /** wiki import */
    public static final String WIKI_TAG_COMMENTS = "comments";



    /** _more_ */
    public static final String WIKI_TAG_TAGCLOUD = "tagcloud";

    /** wiki import */
    public static final String WIKI_TAG_RECENT = "recent";

    /** wiki import */
    public static final String WIKI_TAG_GALLERY = "gallery";

    /**  */
    public static final String WIKI_TAG_ZIPFILE = "zipfile";


    /** the image player property */
    public static final String WIKI_TAG_PLAYER = "imageplayer";

    /** the old image player property */
    public static final String WIKI_TAG_PLAYER_OLD = "player";

    /** wiki import */
    public static final String WIKI_TAG_TABS = "tabs";

    /** _more_ */
    public static final String WIKI_TAG_BOUNDS = "bounds";

    /** _more_ */
    public static final String WIKI_TAG_BOOTSTRAP = "bootstrap";

    public static final String WIKI_TAG_ACCESS_STATUS = "access_status";
    public static final String WIKI_TAG_EDITBUTTON = "editbutton";
    public static final String WIKI_TAG_NEWBUTTON = "newbutton";
    public static final String WIKI_TAG_NEW_TYPE = "new_type";
    public static final String WIKI_TAG_NEW_PROPERTY = "new_property";    

    /** _more_ */
    public static final String WIKI_TAG_APPLY = "apply";

    /** _more_ */
    public static final String WIKI_TAG_ATTRS = "attrs";

    /** _more_ */
    public static final String WIKI_TAG_DISPLAYPROPERTY = "displayProperty";

    /** _more_ */
    public static final String WIKI_TAG_DISPLAYPROPERTIES =
        "displayProperties";



    /** _more_ */
    public static final String WIKI_TAG_MULTI = "multi";

    /** _more_ */
    public static final String APPLY_PREFIX = "apply.";

    /** _more_ */
    public static final String ATTR_APPLY_TAG = APPLY_PREFIX + "tag";

    /** _more_ */
    public static final String ATTR_SELECTFIELDS = "selectFields";

    /** _more_ */
    public static final String ATTR_SELECTBOUNDS = "selectBounds";

    /** _more_ */
    public static final String ATTR_VIEWBOUNDS = "viewBounds";

    /** _more_ */
    public static final String ATTR_MAPVAR = "mapVar";

    /** accordian property */
    public static final String WIKI_TAG_ACCORDIAN = "accordian";

    /** accordion property */
    public static final String WIKI_TAG_ACCORDION = "accordion";

    /** the slideshow property */
    public static final String WIKI_TAG_SLIDESHOW = "slideshow";

    /**  */
    public static final String WIKI_TAG_TAGS = "tags";


    /** wiki import */
    public static final String WIKI_TAG_GRID = "grid";

    /**  */
    public static final String WIKI_TAG_FLIPCARDS = "flipcards";

    /** _more_ */
    public static final String WIKI_TAG_CARD = "card";

    /** wiki import */
    public static final String WIKI_TAG_TOOLBAR = "toolbar";

    /** wiki import */
    public static final String WIKI_TAG_BREADCRUMBS = "breadcrumbs";

    /** wiki import */
    public static final String WIKI_TAG_INFORMATION = "information";

    public static final String WIKI_TAG_THIS = "this";
    public static final String WIKI_TAG_ANCESTOR = "ancestor";    

    public static final String WIKI_TAG_USER = "user";


    /**  */
    public static final String WIKI_TAG_PREV = "prev";

    /**  */
    public static final String WIKI_TAG_UP = "up";

    /**  */
    public static final String WIKI_TAG_NEXT = "next";

    /**  */
    public static final String WIKI_TAG_PREVNEXT = "prevnext";


    /** _more_ */
    public static final String WIKI_TAG_DOWNLOAD = "download";

    /** wiki import */
    public static final String WIKI_TAG_IMAGE = "image";
    public static final String WIKI_TAG_ZOOMIFY = "zoomify";

    public static final String WIKI_TAG_FA = "fa";    

    /**  */
    public static final String WIKI_TAG_IMAGE2 = "image2";


    /** _more_ */
    public static final String WIKI_TAG_STREETVIEW = "streetview";

    /** wiki import */
    public static final String WIKI_TAG_NAME = "name";
    public static final String WIKI_TAG_ICON = "icon";    
    public static final String WIKI_TAG_TYPENAME = "typename";

    public static final String WIKI_TAG_ARK = "ark";
    public static final String WIKI_TAG_DISPLAY_IMPORTS = "displayImports";


    /** wiki import */
    public static final String WIKI_TAG_MAP = "map";


    /** wiki import */
    public static final String WIKI_TAG_EARTH = "earth";

    /** wiki import */
    public static final String WIKI_TAG_HTML = "html";

    /** wiki import */
    public static final String WIKI_TAG_MAPENTRY = "mapentry";

    /** wiki import */
    public static final String WIKI_TAG_DESCRIPTION = "description";

    public static final String WIKI_TAG_SNIPPET = "snippet";    

    /** _more_ */
    public static final String WIKI_TAG_SIMPLE = "simple";

    

    /** wiki import */
    public static final String WIKI_TAG_PROPERTIES = "properties";

    public static final String WIKI_TAG_LICENSE = "license";
    public static final String WIKI_TAG_USAGE = "usage";

    public static final String WIKI_TAG_COPYABLE = "copyable";    

    /**  */
    public static final String WIKI_TAG_DATAPOLICIES = "datapolicies";

    /** _more_ */
    public static final String WIKI_TAG_PROPERTY = "property";

    /** _more_ */
    public static final String WIKI_TAG_LABEL = "label";


    /** wiki import */
    public static final String WIKI_TAG_LINKS = "links";

    /** link property */
    public static final String WIKI_TAG_LINK = "link";

    /** list property */
    public static final String WIKI_TAG_LIST = "list";

    public static final String WIKI_TAG_NAVBAR  = "navbar";    


   /** wiki import */
    public static final String WIKI_TAG_ENTRYID = "entryid";

    public static final String WIKI_TAG_ALIAS = "alias";    

    /** wiki import */
    public static final String WIKI_TAG_LAYOUT = "layout";

    /** wiki import */
    public static final String WIKI_TAG_CHILDREN_GROUPS = "subgroups";

    /** wiki import */
    public static final String WIKI_TAG_CHILDREN_ENTRIES = "subentries";

    /** wiki import */
    public static final String WIKI_TAG_CHILDREN = "children";

    public static final String WIKI_TAG_CHILDREN_COUNT = "children_count";    

    /** wiki import */
    public static final String WIKI_TAG_URL = "url";

    /** _more_ */

    public static final String WIKI_TAG_RESOURCE = "resource";

    /**  */
    public static final String WIKI_TAG_ENTRYLINK = "entrylink";

    /** _more_ */
    public static final String WIKI_TAG_TOOLS = "tools";

    /** Upload property */
    public static final String WIKI_TAG_UPLOAD = "upload";


    public static final String WIKI_TAG_MAPPOPUP = "mappopup";

    public static final String WIKI_TAG_QRCODE = "qrcode";


    /** property delimiter */
    public static final String PROP_DELIM = ":";

    /**  */
    public static final String PROP_SHOW_TITLE = "wikiShowTitle";

    /** _more_ */
    public static final String PROP_GROUP_VAR = "groupvar";

    /** _more_ */
    public static final String ATTR_SEARCH_TYPE = PREFIX_SEARCH + "type";

    /** _more_ */
    public static final String ATTR_SEARCH_TEXT = PREFIX_SEARCH + "text";

    /** _more_ */
    public static final String ATTR_SEARCH_PARENT = PREFIX_SEARCH + "parent";

    /** _more_ */
    public static final String ATTR_SEARCH_NORTH = PREFIX_SEARCH + "north";

    /** _more_ */
    public static final String ATTR_SEARCH_URL = PREFIX_SEARCH + "url";

    /** _more_ */
    public static final String ATTR_SHOWFORM = "showForm";

    /** _more_ */
    public static final String ATTR_TEXT = "text";

    /** _more_ */
    public static final String ATTR_FORMOPEN = "formOpen";

    /** _more_ */
    public static final String ATTR_LAYOUTHERE = "layoutHere";


    //    public static final String ATTR_SEARCH_PARENT = PREFIX_SEARCH +"parent";

    /** default label */
    public static final String LABEL_LINKS = "Actions";


}

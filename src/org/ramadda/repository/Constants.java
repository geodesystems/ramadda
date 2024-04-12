/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository;


import org.ramadda.repository.util.RequestArgument;


/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public interface Constants {

    //j--

    /** _more_ */
    public static final String RESOURCE_ALLCSS = "allcss.css";

    /** _more_ */
    public static final String RESOURCE_ALLJS = "alljs.js";

    /** _more_ */
    public static final String WIKI_PREFIX = "<wiki>";


    /** _more_ */
    public static final String ICON_ACCESS = "fas fa-lock";


    /** _more_ */
    public static final String ICON_ROTATE =
        "/icons/arrow_rotate_clockwise.png";

    /** _more_ */
    public static final String ICON_ANTIROTATE =
        "/icons/arrow_rotate_anticlockwise.png";

    /** _more_ */
    public static final String ICON_ARROW = "fas fa-arrow-right";

    /** _more_ */
    public static final String ICON_GOOGLEEARTH = "/icons/googleearth.gif";

    /** _more_ */
    public static final String ICON_ASSOCIATION = "fas fa-arrow-right";

    /** _more_ */
    public static final String ICON_BLANK = "/icons/blank.gif";

    /** _more_ */
    public static final String ICON_CALENDAR = "fas fa-calendar-alt";

    /** _more_ */
    public static final String ICON_CART = "/icons/cart.png";

    /** _more_ */
    public static final String ICON_CART_ADD = "fas fa-cart-plus";

    /** _more_ */
    public static final String ICON_CART_DELETE = "/icons/cart_delete.png";

    /** _more_ */
    public static final String ICON_CHAT = "fas fa-comments";

    /** _more_ */
    public static final String ICON_TIMELINE = "/icons/timeline_marker.png";

    /** _more_ */
    public static final String ICON_CLOSE = "fas fa-window-close";

    /** _more_ */
    public static final String ICON_CLOUD = "fas fa-cloud";

    /** _more_ */
    public static final String ICON_COMMENTS = "fas fa-comments";

    /** _more_ */
    public static final String ICON_FTP = "/icons/server_go.png";

    /** _more_ */
    public static final String ICON_CSV = "fas fa-file-csv";

    /** _more_ */
    public static final String ICON_DATA = "fas fa-table";

    /** _more_ */
    public static final String ICON_DIF = "fas fa-info-circle";

    /** _more_ */
    public static final String ICON_DATEGRID = "/icons/dategrid.gif";

    /** _more_ */
    public static final String ICON_DELETE = "fas fa-cut";

    /** _more_ */
    public static final String ICON_DOWNARROW = "/icons/downarrow.gif";

    /** _more_ */
    public static final String ICON_DOWNLOAD = "fas fa-download";

    /** _more_ */
    public static final String ICON_DOWNDART = "/icons/downdart.gif";

    /** _more_ */
    public static final String ICON_EDIT = "fas fa-edit";

    /** _more_ */
    public static final String ICON_ENTRY = "fas fa-info-circle";

    /** _more_ */
    public static final String ICON_ENTRY_ADD = "fas fa-file-medical";

    /** _more_ */
    public static final String ICON_PUBLISH = "/icons/flag_green.png";

    /** _more_ */
    public static final String ICON_PLANVIEW = "/icons/planviewcontour.png";

    /** _more_ */
    public static final String ICON_ENTRY_UPLOAD = "/icons/flaggedentry.png";


    /** _more_ */
    public static final String ICON_FAVORITE = "fas fa-star";

    /** _more_ */
    public static final String ICON_FETCH = "fas fa-download";

    /** _more_ */
    public static final String ICON_FILE = "/icons/page.png";

    /** _more_ */
    public static final String ICON_FILELISTING = "fas fa-list-alt";

    /** _more_ */
    public static final String ICON_FOLDER = "/icons/folder.png";

    /** _more_ */
    public static final String ICON_FOLDER_ADD = "fas fa-folder-plus";

    /** _more_ */
    public static final String ICON_FOLDER_CLOSED = "/icons/folderclosed.png";

    /** _more_ */
    public static final String ICON_FOLDER_CLOSED_LOCKED =
        "/icons/folder_key.png";

    /** _more_ */
    public static final String ICON_FOLDER_OPEN = "/icons/folderopen.png";

    /** _more_ */
    public static final String ICON_GRAPH = "/icons/vector.png";

    /** _more_ */
    public static final String ICON_TABLE = "fas fa-table";

    /** _more_ */
    public static final String ICON_GRAYRECT = "/icons/grayrect.gif";

    /** _more_ */
    public static final String ICON_GRAYRECTARROW = "";

    /** _more_ */
    public static final String ICON_HOME = "fas fa-home";

    /** _more_ */
    public static final String ICON_HEADER = "/images/header.jpg";

    /** _more_ */
    public static final String ICON_HELP = "/icons/help.png";

    /** _more_ */
    public static final String ICON_IMAGE = "fas fa-image";

    /** _more_ */
    public static final String ICON_MOVIE = "fas fa-film";

    /** _more_ */
    public static final String ICON_IMPORT = "fas fa-file-import";

    /** _more_ */
    public static final String ICON_EXPORT = "fas fa-file-export";

    /** _more_ */
    public static final String ICON_IMAGES = "fas fa-images";

    /** _more_ */
    public static final String ICON_INFORMATION = ICON_ENTRY;

    /** _more_ */
    public static final String ICON_TREE = ICON_ENTRY;

    /** _more_ */
    public static final String ICON_KML = "fas fa-globe";

    /** _more_ */
    public static final String ICON_LCURVE = "/icons/blc.gif";

    /** _more_ */
    public static final String ICON_SYNTH_FILE = "/icons/server_database.png";

    /** _more_ */
    public static final String ICON_LEFT = "/icons/resultset_previous.png";

    /** _more_ */
    public static final String ICON_LINK = "/icons/link.png";

    /** _more_ */
    public static final String ICON_USERLINKS = "/icons/cog.png";

    /** _more_ */
    public static final String ICON_LIST = "/icons/list.gif";

    /** _more_ */
    public static final String ICON_LOG = "fas fa-scroll";

    /** _more_ */
    public static final String ICON_MAP = "fas fa-map";

    /** _more_ */
    public static final String ICON_MAP_NAV = "/icons/bullet_go.png";

    /** _more_ */
    public static final String ICON_METADATA = "fas fa-info";

    /** _more_ */
    public static final String ICON_METADATA_ADD = "fas fa-database";

    /** _more_ */
    public static final String ICON_METADATA_EDIT = "fas fa-database";



    /** _more_ */
    public static final String ICON_MOVE = "fas fa-copy";

    /** _more_ */
    public static final String ICON_NEW = "fas fa-plus";

    /** _more_ */
    //    public static final String ICON_PLUS = "fas fa-plus-square";
    public static final String ICON_PLUS = "fa-regular fa-square-plus" ;    


    /** _more_ */
    public static final String ICON_MINUS = "fa-regular fa-square-minus";

    /** _more_ */
    public static final String ICON_PROGRESS = "/icons/progress.gif";

    /** _more_ */
    public static final String ICON_QUESTION = "/icons/question.png";

    /** _more_ */
    public static final String ICON_RANGE = "fas fa-arrow-right";

    /** _more_ */
    public static final String ICON_RCURVE = "/icons/brc.gif";

    /** _more_ */
    public static final String ICON_RIGHT = "/icons/resultset_next.png";

    /** _more_ */
    public static final String ICON_RIGHTARROW = "fas fa-arrow-right";

    /** _more_ */
    public static final String ICON_ATOM = "fas fa-rss";

    /** _more_ */
    public static final String ICON_RSS = "fas fa-rss";

    /** _more_ */
    public static final String ICON_SEARCH = "fas fa-binoculars";

    /** _more_ */
    public static final String ICON_SEARCH_SMALL = "fas fa-search";

    /** _more_ */
    public static final String ICON_TEXT = "fas fa-align-left";

    /** _more_ */
    public static final String ICON_TOGGLEARROWDOWN = "fas fa-caret-down";

    /** _more_ */
    public static final String ICON_TOGGLEARROWRIGHT = "fas fa-caret-right";

    /** _more_ */
    public static final String ICON_TOOLS = "fas fa-tools";

    /** _more_ */
    public static final String ICON_UPARROW = "/icons/uparrow.gif";

    /** _more_ */
    public static final String ICON_UPDART = "/icons/updart.gif";

    /** _more_ */
    public static final String ICON_UPLOAD = "/icons/add.png";


    /**  */
    public static final String ICON_DIALOG_QUESTION = "fas fa-question";

    /**  */
    public static final String ICON_DIALOG_INFO = "fas fa-info";

    /** _more_ */
    public static final String ICON_DIALOG_ERROR =
        "fas fa-exclamation-triangle";

    /** _more_ */
    public static final String ICON_DIALOG_WARNING = "fas fa-exclamation";

    public static final String ICON_WIDTH = "18px";

    /** _more_ */
    public static final String ICON_WIKI = "/icons/wiki.png";

    /** _more_ */
    public static final String ICON_XML = "/icons/xml.png";

    /** _more_ */
    public static final String ICON_JSON = "/icons/json.png";

    /** _more_ */
    public static final String ICON_GEOJSON = "/icons/geojson.png";

    /** _more_ */
    public static final String ICON_ZIP = "fas fa-file-archive";

    /** _more_ */
    public static final String ICON_ZIPTREE = "fas fa-file-archive";



    //j++

    /** _more_ */
    public static final String ARG_ICON = "ramadda.icon";

    /** _more_ */
    public static final String ARG_AGREE = "agree";


    /** _more_ */
    public static final String ATTR_ADDMETADATA = "addmetadata";

    /** _more_ */
    public static final String ATTR_FILESIZE = "filesize";




    /** _more_ */
    public static final String ARG_SERVICEID = "serviceid";


    /** _more_ */
    public static final String PROP_INSTALL_PASSWORD =
        "ramadda.install.password";


    /** _more_ */
    public static final String PROP_MAKESNAPSHOT = "makesnapshot";

    /** _more_ */
    public static final String PROP_OVERRIDE_URL = "overrideurl";

    /** _more_ */
    public static final String ARG_SHOWCATEGORIES = "showCategories";

    /** _more_ */
    public static final String ARG_SHOWNEXT = "showNext";

    public static final String ARG_INDEX_IMAGE = "indeximage";

    /** _more_ */
    public static final String ARG_RETURNFILENAME = "returnfilename";

    /** _more_ */
    public static final String ATTR_ADDSHORTMETADATA = "addshortmetadata";

    public static final String ATTR_MAKETHUMBNAILS = "makethumbnails";

    /** _more_ */
    public static final String ATTR_ATTR = "attr";

    /** _more_ */
    public static final String ATTR_ATTR1 = ATTR_ATTR + "1";

    /** _more_ */
    public static final String ATTR_ATTR2 = ATTR_ATTR + "2";

    /** _more_ */
    public static final String ATTR_ATTR3 = ATTR_ATTR + "3";

    /** _more_ */
    public static final String ATTR_ATTR4 = ATTR_ATTR + "4";

    /** _more_ */
    public static final String ATTR_CANDONEW = "candonew";

    /** _more_ */
    public static final String ATTR_CANDOUPLOAD = "candoupload";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_CODE = "code";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";

    /** _more_ */
    public static final String ATTR_DIRECTORY = "directory";

    /** _more_ */
    public static final String ATTR_FILE_PATTERN = "filePattern";

    /**  */
    public static final String ATTR_UNIQUE = "unique";

    /** _more_ */
    public static final String ATTR_DB_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_FORUSER = "foruser";

    /** _more_ */
    public static final String ATTR_DB_NAME = "name";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_CATEGORY = "category";

    /** _more_ */
    public static final String ATTR_ENTRYORDER = "entryorder";

    /** _more_ */
    public static final String ATTR_EAST = "east";

    /** _more_ */
    public static final String ATTR_FILE = "file";

    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ATTR_GROUP = "group";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */

    public static final String ATTR_ORIGINALID = "originalid";

    /** _more_ */
    public static final String ATTR_INHERITED = "inherited";

    /** _more_ */
    public static final String ATTR_INPUTID = "inputid";

    /** _more_ */
    public static final String ATTR_LOCALFILE = "localfile";

    /** _more_ */
    public static final String ATTR_LOCALFILETOMOVE = "localfiletomove";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_NORTH = "north";

    /** _more_ */
    public static final String ATTR_LATITUDE = "latitude";

    /** _more_ */
    public static final String ATTR_LONGITUDE = "longitude";

    /** _more_ */
    public static final String ATTR_PARENT = "parent";

    /** _more_ */
    public static final String ATTR_SUPER = "super";

    /** _more_ */
    public static final String ATTR_RESOURCE = "resource";

    /** _more_ */
    public static final String ATTR_FILENAME = "filename";

    /** _more_ */
    public static final String ATTR_RESOURCE_TYPE = "resource_type";


    /** _more_ */
    public static final String ATTR_SERVER = "server";

    /** _more_ */
    public static final String ATTR_SOUTH = "south";

    /** _more_ */
    public static final String ATTR_TARGET = "target";

    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_TO = "to";

    /** _more_ */
    public static final String ATTR_TODATE = "todate";

    /** _more_ */
    public static final String ATTR_PATH = "path";

    /** _more_ */
    public static final String ATTR_CREATEDATE = "createdate";

    /** _more_ */
    public static final String ATTR_CHANGEDATE = "changedate";

    /** _more_ */
    public static final String ATTR_TOOLTIP = "tooltip";

    /** _more_ */
    public static final String ATTR_TYPE = "type";


    /** _more_ */
    public static final String ATTR_ISGROUP = "isgroup";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_SIZE = "size";

    /** _more_ */
    public static final String ATTR_DOWNLOAD = "download";

    /** _more_ */
    public static final String ATTR_WEST = "west";

    /** _more_ */
    public static final String ATTR_ALTITUDE = "altitude";

    /** _more_ */
    public static final String ATTR_ALTITUDE_TOP = "altitudetop";

    /** _more_ */
    public static final String ATTR_ALTITUDE_BOTTOM = "altitudebottom";


    /** _more_ */
    public static final String ARG_DISPLAY = "display";

    /** _more_ */
    public static final String DISPLAY_FULL = "full";

    /** _more_ */
    public static final String DISPLAY_SMALL = "small";

    /** _more_ */
    public static final String ARG_DECORATE = "decorate";

    /** _more_ */
    public static final String ARG_TREEVIEW = "treeview";

    /** _more_ */
    public static final String ARG_DEPTH = "depth";

    /** _more_ */
    public static final String ARG_FULLURL = "fullurl";


    /** _more_ */
    public static final String ARG_ACTION = "action";

    /** _more_ */
    public static final String ARG_ACTION_FORCE = "action.force";

    /** _more_ */
    public static final String ARG_ACTION_ASSOCIATE = "action.associate";

    /** _more_ */
    public static final String ARG_ACTION_COPY = "action.copy";

    /** _more_ */
    public static final String ARG_ACTION_ID = "actionid";

    /** _more_ */
    public static final String ARG_ACTION_MOVE = "action.move";

    /** _more_ */
    public static final String ARG_ADD = "add";

    /** _more_ */
    public static final String ARG_ADMIN = "admin";


    /** _more_ */
    public static final String ARG_ADMIN_WHAT = "what";

    /** _more_ */
    public static final String ARG_PLUGIN_FILE = "plugin.file";

    /** _more_ */
    public static final String ARG_ALLENTRIES = "allentries";

    /** _more_ */
    public static final String ARG_ENTRYTYPE = "entrytype";

    /** _more_ */
    public static final String ARG_ANCESTOR = "ancestor";
    public static final String ARG_DESCENDENT = "descendent";    

    /** _more_ */
    public static final String ARG_APPLET = "applet";

    /** _more_ */
    public static final String ARG_AREA = "area";

    /** _more_ */
    public static final String ARG_LOCATION = "location";

    /** _more_ */
    public static final String ARG_LOCATION_LATITUDE = "location.latitude";

    /** _more_ */
    public static final String ARG_LOCATION_LONGITUDE = "location.longitude";


    /** _more_ */
    public static final String ARG_LATITUDE = "latitude";

    /** _more_ */
    public static final String ARG_LONGITUDE = "longitude";

    /** _more_ */
    public static final String ARG_BBOX = "bbox";

    /** _more_ */
    public static final String ARG_DEFAULTBBOX = "defaultbbox";

    /** _more_ */
    public static final String ARG_AREA_MODE = "areamode";

    /** _more_ */
    public static final String VALUE_BLANK = "-blank-";

    /** _more_ */
    public static final String VALUE_AREA_CONTAINS = "contains";

    /** _more_ */
    public static final String VALUE_AREA_OVERLAPS = "overlaps";


    /** _more_ */
    public static final String ARG_AREA_EAST = ARG_AREA + "_east";

    /** _more_ */
    public static final String ARG_AREA_NORTH = ARG_AREA + "_north";

    /** _more_ */
    public static final String ARG_AREA_SOUTH = ARG_AREA + "_south";

    /** _more_ */
    public static final String ARG_AREA_WEST = ARG_AREA + "_west";

    /** _more_ */
    public static final String ARG_ASCENDING = "ascending";
    public static final String ARG_DESCENDING = "descending";    

    public static final String DIR_UP = "up";
    public static final String DIR_DOWN = "down";


    /** _more_ */
    public static final String ARG_ASSOCIATION = "association";

    /** _more_ */
    public static final String ARG_AUTH_PASSWORD = "auth.password";

    /** _more_ */
    public static final String ARG_AUTH_USER = "auth.user";

    /** _more_ */
    public static final String ARG_BYTES = "bytes";

    /** _more_ */
    public static final String ARG_CANCEL = "cancel";

    public static final String ARG_UNDO = "undo";

    /** _more_ */
    public static final String ARG_EXECUTE = "execute";

    /** _more_ */
    public static final String ARG_CONFIRM = "confirm";

    /** _more_ */
    public static final String ARG_COLUMNS = "columns";

    /** _more_ */
    public static final String ARG_COMPRESS = "compress";

    /** _more_ */
    public static final String ARG_CANCEL_DELETE = "canceldelete";


    public static final String ARG_DO_CHILDREN = "dochildren";


    /** _more_ */
    public static final String ARG_CHANGE = "change";

    /** _more_ */
    public static final String ARG_COLLECTION = "collection";

    /** _more_ */
    public static final String ARG_COMMENT = "comment";

    /** _more_ */
    public static final String ARG_COMMENTS = "showcomments";

    /** _more_ */
    public static final String ARG_COMMENT_ID = "comment_id";

    /** _more_ */
    public static final String ARG_CONTRIBUTION_FROMEMAIL =
        "contribution.fromemail";

    /** _more_ */
    public static final String ARG_CONTRIBUTION_FROMNAME =
        "contribution.fromname";

    /** _more_ */
    public static final String ARG_ENTRYORDER = "entryorder";


    /** _more_ */
    public static final String ARG_CREATEDATE = "createdate";

    /** _more_ */
    public static final String ARG_CHANGEDATE = "changedate";

    /** _more_ */
    public static final String ARG_CREATOR = "creator";

    /** _more_ */
    public static final String ARG_DATASET = "dataset";

    /** _more_ */
    public static final String ARG_CATEGORY = "category";

    /** _more_ */
    public static final String ARG_CATEGORY_SELECT = "category.select";

    /** _more_ */
    public static final String ARG_DATE = "date";

    /**  */
    public static final String ARG_TAGS = "tags";

    /** _more_ */
    public static final String ARG_DATE_OVERLAPS = "date.overlaps";

    /** _more_ */
    public static final String ARG_DATE_SEARCHMODE = "date.searchmode";

    /** _more_ */
    public static final String ARG_DATE_NODATAMODE = "date.nodatamode";

    /** _more_ */
    public static final String VALUE_NODATAMODE_NONE = "none";

    /** _more_ */
    public static final String VALUE_NODATAMODE_INCLUDE = "include";




    /** _more_ */
    public static final String DATE_SEARCHMODE_OVERLAPS = "overlaps";

    /** _more_ */
    public static final String DATE_SEARCHMODE_CONTAINEDBY = "containedby";

    /** _more_ */
    public static final String DATE_SEARCHMODE_CONTAINS = "contains";

    /** _more_ */
    public static final String DATE_SEARCHMODE_DEFAULT =
        DATE_SEARCHMODE_OVERLAPS;


    /** _more_ */
    public static final String ARG_DATE_PATTERN = "date.pattern";

    /** _more_ */
    public static final String ARG_DAY = "day";

    /** _more_ */
    public static final String ARG_DELETE = "delete";

    /** _more_ */
    public static final String ARG_DELETE_CONFIRM = "delete.confirm";

    /** _more_ */
    public static final String ARG_DESCRIPTION = "description";

    /** _more_ */
    public static final String ARG_ISWIKI = "iswiki";

    /** _more_ */
    public static final String ARG_WIKITEXT = "wikitext";

    /** _more_ */
    public static final String ARG_EAST = "east";

    /** _more_ */
    public static final String ARG_ELEMENT = "element";

    /** _more_ */
    public static final String ARG_EDIT_METADATA = "edit.metadata";

    /** _more_ */
    public static final String ARG_EMBEDDED = "embedded";

    /** _more_ */
    public static final String ARG_ENTRYID = "entryid";


    /** _more_ */
    public static final String ARG_SELENTRY = "selentry";

    /** _more_ */
    public static final String ARG_ALLENTRY = "allentry";

    /** _more_ */
    public static final String ARG_ENTRY_TIMESTAMP = "entry.timestamp";

    /** _more_ */
    public static final String ARG_LOCALEID = "localeid";

    /** _more_ */
    public static final String ARG_ENTRYIDS = "entryids";

    /** _more_ */
    public static final String ARG_EXACT = "exact";

    /** _more_ */
    public static final String ARG_ISREGEXP = "isregexp";

    /** _more_ */
    public static final String ARG_FAVORITE_ADD = "user.favorite.add";

    /** _more_ */
    public static final String ARG_FAVORITE_DELETE = "user.favorite.delete";

    /** _more_ */
    public static final String ARG_FAVORITE_ID = "user.favorite.id";

    /** _more_ */
    public static final String ARG_FILE = "file";

    /** _more_ */
    public static final String ARG_FILESUFFIX = "filesuffix";

    /** _more_ */
    public static final String ARG_MAXFILESIZE = "maxfilesize";

    /** _more_ */
    public static final String ARG_FILE_UNZIP = "file.unzip";

    public static final String ARG_STRIPEXIF = "stripexif";    

    /** _more_ */
    public static final String ARG_MAKENAME = "makename";

    /** _more_ */
    public static final String ARG_DELETEFILE = "deletefile";

    /** _more_ */
    public static final String ARG_FILE_PRESERVEDIRECTORY =
        "file.preservedirectoryfile";


    /** _more_ */
    public static final String ARG_FORMAT = "format";


    /** _more_ */
    public static final String ARG_FORM_ADVANCED = "form.advanced";

    /** _more_ */
    public static final String ARG_FORM_METADATA = "form.metadata";

    /** _more_ */
    public static final String ARG_FORM_TYPE = "form.type";

    /** _more_ */
    public static final String ARG_FROM = "from";

    /** _more_ */
    public static final String ARG_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ARG_FROMDATE_TIME = ARG_FROMDATE + ".time";


    /** _more_ */
    public static final String ARG_DATA_DATE = "datadate";

    /** _more_ */
    public static final String ARG_CREATE_DATE = "createdate";

    /** _more_ */
    public static final String ARG_CHANGE_DATE = "changedate";




    /** _more_ */
    public static final String ARG_FROMLOGIN = "user.fromlogin";

    /** _more_ */
    public static final String ARG_GROUP = "group";

    /** _more_ */
    public static final String ARG_DEST_ENTRY = "destentry";

    /** _more_ */
    public static final String ARG_GROUPID = "groupid";

    /** _more_ */
    public static final String ARG_GROUP_CHILDREN = "group_children";

    public static final String ARG_PATHTEMPLATE = "pathtemplate";

    /** _more_ */
    public static final String ARG_HARVESTER_CLASS = "harvester.class";

    /** _more_ */
    public static final String ARG_HARVESTER_GETXML = "harvester.getxml";

    /** _more_ */
    public static final String ARG_HARVESTER_ID = "harvester.id";

    /** _more_ */
    public static final String ARG_HARVESTER_REDIRECTTOEDIT =
        "harvester.redirecttoedit";

    /** _more_ */
    public static final String ARG_HARVESTER_XMLFILE = "harvester.xmlfile";

    /** _more_ */
    public static final String ARG_HEIGHT = "height";

    /** _more_ */
    public static final String ARG_IMAGEHEIGHT = "imageheight";

    /** _more_ */
    public static final String ARG_IMAGEWIDTH = "imagewidth";
    public static final String ARG_SERVERIMAGEWIDTH = "serverimagewidth";    

    /** _more_ */
    public static final String ARG_INCLUDENONGEO = "includenongeo";

    /** _more_ */
    public static final String ARG_LABEL = "label";

    /** _more_ */
    public static final String ARG_LANGUAGE = "language";

    /** _more_ */
    public static final String ARG_LATEST = "latest";

    /** _more_ */
    public static final String ARG_LATESTOPENDAP = "latestopendap";

    /** _more_ */
    public static final String ARG_LAYOUT = "layout";

    /** _more_ */
    public static final String ARG_LIMIT = "limit";

    /** _more_ */
    public static final String ARG_SERVERFILE = "serverfile";

    /** _more_ */
    public static final String ARG_SERVERFILE_HARVEST = "serverfile_harvest";

    /** _more_ */
    public static final String ARG_SERVERFILE_PATTERN = "serverfile_pattern";

    /** _more_ */
    public static final String ARG_LOG = "log";

    /** _more_ */
    public static final String ARG_MAX = "max";

    public static final String ARG_MARKER = "marker";
    public static final String ARG_PREVMARKERS = "prevmarkers";


    /** _more_ */
    public static final String ARG_LAST = "last";

    /** _more_ */
    public static final String ARG_MAXLAT = "maxlat";

    /** _more_ */
    public static final String ARG_MAXLON = "maxlon";

    //These can be used as well for search and subset. They are defined in the repository.properties 

    /** _more_ */
    public static final String ARG_MAXLATITUDE = "maxlatitude";

    /** _more_ */
    public static final String ARG_MINLATITUDE = "minlatitude";

    /** _more_ */
    public static final String ARG_MAXLONGITUDE = "maxlongitude";

    /** _more_ */
    public static final String ARG_MINLONGITUDE = "minlongitude";


    /** _more_ */
    public static final String ARG_MESSAGE = "message";


    /** _more_ */
    public static final String ARG_METADATA_ADD = "metadata.add";



    
    /** _more_ */
    public static final String ARG_FROMHARVESTER = "fromharvester";




    /** _more_ */
    public static final String ARG_METADATA_ADDSHORT = "metadata.addshort";

    /** _more_ */
    public static final String ARG_METADATA_ADDTOPARENT =
        "metadata.addtoparent";

    /** _more_ */
    public static final String ARG_METADATA_CLIPBOARD_COPY =
        "metadata.clipboard.copy";

    /** _more_ */
    public static final String ARG_METADATA_CLIPBOARD_PASTE =
        "metadata.clipboard.paste";

    /** _more_ */
    public static final String ARG_METADATA_ATTR = "metadata_attr";


    /** _more_ */
    public static final String ARG_METADATA_ATTR1 = "metadata_attr1";

    /** _more_ */
    public static final String ARG_METADATA_ATTR2 = "metadata_attr2";

    /** _more_ */
    public static final String ARG_METADATA_ATTR3 = "metadata_attr3";

    /** _more_ */
    public static final String ARG_METADATA_ATTR4 = "metadata_attr4";

    /** _more_ */
    public static final String ARG_METADATA_DELETE = "metadata_delete";

    /** _more_ */
    public static final String ARG_METADATA_ID = "metadata_id";

    /** _more_ */
    public static final String ARG_METADATA_INHERITED = "metadata_inherited";

    /** _more_ */
    public static final String ARG_METADATA_TYPE = "metadata_type";

    /** _more_ */
    public static final String ARG_MINLAT = "minlat";

    /** _more_ */
    public static final String ARG_MINLON = "minlon";

    /** _more_ */
    public static final String PROP_SYSTEM_MESSAGE = "system.message";


    /** _more_ */
    public static final String PROP_ENTRY_TABLE_SHOW_CREATEDATE =
        "ramadda.entry.table.show.createdate";


    /** _more_ */
    public static final String PROP_CREATED_DISPLAY_MODE =
        "ramadda.created.display";


    /** _more_ */
    public static final String PROP_PASSPHRASE = "ramadda.passphrase";


    /** _more_ */
    public static final String PROP_MONITOR_ENABLE_EXEC =
        "ramadda.monitor.enable.exec";

    /** _more_ */
    public static final String PROP_PROXY_WHITELIST =
        "ramadda.proxy.whitelist";

    /** _more_ */
    public static final String PROP_READ_ONLY = "ramadda.readonly";

    /** _more_ */
    public static final String PROP_DOCACHE = "ramadda.docache";

    /** _more_ */
    public static final String PROP_ENABLE_FILE_LISTING =
        "ramadda.enable_file_listing";

    /** _more_ */
    public static final String PROP_ENABLE_HOSTNAME_MAPPING =
        "ramadda.enable_hostname_mapping";

    /** _more_ */
    public static final String PROP_SHOW_HELP = "ramadda.html.show.help";

    /** _more_ */
    public static final String PROP_SHOW_CART = "ramadda.html.show.cart";







    /** _more_ */
    public static final String ARG_MONTH = "month";

    /** _more_ */
    public static final String ARG_MOVE_CONFIRM = "move.confirm";

    /** _more_ */
    public static final String ARG_NAME = "name";


    public static final String ARG_BULKUPLOAD = "bulkupload";

    /** _more_ */
    public static final String ARG_NOREDIRECT = "noredirect";

    /** _more_ */
    public static final String ARG_NEW = "new";

    /** _more_ */
    public static final String ARG_NEXT = "next";

    /** _more_ */
    public static final String ARG_NODETYPE = "nodetype";

    /** _more_ */
    public static final String ARG_DETAILS = "details";

    /** _more_ */
    public static final String ARG_USER_MESSAGE = "usermessage";

    public static final String ARG_CAPTCHA_INDEX = "captchindex";
    public static final String ARG_CAPTCHA_RESPONSE = "captchresponse";    

    /** _more_ */
    public static final String ARG_USER_ID = "user_id";


    /** _more_ */
    public static final String ARG_USER_SENDMAIL = "usersendmail";

    /** _more_ */
    public static final String ARG_USER_HOME = "userhome";

    /** _more_ */
    public static final String ARG_ALTITUDE = "altitude";

    /** _more_ */
    public static final String ARG_ALTITUDE_TOP = "altitude.top";

    /** _more_ */
    public static final String ARG_ALTITUDE_BOTTOM = "altitude.bottom";


    public static final String ARG_SIZE_MIN = "sizemin";
    public static final String ARG_SIZE_MAX = "sizemax";    

    /** _more_ */
    public static final String ARG_SETFROMCHILDREN = "setfromchildren";

    /** _more_ */
    public static final String ARG_SETFROMCHILDREN_RECURSE =
        "setfromchildren.recurse";


    public static final String ARG_SEARCH_POLYGON = "search_polygon";

    /** _more_ */
    public static final String ARG_NORTH = "north";

    /** _more_ */
    public static final String ARG_OK = "ok";

    /** _more_ */
    public static final String ARG_ONLYGROUPS = "onlygroups";

    /** _more_ */
    public static final String ARG_ORDERBY = "orderby";

    /** _more_ */
    public static final String ARG_GROUPBY = "group_by";

    /** _more_ */
    public static final String ARG_AGG = "group_agg";


    /** _more_ */
    public static final String ARG_AGG_TYPE = "group_agg_type";

    /** _more_ */
    public static final String ARG_OUTPUT = "output";

    /** _more_ */
    public static final String ARG_PREVIOUS = "previous";

    /** _more_ */
    public static final String ARG_VISIBLE = "visible";

    /** _more_ */
    public static final String ARG_PRODUCT = "product";

    public static final String ARG_DATEFORMAT="dateformat";

    /** _more_ */
    public static final String ARG_PUBLISH = "publish";


    /** _more_ */
    public static final String ARG_SNAPSHOT_TYPE = "snapshottype";

    /** _more_ */
    public static final String SNAPSHOT_ENTRY = "snapshotentry";

    /** _more_ */
    public static final String SNAPSHOT_FILE = "snapshotfile";

    /** _more_ */
    public static final String SNAPSHOT_EXPORT = "snapshotexport";


    /** _more_ */

    public static final String ARG_SUBMIT_PUBLISH = "submit.publish";

    /** _more_ */
    public static final String ARG_PUBLISH_ENTRY = "publish_entry";

    /** _more_ */
    public static final String ARG_PUBLISH_NAME = "publish.name";

    /** _more_ */
    public static final String ARG_PUBLISH_DESCRIPTION =
        "publish.description";




    /** _more_ */
    public static final String ARG_JUSTPUBLISH = "justpublish";

    /** _more_ */
    public static final String ARG_QUERY = "query";

    /** _more_ */
    public static final String ARG_RECURSE = "recurse";

    /** _more_ */
    public static final String ARG_REDIRECT = "redirect";

    /** _more_ */
    public static final String ARG_RELATIVEDATE = "relativedate";

    /** _more_ */
    public static final String ARG_REQUIRED = "required";

    /** _more_ */
    public static final String ARG_RESOURCE = "resource";

    /** _more_ */
    public static final String ARG_RESOURCE_DOWNLOAD = "resource.download";
    public static final String ARG_DOWNLOAD_FILE = "downloadfile";

    /** _more_ */
    public static final String ARG_RESPONSE = "response";

    /** _more_ */
    public static final String ARG_RESPONSETYPE = "responsetype";

    /** _more_ */
    public static final String ARG_ROLES = "roles";

    /** _more_ */
    public static final String ARG_SEARCHMETADATA = "searchmetadata";

    /** _more_ */
    public static final String ARG_SEARCH_TYPE = "search.type";

    /** _more_ */
    public static final String ARG_SEARCH_SHOWFORM = "search.showform";

    /** _more_ */
    public static final String ARG_SEARCH_SHOWHEADER = "search.showheader";

    /** _more_ */
    public static final String SEARCH_TYPE_TEXT = "search.type.text";

    /** _more_ */
    public static final String SEARCH_TYPE_ADVANCED = "search.type.advanced";


    /** _more_ */
    public static final String ARG_SELECTTYPE = "selecttype";

    /** _more_ */
    public static final String ARG_SESSIONID = "sessionid";

    /** _more_ */
    public static final String ARG_ANONYMOUS = "anonymous";

    /** _more_ */
    public static final String ARG_AUTHTOKEN = "authtoken";

    /** _more_ */
    public static final String ARG_REMOVESESSIONID = "removesessionid";

    /** _more_ */
    public static final String ARG_SHORT = "short";

    /** _more_ */
    public static final String ARG_SHOWENTRYSELECTFORM =
        "showentryselectform";

    /** _more_ */
    public static final String ARG_SHOWLINK = "showlink";

    /** _more_ */
    public static final String ARG_DISPLAYLINK = "displaylink";

    /** _more_ */
    public static final String ARG_SHOWTAB = "showtab";

    /** _more_ */
    public static final String ARG_SHOWMETADATA = "showmetadata";

    /** _more_ */
    public static final String ARG_SHOWYEAR = "showyear";

    /** _more_ */
    public static final String ARG_SHOW_ASSOCIATIONS = "showassociations";

    /** _more_ */
    public static final String ARG_SKIP = "skip";

    /** _more_ */
    public static final String ARG_SOUTH = "south";

    /** _more_ */
    public static final String ARG_SQLFILE = "sqlfile";

    /** _more_ */
    public static final String ARG_SSLOK = "sslok";

    /** _more_ */
    public static final String ARG_STATION = "station";

    /** _more_ */
    public static final String ARG_STEP = "step";

    /** _more_ */
    public static final String ARG_SUBJECT = "subject";

    /** _more_ */
    public static final String ARG_SUBMIT = "submit";

    /** _more_ */
    public static final String ARG_SAVENEXT = "savenext";

    /** _more_ */
    public static final String ARG_TEMPLATE = "template";
    public static final String ARG_TESTNEW = "testnew";

    /** _more_ */
    public static final String ARG_USER_TEMPLATE = "usertemplate";

    /** _more_ */
    public static final String ARG_TARGET = "target";

    /** _more_ */
    public static final String ARG_TEXT = "text";

    /** title argument */
    public static final String ARG_TITLE = "title";

    /** _more_ */
    public static final String ARG_THUMBNAIL = "thumbnail";

    /** _more_ */
    public static final String ARG_TO = "to";

    /** _more_ */
    public static final String ARG_TODATE = "todate";

    /** _more_ */
    public static final String ARG_TODATE_TIME = ARG_TODATE + ".time";

    /** _more_ */
    public static final String ARG_TONAME = "toname";

    /** _more_ */
    public static final String ARG_TOPLEVEL = "toplevel";

    /** _more_ */
    public static final String ARG_TYPE = "type";

    /** _more_ */
    public static final String ARG_TYPE_GUESS = "type.guess";

    /** _more_ */
    public static final String ARG_TYPE_FREEFORM = "type.freeform";

    /** _more_ */
    public static final String ARG_TYPE_EXCLUDE = "type.exclude";

    /** _more_ */
    public static final String ARG_URL = "url";

    /** _more_ */
    public static final String ARG_MD5 = "md5";

    /** _more_ */
    public static final String ATTR_MD5 = "md5";

    /** _more_ */
    public static final String ARG_FILESIZE = "filesize";

    /** _more_ */
    public static final String ARG_USER = "user";



    /** _more_ */
    public static final String ARG_PASSWORD = "password";

    /** _more_ */
    public static final String ARG_USER_PASSWORD = "user_password";

    /** _more_ */
    public static final String ARG_USER_PASSWORD1 = "user_password1";

    /** _more_ */
    public static final String ARG_USER_PASSWORD2 = "user_password2";

    /** _more_ */
    public static final String ARG_USER_PASSWORDKEY = "user_passwordkey";

    /** _more_ */
    public static final String ARG_USER_QUESTION = "user_question";

    /** _more_ */
    public static final String ARG_USER_ROLES = "user_roles";

    /** _more_ */
    public static final String ARG_VARIABLE = "variable";

    /** _more_ */
    public static final String ARG_WAIT = "wait";

    /** _more_ */
    public static final String ARG_WEST = "west";

    /** _more_ */
    public static final String ARG_WHAT = "what";

    /** _more_ */
    public static final String ARG_WIDTH = "width";


    /** _more_ */
    public static final String ARG_YEAR = "year";



    /** _more_ */
    public static final String TAG_ASSOCIATION = "association";

    /** _more_ */
    public static final String TAG_ASSOCIATIONS = "associations";

    /** _more_ */
    public static final String TAG_DESCRIPTION = "description";

    /** _more_ */
    public static final String TAG_SERVICE = "service";

    /** _more_ */
    public static final String TAG_EDGE = "edge";

    /** _more_ */
    public static final String TAG_ENTRIES = "entries";

    /** _more_ */
    public static final String TAG_WIKITEXT = "wikitext";

    /** _more_ */
    public static final String TAG_ENTRY = "entry";

    /** _more_ */
    public static final String TAG_GROUP = "group";

    /** _more_ */
    public static final String TAG_GROUPS = "groups";


    /** _more_ */
    public static final String TAG_METADATA = "metadata";


    /** _more_ */
    public static final String TAG_METADATAHANDLER = "metadatahandler";

    /** _more_ */
    public static final String TAG_NODE = "node";

    /** _more_ */
    public static final String TAG_OUTPUTHANDLER = "outputhandler";

    /** _more_ */
    public static final String TAG_RESPONSE = "response";

    /** _more_ */
    public static final String TAG_TAG = "tag";

    /** _more_ */
    public static final String TAG_TAGS = "tags";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_TYPES = "types";


    /** _more_ */
    public static final String PROP_FTP_PORT = "ramadda.ftp.port";

    /** _more_ */
    public static final String PROP_FTP_PASSIVEPORTS =
        "ramadda.ftp.passiveports";

    /** _more_ */
    public static final String PROP_SHOWMAP = "ramadda.showmap";

    /** _more_ */
    public static final String PROP_NOSTYLE = "nostyle";


    /** _more_ */
    public static final String PROP_SEARCH_LUCENE_ENABLED =
        "ramadda.search.lucene.enabled";

    /** _more_ */
    public static final String PROP_SEARCH_SHOW_METADATA =
        "ramadda.search.show.metadata";

    /** _more_ */
    public static final String PROP_PROPERTIES = "ramadda.properties";


    /** _more_ */
    public static final String PROP_BUILD_VERSION = "ramadda.build.version";

    /** _more_ */
    public static final String PROP_BUILD_DATE = "ramadda.build.date";

    /** _more_ */
    public static final String PROP_JAVA_VERSION = "java.version";

    /** _more_ */
    public static final String PROP_ACCESS_ADMINONLY =
        "ramadda.access.adminonly";

    /** _more_ */
    public static final String PROP_ACCESS_NOBOTS = "ramadda.access.nobots";

    public static final String PROP_ACCESS_NOGOOGLEBOT = "ramadda.access.nogooglebot";    

    /** _more_ */

    public static final String PROP_ACCESS_REQUIRELOGIN =
        "ramadda.access.requirelogin";

    /** _more_ */
    public static final String PROP_PASSWORD_OLDMD5 =
        "ramadda.password.oldmd5";

    /** _more_ */
    public static final String PROP_ACCESS_ALLSSL = "ramadda.access.allssl";

    /** _more_ */
    public static final String PROP_ADMIN = "ramadda.admin";

    /** _more_ */
    public static final String PROP_ADMIN_INCLUDESQL =
        "ramadda.admin.includesql";

    /** _more_ */
    public static final String ARG_SHUTDOWN_CONFIRM = "shutdown.confirm";


    /** _more_ */
    public static final String ARG_MAP_ICONSONLY = "iconsonly";

    /** _more_ */
    public static final String ARG_MAP_EXTRA = "map.extra";

    public static final String ARG_COPY_DEEP = "copydeep";
    public static final String ARG_COPY_SIZE_LIMIT = "sizelimit";
    public static final String ARG_EXCLUDES = "excludes";    
    public static final String ARG_COPY_DO_METADATA = "dometadata";



    /** _more_ */
    public static final String PROP_ADMIN_EMAIL = "ramadda.admin.email";

    /** _more_ */
    public static final String PROP_ADMIN_PHRASES = "ramadda.admin.phrases";

    /** _more_ */
    public static final String PROP_ADMIN_SMTP = "ramadda.admin.smtp";

    /** _more_ */
    public static final String PROP_REGISTER_KEY = "ramadda.register.key";


    /** _more_ */
    public static final String PROP_SMTP_USER = "ramadda.admin.smtp.user";

    /** _more_ */
    public static final String PROP_SMTP_PASSWORD =
        "ramadda.admin.smtp.password";

    /** _more_ */
    public static final String PROP_API = "ramadda.api";

    /** _more_ */
    public static final String PROP_DATE_FORMAT = "ramadda.date.format";

    /** _more_ */
    public static final String PROP_DATE_SHORTFORMAT =
        "ramadda.date.shortformat";

    /** _more_ */
    public static final String PROP_DB = "ramadda.db";

    /** _more_ */
    public static final String PROP_DB_CANCACHE = "ramadda.db.cancache";

    /** _more_ */
    public static final String PROP_DB_POOL_MAXACTIVE =
        "ramadda.db.pool.maxactive";

    /** _more_ */
    public static final String PROP_DB_POOL_MAXIDLE =
        "ramadda.db.pool.maxidle";

    /** _more_ */
    public static final String PROP_DB_POOL_TIMEUNTILCLOSED =
        "ramadda.db.pool.timeuntilclosed";


    /** _more_ */
    public static final String PROP_DB_DERBY_HOME = "ramadda.db.derby.home";


    /** _more_ */
    public static final String PROP_DEBUG = "ramadda.debug";

    /** _more_ */
    public static final String PROP_DOWNLOAD_ASFILES =
        "ramadda.download.asfiles";

    /** _more_ */
    public static final String PROP_DOWNLOAD_OK = "ramadda.download.ok";

    /** _more_ */
    public static final String PROP_MINIFIED = "ramadda.minified";

    /** _more_ */
    public static final String PROP_CDNOK = "ramadda.cdnok";

    /** _more_ */
    public static final String PROP_TIMEZONE = "ramadda.timezone";

    /** _more_ */
    public static final String PROP_ENTRY_TOP = "ramadda.entry.top";


    /** _more_ */
    public static final String PROP_ENTRY_HEADER = "ramadda.entryheader";

    public static final String PROP_ENTRY_NAME = "ramadda.entryname";
    public static final String PROP_ENTRY_URL = "ramadda.entryurl";
    public static final String PROP_ENTRY_MENU = "ramadda.menu";                

    /** _more_ */
    public static final String PROP_ENTRY_FOOTER = "ramadda.entryfooter";


    /** _more_ */
    public static final String PROP_ENTRY_BREADCRUMBS =
        "ramadda.entry.breadcrumbs";

    public static final String PROP_ENTRY_POPUP =
        "ramadda.entry.popup";    

    /** _more_ */
    public static final String PROP_FACEBOOK_CONNECT_KEY =
        "ramadda.facebook.connect.key";

    /** _more_ */
    public static final String PROP_GOOGLEAPIKEYS = "ramadda.googleapikeys";

    /** _more_ */
    public static final String PROP_HARVESTERS = "ramadda.harvesters";


    /** _more_ */
    public static final String PROP_ALWAYS_HTTPS = "ramadda.always_https";

    /** _more_ */
    public static final String PROP_HARVESTERS_ACTIVE =
        "ramadda.harvesters.active";

    /** the url that the logo goes to when clicked */
    public static final String PROP_LOGO_URL = "ramadda.logo.url";

    /** the logo image property */
    public static final String PROP_LOGO_IMAGE = "ramadda.logo.image";



    /** The map layers property */
    public static final String PROP_MAP_LAYERS = "ramadda.map.layers";

    /** The default map layer property */
    public static final String PROP_MAP_DEFAULTLAYER =
        "ramadda.map.defaultlayer";

    /** _more_ */
    public static final String PROP_HOSTNAME = "ramadda.hostname";

    /** _more_ */
    public static final String PROP_HTML_FOOTER = "ramadda.html.footer";


    /** _more_ */
    public static final String PROP_HTML_MIMEPROPERTIES =
        "ramadda.html.mimeproperties";


    /** _more_ */
    public static final String PROP_HTML_TEMPLATE = "ramadda.html.template";

    /** _more_ */
    public static final String PROP_HTML_TEMPLATES = "ramadda.html.templates";

    /** _more_ */
    public static final String PROP_HTML_TEMPLATE_DEFAULT =
        "ramadda.html.template.default";

    /** _more_ */
    public static final String PROP_HTML_URLBASE = "ramadda.html.urlbase";



    /** _more_ */
    public static final String PROP_LANGUAGE = "ramadda.language";

    /** _more_ */
    public static final String PROP_LANGUAGE_DEFAULT =
        "ramadda.language.default";



    /** _more_ */
    public static final String PROP_LOCALFILEPATHS = "ramadda.localfilepaths";

    /** _more_ */
    public static final String PROP_LOG_TOSTDERR = "ramadda.log.tostderr";

    /** _more_ */
    public static final String PROP_METADATA = "ramadda.metadata";

    /** _more_ */
    public static final String PROP_NAVSUBLINKS = "ramadda.navsublinks";

    /** _more_ */
    public static final String PROP_OUTPUTHANDLERS = "ramadda.outputhandlers";

    /** _more_ */
    public static final String PROP_PORT = "ramadda.port";


    /** _more_ */
    public static final String PROP_USE_FIXED_HOSTNAME =
        "ramadda.usefixedhostname";

    /** _more_ */
    public static final String PROP_CORS_OK = "ramadda.cors.ok";


    /** _more_ */
    public static final String PROP_RATINGS_ENABLE = "ramadda.ratings.enable";

    /** _more_ */
    public static final String PROP_REPOSITORY_HOME = "ramadda_home";
    public static final String PROP_REPOSITORY_HOME_UPPER = "RAMADDA_HOME";    


    /** _more_ */
    public static final String PROP_REPOSITORY_NAME =
        "ramadda.repository.name";

    public static final String PROP_REPOSITORY_SLUG =
        "ramadda.repository.slug";    

    /** _more_ */
    public static final String PROP_REPOSITORY_DESCRIPTION =
        "ramadda.repository.description";

    /** _more_ */
    public static final String PROP_REQUEST_PATTERN =
        "ramadda.request.pattern";

    /** _more_ */
    public static final String PROP_SHOW_APPLET = "ramadda.html.showapplet";

    /** _more_ */
    public static final String PROP_SSL_IGNORE = "ramadda.ssl.ignore";

    /** _more_ */
    public static final String PROP_SSL_PORT = "ramadda.ssl.port";

    /** _more_ */
    public static final String PROP_SSL_PASSWORD = "ramadda.ssl.password";

    /** _more_ */
    public static final String PROP_SSL_KEYPASSWORD =
        "ramadda.ssl.keypassword";

    /** _more_ */
    public static final String PROP_SSL_KEYSTORE = "ramadda.ssl.keystore";

    /** _more_ */
    public static final String PROP_SSL_CERTALIAS = "ramadda.ssl.certalias";




    /** _more_ */
    public static final String PROP_TYPES = "ramadda.types";

    /** _more_ */
    public static final String PROP_UPLOAD_MAXSIZEGB =
        "ramadda.upload.maxsizegb";

    /** _more_ */
    public static final String PROP_CACHE_TTL = "ramadda.cache.ttl";


    /** _more_ */
    public static final String PROP_CACHE_MAXSIZEGB =
        "ramadda.cache.maxsizegb";

    /** _more_ */
    public static final String PROP_ZIPOUTPUT_REGISTERED_MAXSIZEMB =
        "ramadda.zip.registered.maxsizemb";

    /** _more_ */
    public static final String PROP_ZIPOUTPUT_ANONYMOUS_MAXSIZEMB =
        "ramadda.zip.anonymous.maxsizemb";




    /** _more_ */
    public static final String PROP_USER_RESET_ID_SUBJECT =
        "ramadda.user.reset.id.subject";

    /** _more_ */
    public static final String PROP_USER_RESET_ID_TEMPLATE =
        "ramadda.user.reset.id.template";

    /** _more_ */
    public static final String PROP_USER_RESET_PASSWORD_SUBJECT =
        "ramadda.user.reset.password.subject";

    /** _more_ */
    public static final String PROP_USER_RESET_PASSWORD_TEMPLATE =
        "ramadda.user.reset.password.template";

    /** _more_ */
    public static final String PROP_VERSION = "ramadda.version";

    /**  */
    public static final String PROP_VERSION_MAJOR = "ramadda.version.major";

    /**  */
    public static final String PROP_VERSION_MINOR = "ramadda.version.minor";

    /**  */
    public static final String PROP_VERSION_PATCH = "ramadda.version.patch";


    /** _more_ */
    public static final String TYPE_ANY = "any";

    /** _more_ */
    public static final String TYPE_ASSOCIATION = "association";


    /** _more_ */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_GROUP = "group";



    /** _more_ */
    public static final String ACTION_ADD = "action.add";


    /** _more_ */
    public static final String ACTION_CLEAR = "action.clear";

    /** _more_ */
    public static final String ACTION_COPY = "action.copy";

    /** _more_ */
    public static final String ACTION_DELETE_ASK = "action.delete.ask";

    /** _more_ */
    public static final String ACTION_DELETE_DOIT = "action.delete.doit";

    /** _more_ */
    public static final String ACTION_EDIT = "action.edit";

    /** _more_ */
    public static final String ACTION_MOVE = "action.move";

    /** _more_ */
    public static final String ACTION_REMOVE = "action.remove";

    /** _more_ */
    public static final String ACTION_SPLIT = "action.split";

    /** _more_ */
    public static final String ACTION_START = "action.start";

    /** _more_ */
    public static final String ACTION_STOP = "action.stop";


    /** _more_ */
    public static final String ACTION_PASSWORDS_CLEAR =
        "action.passwords.clear";

    /** _more_ */
    public static final String ARG_PASSWORDS_CLEAR_CONFIRM =
        "action.passwords.clear.confirm";

    public static  final boolean ARG_INLINE_DFLT = false;
    public static  final boolean ARG_INLINE_TRUE = true;
    public static  final boolean ARG_INLINE_FALSE = true;    
    public static  final boolean ARG_FULL_DFLT = false;
    public static  final boolean ARG_FULL_TRUE = true;
    public static  final boolean ARG_FULL_FALSEE = false;    
    public static  final boolean ARG_ADDPATH_DFLT = false;
    public static  final boolean ARG_ADDPATH_TRUE = true;
    public static  final boolean ARG_ADDPATH_FALSE = false;    





    /** _more_ */
    public static final String WHAT_ENTRIES = "entries";

    /** _more_ */
    public static final String WHAT_TYPE = "type";

    /** _more_ */
    public static final String WHAT_TAG = "tag";

    /** _more_ */
    public static final String WHAT_METADATA = "metadata";

    /** _more_ */
    public static final String WHAT_ASSOCIATION = "association";

    /** _more_ */
    public static final String WHAT_USER = "user";




    /** _more_ */
    public static final int DB_MAX_ROWS = 1000;

    /** _more_ */
    public static final int DB_VIEW_ROWS = 100;

    /** _more_ */
    public static final int VIEW_MAX_ROWS = DB_VIEW_ROWS;



    /** _more_ */
    public static final String NODETYPE_ENTRY = "entry";

    /** _more_ */
    public static final String NODETYPE_GROUP = "group";


    /** _more_ */
    public static final String RESPONSE_XML = "xml";

    /** _more_ */
    public static final String RESPONSE_TEXT = "text";

    /** _more_ */
    public static final String RESPONSE_JSON = "json";

    /** _more_ */
    public static final String CODE_OK = "ok";

    /** _more_ */
    public static final String CODE_ERROR = "error";

    /** _more_ */
    public static final String NEWLINE = "\n";

    /** _more_ */
    public static final String BR = "<br>";

    /** _more_ */
    public static final String HR = "<hr>";

    /** _more_ */
    public static final String BLANK = "";


    /** _more_ */
    public static final String MIME_XML = "text/xml";

    /** _more_ */
    public static final String MIME_TEXT = "text/plain";


    /** _more_ */
    public static final boolean DFLT_INHERITED = false;

    public static final String DEFAULT_SEARCH_SIZE = "100";


    /** _more_ */
    public static final String ID_PREFIX_SYNTH = "synth:";


    //j++


    /** _more_ */
    public static final String MSG_ACCESS_CHANGED = "Access Changed";

    /** _more_ */
    public static final String MSG_ASSOCIATION_ADDED =
        "The association has been added";


    /** _more_ */
    public static final String SERVICE_OPENDAP = "opendap";

    /** _more_ */
    public static final String SERVICE_FILE = "file";


    /** _more_ */
    public static long MEGA = 1000000;

    /** _more_ */
    public static long GIGA = MEGA * 1000;


    /** _more_ */
    public static final String PROP_PROXY_USER = "ramadda.proxy.user";

    /** _more_ */
    public static final String PROP_PROXY_PASSWORD = "ramadda.proxy.password";


    /** _more_ */
    public static final String PROP_PROXY_HOST = "ramadda.proxy.host";

    /** _more_ */
    public static final String PROP_PROXY_PORT = "ramadda.proxy.port";

    /** _more_ */
    public static final String PROP_ENCRYPT_PASSWORD =
        "ramadda.encrypt.password";

    /** _more_ */
    public static final String PROP_ENCRYPT_CIPHER = "ramadda.encrypt.cipher";

    public static final String PROP_AWS_KEY = "ramadda.aws.key";


    /** _more_ */
    public static final String BREADCRUMB_SEPARATOR = "&raquo;";

    /** _more_ */
    public static final String BREADCRUMB_SEPARATOR_PAD =
        "&nbsp;&raquo;&nbsp;";



    /** _more_ */
    public static final String CSS_CLASS_ENTRY_TREE_ROW = "entry-tree-row";


    /** _more_ */
    public static final String CSS_CLASS_ENTRY_LIST_ROW = "entry-list-row";

    /** _more_ */
    public static final String CSS_CLASS_ENTRY_ROW_LABEL = "entry-row-label";

    /** _more_ */
    public static final String CSS_CLASS_FOLDER_BLOCK =
        "ramadda-folder-block";

    /** _more_ */
    public static final String CSS_CLASS_SERVER = "ramadda-server";

    /** _more_ */
    public static final String CSS_CLASS_SERVER_BLOCK =
        "ramadda-server-block";

    /** _more_ */
    public static final String CSS_CLASS_STACK = "ramadda-stack";

    /** _more_ */
    public static final String CSS_CLASS_SMALLLINK = "ramadda-smalllink";

    /** _more_ */
    public static final String CSS_CLASS_SMALLHELP = "ramadda-smallhelp";

    /** _more_ */
    public static final String CSS_CLASS_MENUBAR = "ramadda-menubar";

    /** _more_ */
    public static final String CSS_CLASS_MENUBUTTON = "ramadda-menubutton";

    /** _more_ */
    public static final String CSS_CLASS_MENUBUTTON_SEPARATOR =
        "ramadda-menubutton-separator";

    /** _more_ */
    public static final String CSS_CLASS_DATETIME = "ramadda-datetime";


    /** _more_ */
    public static final String CSS_CLASS_MENUITEM_LINK =
        "ramadda-menuitem-link";

    /** _more_ */
    public static final String CSS_CLASS_MENUITEM_SEPARATOR =
        "ramadda-menuitem-separator";

    /** _more_ */
    public static final String CSS_CLASS_MENUITEM = "ramadda-menuitem";

    /** _more_ */
    public static final String CSS_CLASS_MENU_GROUP = "ramadda-menugroup";

    /** _more_ */
    public static final String CSS_CLASS_HIGHLIGHT = "ramadda-highlight";

    /** _more_ */
    public static final String CSS_CLASS_ERROR_LABEL = "ramadda-error";

    /** _more_ */
    public static final String CSS_CLASS_HEADING_1 = "ramadda-heading-1";

    /** _more_ */
    public static final String CSS_CLASS_HEADING_2 = "ramadda-heading-2";

    /** _more_ */
    public static final String CSS_CLASS_HEADING_2_LINK =
        "ramadda-heading-2-link";

    /** _more_ */
    public static final String CSS_CLASS_POPUP = "ramadda-popup";


    /** _more_ */
    public static final String CSS_CLASS_REQUIRED = "ramadda-required";

    /** _more_ */
    public static final String CSS_CLASS_REQUIRED_LABEL =
        "ramadda-required-label";

    /** _more_ */
    public static final String CSS_CLASS_REQUIRED_FIELD =
        "ramadda-required-field";

    /** _more_ */
    public static final String CSS_CLASS_REQUIRED_DISABLED =
        "ramadda-required-disabled";

    /** _more_ */
    public static final String CSS_CLASS_EARTH_NAV = "ramadda-earth-nav";

    /** _more_ */
    public static final String CSS_CLASS_EARTH_LINK = "ramadda-earth-link";

    /** _more_ */
    public static final String CSS_CLASS_EARTH_ENTRIES =
        "ramadda-earth-entries";

    /** _more_ */
    public static final String CSS_CLASS_EARTH_CONTAINER =
        "ramadda-earth-container";

    /** _more_ */
    public static final String CSS_CLASS_COMMENT_BLOCK =
        "ramadda-comment-block";

    /** _more_ */
    public static final String CSS_CLASS_COMMENT_COMMENTER =
        "ramadda-comment-commenter";

    /** _more_ */
    public static final String CSS_CLASS_COMMENT_DATE =
        "ramadda-comment-date";

    /** _more_ */
    public static final String CSS_CLASS_COMMENT_INNER =
        "ramadda-comment-inner";

    /** _more_ */
    public static final String CSS_CLASS_COMMENT_SUBJECT =
        "ramadda-comment-subject";

    /** _more_ */
    public static final String CSS_CLASS_USER_FIELD = "ramadda-user-field";


    /** _more_ */
    public static final String CSS_CLASS_SEPARATOR = "ramadda-separator";



    /** text position attribute */
    public static final String ATTR_TEXTPOSITION = "textposition";

    /** position left id */
    public static final String POS_LEFT = "left";

    /** position bottom id */
    public static final String POS_BOTTOM = "bottom";

    /** position right id */
    public static final String POS_RIGHT = "right";

    /** position top id */
    public static final String POS_TOP = "top";

    /** position none (hide) id */
    public static final String POS_NONE = "none";


    /** attribute in import tag */
    public static final String ATTR_WIDTH = "width";

    /** attribute in import tag */
    public static final String ATTR_HEIGHT = "height";

    /** imagewidth attribute */
    public static final String ATTR_IMAGEWIDTH = "imagewidth";

    /** imageheight attribute */
    public static final String ATTR_IMAGEHEIGHT = "imageheight";

    /** the alt attribute for images */
    public static final String ATTR_ALT = "alt";

    /** _more_ */
    public static final String LABEL_NEW_ENTRY = "Pick a Type...";

    /** _more_ */
    public static final String LABEL_ENTRY = "Entry";

    /** _more_ */
    public static final String LABEL_ENTRIES = "Entries";

    /** _more_ */
    public static final String LABEL_EMPTY_FOLDER = "This folder is empty";

    /** _more_ */
    public static final String LABEL_NO_ENTRIES_FOUND = "No entries found";

    /** _more_ */
    public static final String ARG_IMPORT_TYPE = "import.type";

    /** _more_ */
    public static final String FILTER_IMAGE = "image";
    public static final String FILTER_IMAGE_OR_ATTACHMENT = "image_or_attachment";    

    /** _more_ */
    public static final String FILTER_FILE = "file";

    /** _more_ */
    public static final String FILTER_GEO = "geo";

    /** _more_ */
    public static final String FILTER_FOLDER = "folder";

    /** _more_ */
    public static final String FILTER_TYPE = "type:";

    /** _more_ */
    public static final String FILTER_SUFFIX = "suffix:";

    /** _more_ */
    public static final String FILTER_NAME = "name:";

    /** _more_ */
    public static final String FILTER_ID = "id:";



    /** _more_ */
    public static final String ORDERBY_DATE = "date";




    /** _more_ */
    public static final String ORDERBY_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ORDERBY_TODATE = "todate";

    /** _more_ */
    public static final String ORDERBY_CHANGEDATE = "changedate";

    /** _more_ */
    public static final String ORDERBY_SIZE = "size";

    /** _more_ */
    public static final String ORDERBY_TYPE = "type";

    /**  */
    public static final String ORDERBY_RELEVANT = "relevant";

    /** _more_ */
    public static final String ORDERBY_ENTRYORDER = "entryorder";

    /** _more_ */
    public static final String ORDERBY_CREATEDATE = "createdate";

    /** _more_ */
    public static final String ORDERBY_NAME = "name";

    /**  */
    public static final String ORDERBY_NUMBER = "number";


    /** _more_ */
    public static final String ORDERBY_MIXED = "mixed";


    /** _more_ */
    public static final String MACRO_ROOT = "root";

    /** _more_ */
    public static final RequestArgument REQUESTARG_NORTH =
        new RequestArgument("ramadda.arg.area.north");

    /** _more_ */
    public static final RequestArgument REQUESTARG_WEST =
        new RequestArgument("ramadda.arg.area.west");

    /** _more_ */
    public static final RequestArgument REQUESTARG_SOUTH =
        new RequestArgument("ramadda.arg.area.south");

    /** _more_ */
    public static final RequestArgument REQUESTARG_EAST =
        new RequestArgument("ramadda.arg.area.east");

    /** _more_ */
    public static final RequestArgument REQUESTARG_LATITUDE =
        new RequestArgument("ramadda.arg.latitude");

    /** _more_ */
    public static final RequestArgument REQUESTARG_LONGITUDE =
        new RequestArgument("ramadda.arg.longitude");


    public static final String LABEL_CANCEL = "Cancel";
    public static final String LABEL_OK = "OK";    
    public static final String LABEL_YES = "Yes";
    public static final String LABEL_NO = "No";    
    public static final String LABEL_SUBMIT = "Submit";
    public static final String LABEL_SEARCH="Search";
    public static final String LABEL_LOGIN = "Login";
}

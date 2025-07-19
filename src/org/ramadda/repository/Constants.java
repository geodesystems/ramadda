/**
Copyright (c) 2008-2025 Geode Systems LLC
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

    public static final String MESSAGE_ACCESS="You do not have access to this entry";

    public static final String RESOURCE_ALLCSS = "allcss.css";

    public static final String RESOURCE_ALLJS = "alljs.js";

    public static final String WIKI_PREFIX = "<wiki>";

    public static final String ICON_ACCESS = "fas fa-lock";

    public static final String ICON_ROTATE =
        "/icons/arrow_rotate_clockwise.png";

    public static final String ICON_ANTIROTATE =
        "/icons/arrow_rotate_anticlockwise.png";

    public static final String ICON_ARROW = "fas fa-arrow-right";

    public static final String ICON_GOOGLEEARTH = "/icons/googleearth.gif";

    public static final String ICON_ASSOCIATION = "fas fa-arrow-right";

    public static final String ICON_BLANK = "/icons/blank.gif";

    public static final String ICON_CALENDAR = "fas fa-calendar-alt";

    public static final String ICON_CART = "/icons/cart.png";

    public static final String ICON_CART_ADD = "fas fa-cart-plus";

    public static final String ICON_CART_DELETE = "/icons/cart_delete.png";

    public static final String ICON_CHAT = "fas fa-comments";

    public static final String ICON_TIMELINE = "/icons/timeline_marker.png";

    public static final String ICON_CLOSE = "fas fa-window-close";

    public static final String ICON_CLOUD = "fas fa-cloud";

    public static final String ICON_COMMENTS = "fas fa-comments";

    public static final String ICON_FTP = "/icons/server_go.png";

    public static final String ICON_CSV = "fas fa-file-csv";

    public static final String ICON_DATA = "fas fa-table";

    public static final String ICON_DIF = "fas fa-info-circle";

    public static final String ICON_DATEGRID = "/icons/dategrid.gif";

    public static final String ICON_DELETE = "fas fa-cut";

    public static final String ICON_DOWNARROW = "/icons/downarrow.gif";

    public static final String ICON_DOWNLOAD = "fas fa-download";

    public static final String ICON_DOWNDART = "/icons/downdart.gif";

    public static final String ICON_EDIT = "fas fa-edit";

    public static final String ICON_ENTRY = "fas fa-info-circle";

    public static final String ICON_ENTRY_ADD = "fas fa-file-medical";

    public static final String ICON_PUBLISH = "/icons/flag_green.png";

    public static final String ICON_PLANVIEW = "/icons/planviewcontour.png";

    public static final String ICON_ENTRY_UPLOAD = "/icons/flaggedentry.png";

    public static final String ICON_FAVORITE = "fas fa-star";

    public static final String ICON_FETCH = "fas fa-download";

    public static final String ICON_FILE = "/icons/page.png";

    public static final String ICON_FILELISTING = "fas fa-list-alt";

    public static final String ICON_FOLDER = "/icons/folder.png";

    public static final String ICON_FOLDER_ADD = "fas fa-folder-plus";

    public static final String ICON_FOLDER_CLOSED = "/icons/folderclosed.png";

    public static final String ICON_FOLDER_CLOSED_LOCKED =
        "/icons/folder_key.png";

    public static final String ICON_FOLDER_OPEN = "/icons/folderopen.png";

    public static final String ICON_GRAPH = "/icons/vector.png";

    public static final String ICON_TABLE = "fas fa-table";

    public static final String ICON_GRAYRECT = "/icons/grayrect.gif";

    public static final String ICON_GRAYRECTARROW = "";

    public static final String ICON_HOME = "fas fa-home";

    public static final String ICON_HEADER = "/images/header.jpg";

    public static final String ICON_HELP = "/icons/help.png";

    public static final String ICON_IMAGE = "fas fa-image";

    public static final String ICON_MOVIE = "fas fa-film";

    public static final String ICON_IMPORT = "fas fa-file-import";

    public static final String ICON_EXPORT = "fas fa-file-export";

    public static final String ICON_IMAGES = "fas fa-images";

    public static final String ICON_INFORMATION = ICON_ENTRY;

    public static final String ICON_TREE = ICON_ENTRY;

    public static final String ICON_KML = "fas fa-globe";

    public static final String ICON_LCURVE = "/icons/blc.gif";

    public static final String ICON_SYNTH_FILE = "/icons/server_database.png";

    public static final String ICON_LEFT = "/icons/resultset_previous.png";

    public static final String ICON_LINK = "/icons/link.png";

    public static final String ICON_USERLINKS = "/icons/cog.png";

    public static final String ICON_LIST = "/icons/list.gif";

    public static final String ICON_LOG = "fas fa-scroll";

    public static final String ICON_MAP = "fas fa-map";

    public static final String ICON_MAP_NAV = "/icons/bullet_go.png";

    public static final String ICON_METADATA = "fas fa-info";

    public static final String ICON_METADATA_ADD = "fas fa-database";

    public static final String ICON_METADATA_EDIT = "fas fa-database";

    public static final String ICON_MOVE = "fas fa-copy";

    public static final String ICON_NEW = "fas fa-plus";

    //    public static final String ICON_PLUS = "fas fa-plus-square";
    public static final String ICON_PLUS = "fa-regular fa-square-plus" ;    

    public static final String ICON_MINUS = "fa-regular fa-square-minus";

    public static final String ICON_PROGRESS = "/icons/progress.gif";

    public static final String ICON_QUESTION = "/icons/question.png";

    public static final String ICON_RANGE = "fas fa-arrow-right";

    public static final String ICON_RCURVE = "/icons/brc.gif";

    public static final String ICON_RIGHT = "/icons/resultset_next.png";

    public static final String ICON_RIGHTARROW = "fas fa-arrow-right";

    public static final String ICON_ATOM = "fas fa-rss";

    public static final String ICON_RSS = "fas fa-rss";

    public static final String ICON_SEARCH = "fas fa-binoculars";

    public static final String ICON_SEARCH_SMALL = "fas fa-search";

    public static final String ICON_TEXT = "fas fa-align-left";

    public static final String ICON_TOGGLEARROWDOWN = "fas fa-caret-down";

    public static final String ICON_TOGGLEARROWRIGHT = "fas fa-caret-right";

    public static final String ICON_TOOLS = "fas fa-tools";

    public static final String ICON_UPARROW = "/icons/uparrow.gif";

    public static final String ICON_UPDART = "/icons/updart.gif";

    public static final String ICON_UPLOAD = "/icons/add.png";

    /**  */
    public static final String ICON_DIALOG_QUESTION = "fas fa-question";

    /**  */
    public static final String ICON_DIALOG_INFO = "fas fa-info-circle";

    public static final String ICON_DIALOG_ERROR = "fas fa-exclamation-triangle";

    public static final String ICON_DIALOG_WARNING = "fas fa-triangle-exclamation";

    public static final String ICON_WIDTH = "18px";

    public static final String ICON_WIKI = "/icons/wiki.png";

    public static final String ICON_XML = "/icons/xml.png";

    public static final String ICON_JSON = "/icons/json.png";

    public static final String ICON_GEOJSON = "/icons/geojson.png";

    public static final String ICON_ZIP = "fas fa-file-archive";

    public static final String ICON_ZIPTREE = "fas fa-file-archive";

    //j++

    public static final String ARG_ICON = "ramadda.icon";

    public static final String ARG_AGREE = "agree";

    public static final String ATTR_ADDMETADATA = "addmetadata";

    public static final String ATTR_FILESIZE = "filesize";

    public static final String ARG_SERVICEID = "serviceid";

    public static final String ARG_EXPORT_SHALLOW = "exportshallow";
    public static final String ARG_EXPORT_DEEP = "exportdeep";    

    public static final String PROP_INSTALL_PASSWORD =
        "ramadda.install.password";

    public static final String PROP_MAKESNAPSHOT = "makesnapshot";

    public static final String PROP_OVERRIDE_URL = "overrideurl";

    public static final String ARG_SHOWCATEGORIES = "showCategories";

    public static final String ARG_SHOWNEXT = "showNext";

    public static final String ARG_DOOCR = "doocr";
    public static final String ARG_DOOCR_CONDITIONAL = "doocr_conditional";    
    public static final String ARG_CORPUS_FORCE = "corpus_force";

    public static final String ARG_RETURNFILENAME = "returnfilename";

    public static final String ATTR_ADDSHORTMETADATA = "addshortmetadata";

    public static final String ATTR_MAKETHUMBNAILS = "makethumbnails";

    public static final String ATTR_ATTR = "attr";

    public static final String ATTR_ATTR1 = ATTR_ATTR + "1";

    public static final String ATTR_ATTR2 = ATTR_ATTR + "2";

    public static final String ATTR_ATTR3 = ATTR_ATTR + "3";

    public static final String ATTR_ATTR4 = ATTR_ATTR + "4";

    public static final String ATTR_CANDONEW = "candonew";

    public static final String ATTR_CANDOUPLOAD = "candoupload";

    public static final String ATTR_CLASS = "class";

    public static final String ATTR_CODE = "code";

    public static final String ATTR_DATATYPE = "datatype";

    public static final String ATTR_DIRECTORY = "directory";

    public static final String ATTR_FILE_PATTERN = "filePattern";

    /**  */
    public static final String ATTR_UNIQUE = "unique";

    public static final String ATTR_DB_DESCRIPTION = "description";

    public static final String ATTR_FORUSER = "foruser";

    public static final String ATTR_DB_NAME = "name";

    public static final String ATTR_DESCRIPTION = "description";

    public static final String ATTR_CATEGORY = "category";

    public static final String ATTR_ENTRYORDER = "entryorder";

    public static final String ATTR_EAST = "east";

    public static final String ATTR_FILE = "file";

    public static final String ATTR_FROM = "from";

    public static final String ATTR_FROMDATE = "fromdate";

    public static final String ATTR_GROUP = "group";

    public static final String ATTR_ID = "id";

    public static final String ATTR_ORIGINALID = "originalid";

    public static final String ATTR_INHERITED = "inherited";

    public static final String ATTR_INPUTID = "inputid";

    public static final String ATTR_LOCALFILE = "localfile";

    public static final String ATTR_LOCALFILETOMOVE = "localfiletomove";

    public static final String ATTR_NAME = "name";

    public static final String ATTR_NORTH = "north";

    public static final String ATTR_LATITUDE = "latitude";

    public static final String ATTR_LONGITUDE = "longitude";

    public static final String ATTR_PARENT = "parent";

    public static final String ATTR_SUPER = "super";

    public static final String ATTR_RESOURCE = "resource";

    public static final String ATTR_FILENAME = "filename";

    public static final String ATTR_RESOURCE_TYPE = "resource_type";

    public static final String ATTR_SERVER = "server";

    public static final String ATTR_SOUTH = "south";

    public static final String ATTR_TARGET = "target";

    public static final String ATTR_TITLE = "title";

    public static final String ATTR_TO = "to";

    public static final String ATTR_TODATE = "todate";

    public static final String ATTR_PATH = "path";

    public static final String ATTR_CREATEDATE = "createdate";

    public static final String ATTR_CHANGEDATE = "changedate";

    public static final String ATTR_TOOLTIP = "tooltip";

    public static final String ATTR_TYPE = "type";

    public static final String ATTR_ISGROUP = "isgroup";

    public static final String ATTR_URL = "url";

    public static final String ATTR_SIZE = "size";

    public static final String ATTR_DOWNLOAD = "download";

    public static final String ATTR_WEST = "west";

    public static final String ATTR_ALTITUDE = "altitude";

    public static final String ATTR_ALTITUDE_TOP = "altitudetop";

    public static final String ATTR_ALTITUDE_BOTTOM = "altitudebottom";

    public static final String ARG_DISPLAY = "display";

    public static final String DISPLAY_FULL = "full";

    public static final String DISPLAY_SMALL = "small";

    public static final String ARG_DECORATE = "decorate";

    public static final String ARG_TREEVIEW = "treeview";

    public static final String ARG_DEPTH = "depth";

    public static final String ARG_FULLURL = "fullurl";

    public static final String ARG_ACTION = "action";

    public static final String ARG_ACTION_FORCE = "action.force";

    public static final String ARG_ACTION_ASSOCIATE = "action.associate";

    public static final String ARG_ACTION_COPY = "action.copy";

    public static final String ARG_ACTION_ID = "actionid";

    public static final String ARG_ACTION_MOVE = "action.move";

    public static final String ARG_ADD = "add";

    public static final String ARG_ADMIN = "admin";

    public static final String ARG_ADMIN_WHAT = "what";

    public static final String ARG_PLUGIN_FILE = "plugin.file";

    public static final String ARG_ALLENTRIES = "allentries";

    public static final String ARG_ENTRYTYPE = "entrytype";

    public static final String ARG_ANCESTOR = "ancestor";
    public static final String ARG_DESCENDENT = "descendent";    

    public static final String ARG_APPLET = "applet";

    public static final String ARG_AREA = "area";

    public static final String ARG_LOCATION = "location";

    public static final String ARG_LOCATION_LATITUDE = "location.latitude";

    public static final String ARG_LOCATION_LONGITUDE = "location.longitude";

    public static final String ARG_LATITUDE = "latitude";

    public static final String ARG_LONGITUDE = "longitude";

    public static final String ARG_BBOX = "bbox";

    public static final String ARG_DEFAULTBBOX = "defaultbbox";

    public static final String ARG_AREA_MODE = "areamode";

    public static final String VALUE_BLANK = "-blank-";

    public static final String VALUE_AREA_CONTAINS = "contains";

    public static final String VALUE_AREA_OVERLAPS = "overlaps";

    public static final String ARG_AREA_EAST = ARG_AREA + "_east";

    public static final String ARG_AREA_NORTH = ARG_AREA + "_north";

    public static final String ARG_AREA_SOUTH = ARG_AREA + "_south";

    public static final String ARG_AREA_WEST = ARG_AREA + "_west";

    public static final String ARG_ASCENDING = "ascending";
    public static final String ARG_DESCENDING = "descending";    

    public static final String DIR_UP = "up";
    public static final String DIR_DOWN = "down";

    public static final String ARG_ASSOCIATION = "association";

    public static final String ARG_AUTH_PASSWORD = "auth.password";

    public static final String ARG_AUTH_USER = "auth.user";

    public static final String ARG_BYTES = "bytes";

    public static final String ARG_CANCEL = "cancel";

    public static final String ARG_UNDO = "undo";

    public static final String ARG_EXECUTE = "execute";

    public static final String ARG_CONFIRM = "confirm";

    public static final String ARG_COLUMNS = "columns";

    public static final String ARG_COMPRESS = "compress";

    public static final String ARG_CANCEL_DELETE = "canceldelete";

    public static final String ARG_DO_CHILDREN = "dochildren";

    public static final String ARG_CHANGE = "change";

    public static final String ARG_COLLECTION = "collection";

    public static final String ARG_COMMENT = "comment";

    public static final String ARG_COMMENTS = "showcomments";

    public static final String ARG_COMMENT_ID = "comment_id";

    public static final String ARG_CONTRIBUTION_FROMEMAIL =
        "contribution.fromemail";

    public static final String ARG_CONTRIBUTION_FROMNAME =
        "contribution.fromname";

    public static final String ARG_ENTRYORDER = "entryorder";

    public static final String ARG_CREATEDATE = "createdate";

    public static final String ARG_CHANGEDATE = "changedate";

    public static final String ARG_CREATOR = "creator";

    public static final String ARG_DATASET = "dataset";

    public static final String ARG_CATEGORY = "category";

    public static final String ARG_CATEGORY_SELECT = "category.select";

    public static final String ARG_DATE = "date";

    /**  */
    public static final String ARG_TAGS = "tags";

    public static final String ARG_DATE_OVERLAPS = "date.overlaps";

    public static final String ARG_DATE_SEARCHMODE = "date.searchmode";

    public static final String ARG_DATE_NODATAMODE = "date.nodatamode";

    public static final String VALUE_NODATAMODE_NONE = "none";

    public static final String VALUE_NODATAMODE_INCLUDE = "include";

    public static final String DATE_SEARCHMODE_OVERLAPS = "overlaps";

    public static final String DATE_SEARCHMODE_CONTAINEDBY = "containedby";

    public static final String DATE_SEARCHMODE_CONTAINS = "contains";

    public static final String DATE_SEARCHMODE_DEFAULT =
        DATE_SEARCHMODE_OVERLAPS;

    public static final String ARG_DATE_PATTERN = "date.pattern";

    public static final String ARG_DAY = "day";

    public static final String ARG_DELETE = "delete";

    public static final String ARG_DELETE_CONFIRM = "delete.confirm";

    public static final String ARG_DESCRIPTION = "description";

    public static final String ARG_ISWIKI = "iswiki";

    public static final String ARG_WIKITEXT = "wikitext";

    public static final String ARG_EAST = "east";

    public static final String ARG_ELEMENT = "element";

    public static final String ARG_EDIT_METADATA = "edit.metadata";

    public static final String ARG_EMBEDDED = "embedded";

    public static final String ARG_ENTRYID = "entryid";

    public static final String ARG_SELENTRY = "selentry";

    public static final String ARG_ALLENTRY = "allentry";

    public static final String ARG_ENTRY_TIMESTAMP = "entry.timestamp";

    public static final String ARG_LOCALEID = "localeid";

    public static final String ARG_ENTRYIDS = "entryids";

    public static final String ARG_EXACT = "exact";

    public static final String ARG_ISREGEXP = "isregexp";

    public static final String ARG_FAVORITE_ADD = "user.favorite.add";

    public static final String ARG_FAVORITE_DELETE = "user.favorite.delete";

    public static final String ARG_FAVORITE_ID = "user.favorite.id";

    public static final String ARG_FILE = "file";

    public static final String ARG_FILESUFFIX = "filesuffix";

    public static final String ARG_MAXFILESIZE = "maxfilesize";

    public static final String ARG_FILE_UNZIP = "file.unzip";
    public static final String ARG_ZIP_PATTERN = "zippattern";    

    public static final String ARG_STRIPEXIF = "stripexif";
    public static final String ARG_REVERSEGEOCODE = "reversegeocode";        

    public static final String ARG_MAKENAME = "makename";

    public static final String ARG_DELETEFILE = "deletefile";

    public static final String ARG_FILE_PRESERVEDIRECTORY =
        "file.preservedirectoryfile";

    public static final String ARG_FORMAT = "format";

    public static final String ARG_FORM_ADVANCED = "form.advanced";

    public static final String ARG_FORM_METADATA = "form.metadata";

    public static final String ARG_FORM_TYPE = "form.type";

    public static final String ARG_FROM = "from";

    public static final String ARG_FROMDATE = "fromdate";

    public static final String ARG_FROMDATE_TIME = ARG_FROMDATE + ".time";

    public static final String ARG_DATA_DATE = "datadate";

    public static final String ARG_CREATE_DATE = "createdate";

    public static final String ARG_CHANGE_DATE = "changedate";

    public static final String ARG_FROMLOGIN = "user.fromlogin";

    public static final String ARG_GROUP = "group";

    public static final String ARG_DEST_ENTRY = "destentry";

    public static final String ARG_GROUPID = "groupid";

    public static final String ARG_GROUP_CHILDREN = "group_children";

    public static final String ARG_PATHTEMPLATE = "pathtemplate";

    public static final String ARG_HARVESTER_CLASS = "harvester.class";

    public static final String ARG_HARVESTER_GETXML = "harvester.getxml";

    public static final String ARG_HARVESTER_ID = "harvester.id";

    public static final String ARG_HARVESTER_REDIRECTTOEDIT =
        "harvester.redirecttoedit";

    public static final String ARG_HARVESTER_XMLFILE = "harvester.xmlfile";

    public static final String ARG_HEIGHT = "height";

    public static final String ARG_IMAGEHEIGHT = "imageheight";

    public static final String ARG_IMAGEWIDTH = "imagewidth";
    public static final String ARG_SERVERIMAGEWIDTH = "serverimagewidth";    

    public static final String ARG_INCLUDENONGEO = "includenongeo";

    public static final String ARG_LABEL = "label";

    public static final String ARG_LANGUAGE = "language";

    public static final String ARG_LATEST = "latest";

    public static final String ARG_LATESTOPENDAP = "latestopendap";

    public static final String ARG_LAYOUT = "layout";

    public static final String ARG_LIMIT = "limit";

    public static final String ARG_SERVERFILE = "serverfile";

    public static final String ARG_SERVERFILE_HARVEST = "serverfile_harvest";

    public static final String ARG_SERVERFILE_PATTERN = "serverfile_pattern";

    public static final String ARG_LOG = "log";

    public static final String ARG_MAX = "max";

    public static final String ARG_MARKER = "marker";
    public static final String ARG_PREVMARKERS = "prevmarkers";

    public static final String ARG_LAST = "last";

    public static final String ARG_MAXLAT = "maxlat";

    public static final String ARG_MAXLON = "maxlon";

    //These can be used as well for search and subset. They are defined in the repository.properties 

    public static final String ARG_MAXLATITUDE = "maxlatitude";

    public static final String ARG_MINLATITUDE = "minlatitude";

    public static final String ARG_MAXLONGITUDE = "maxlongitude";

    public static final String ARG_MINLONGITUDE = "minlongitude";

    public static final String ARG_MESSAGE = "message";

    public static final String ARG_METADATA_ADD = "metadata.add";

    public static final String ARG_FROMHARVESTER = "fromharvester";

    public static final String ARG_METADATA_ADDSHORT = "metadata.addshort";

    public static final String ARG_METADATA_ADDTOPARENT =
        "metadata.addtoparent";

    public static final String ARG_METADATA_CLIPBOARD_COPY =
        "metadata.clipboard.copy";

    public static final String ARG_METADATA_CLIPBOARD_PASTE =
        "metadata.clipboard.paste";

    public static final String ARG_METADATA_ATTR = "metadata_attr";

    public static final String ARG_METADATA_ATTR1 = "metadata_attr1";

    public static final String ARG_METADATA_ATTR2 = "metadata_attr2";

    public static final String ARG_METADATA_ATTR3 = "metadata_attr3";

    public static final String ARG_METADATA_ATTR4 = "metadata_attr4";

    public static final String ARG_METADATA_DELETE = "metadata_delete";

    public static final String ARG_METADATA_ID = "metadata_id";

    public static final String ARG_METADATA_INHERITED = "metadata_inherited";

    public static final String ARG_METADATA_TYPE = "metadata_type";

    public static final String ARG_MINLAT = "minlat";

    public static final String ARG_MINLON = "minlon";

    public static final String PROP_SYSTEM_MESSAGE = "system.message";

    public static final String PROP_ENTRY_TABLE_SHOW_CREATEDATE =
        "ramadda.entry.table.show.createdate";

    public static final String PROP_CREATED_DISPLAY_MODE =
        "ramadda.created.display";

    public static final String PROP_PASSPHRASE = "ramadda.passphrase";

    public static final String PROP_MONITOR_ENABLE_EXEC =
        "ramadda.monitor.enable.exec";

    public static final String PROP_PROXY_WHITELIST =
        "ramadda.proxy.whitelist";

    public static final String PROP_READ_ONLY = "ramadda.readonly";

    public static final String PROP_DOCACHE = "ramadda.docache";

    public static final String PROP_ENABLE_FILE_LISTING =
        "ramadda.enable_file_listing";

    public static final String PROP_ENABLE_HOSTNAME_MAPPING =
        "ramadda.enable_hostname_mapping";

    public static final String PROP_SHOW_HELP = "ramadda.html.show.help";

    public static final String PROP_SHOW_CART = "ramadda.html.show.cart";

    public static final String ARG_MONTH = "month";

    public static final String ARG_MOVE_CONFIRM = "move.confirm";

    public static final String ARG_NAME = "name";

    public static final String ARG_BULKUPLOAD = "bulkupload";

    public static final String ARG_NOREDIRECT = "noredirect";

    public static final String ARG_NEW = "new";

    public static final String ARG_NEXT = "next";

    public static final String ARG_NODETYPE = "nodetype";

    public static final String ARG_DETAILS = "details";

    public static final String ARG_USER_MESSAGE = "usermessage";

    public static final String ARG_CAPTCHA_INDEX = "captchindex";
    public static final String ARG_CAPTCHA_RESPONSE = "captchresponse";    

    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_EDITUSER_ID = "edituser_id";    

    public static final String ARG_USER_SENDMAIL = "usersendmail";

    public static final String ARG_USER_HOME = "userhome";

    public static final String ARG_ALTITUDE = "altitude";

    public static final String ARG_ALTITUDE_TOP = "altitude.top";

    public static final String ARG_ALTITUDE_BOTTOM = "altitude.bottom";

    public static final String ARG_SIZE_MIN = "sizemin";
    public static final String ARG_SIZE_MAX = "sizemax";    

    public static final String ARG_SETFROMCHILDREN = "setfromchildren";

    public static final String ARG_SETFROMCHILDREN_RECURSE =
        "setfromchildren.recurse";

    public static final String ARG_SEARCH_POLYGON = "search_polygon";


    public static final String ARG_SEARCH_URL = "searchurl";

    public static final String ARG_NORTH = "north";

    public static final String ARG_OK = "ok";

    public static final String ARG_ONLYGROUPS = "onlygroups";

    public static final String ARG_ORDERBY = "orderby";

    public static final String ARG_GROUPBY = "group_by";

    public static final String ARG_AGG = "group_agg";

    public static final String ARG_AGG_TYPE = "group_agg_type";

    public static final String ARG_OUTPUT = "output";

    public static final String ARG_PREVIOUS = "previous";

    public static final String ARG_VISIBLE = "visible";

    public static final String ARG_PRODUCT = "product";

    public static final String ARG_DATEFORMAT="dateformat";

    public static final String ARG_PUBLISH = "publish";

    public static final String ARG_SNAPSHOT_TYPE = "snapshottype";

    public static final String SNAPSHOT_ENTRY = "snapshotentry";

    public static final String SNAPSHOT_FILE = "snapshotfile";

    public static final String SNAPSHOT_EXPORT = "snapshotexport";

    public static final String ARG_SUBMIT_PUBLISH = "submit.publish";

    public static final String ARG_PUBLISH_ENTRY = "publish_entry";

    public static final String ARG_PUBLISH_NAME = "publish.name";

    public static final String ARG_PUBLISH_DESCRIPTION =
        "publish.description";

    public static final String ARG_JUSTPUBLISH = "justpublish";

    public static final String ARG_QUERY = "query";

    public static final String ARG_RECURSE = "recurse";

    public static final String ARG_REDIRECT = "redirect";

    public static final String ARG_RELATIVEDATE = "relativedate";

    public static final String ARG_REQUIRED = "required";

    public static final String ARG_RESOURCE = "resource";

    public static final String ARG_RESOURCE_DOWNLOAD = "resource.download";
    public static final String ARG_DOWNLOAD_FILE = "downloadfile";
    public static final String ARG_CLEAR_RESOURCE = "clearresource";

    public static final String ARG_RESPONSE = "response";

    public static final String ARG_RESPONSETYPE = "responsetype";

    public static final String ARG_ROLES = "roles";

    public static final String ARG_SEARCHMETADATA = "searchmetadata";

    public static final String ARG_SEARCH_TYPE = "search.type";

    public static final String ARG_SEARCH_SHOWFORM = "search.showform";

    public static final String ARG_SEARCH_SHOWHEADER = "search.showheader";

    public static final String SEARCH_TYPE_TEXT = "search.type.text";

    public static final String SEARCH_TYPE_ADVANCED = "search.type.advanced";

    public static final String ARG_SELECTTYPE = "selecttype";

    public static final String ARG_SESSIONID = "sessionid";

    public static final String ARG_ANONYMOUS = "anonymous";

    public static final String ARG_AUTHTOKEN = "authtoken";

    public static final String ARG_REMOVESESSIONID = "removesessionid";

    public static final String ARG_SHORT = "short";

    public static final String ARG_SHOWENTRYSELECTFORM =
        "showentryselectform";

    public static final String ARG_SHOWLINK = "showlink";

    public static final String ARG_DISPLAYLINK = "displaylink";

    public static final String ARG_SHOWTAB = "showtab";

    public static final String ARG_SHOWMETADATA = "showmetadata";

    public static final String ARG_SHOWYEAR = "showyear";

    public static final String ARG_SHOW_ASSOCIATIONS = "showassociations";

    public static final String ARG_SKIP = "skip";

    public static final String ARG_SOUTH = "south";

    public static final String ARG_SQLFILE = "sqlfile";

    public static final String ARG_SSLOK = "sslok";

    public static final String ARG_STATION = "station";

    public static final String ARG_STEP = "step";

    public static final String ARG_SUBJECT = "subject";

    public static final String ARG_SUBMIT = "submit";

    public static final String ARG_SAVENEXT = "savenext";

    public static final String ARG_TEMPLATE = "template";
    public static final String ARG_TESTNEW = "testnew";

    public static final String ARG_USER_TEMPLATE = "usertemplate";

    public static final String ARG_TARGET = "target";

    public static final String ARG_TEXT = "text";

    /** title argument */
    public static final String ARG_TITLE = "title";

    public static final String ARG_THUMBNAIL = "thumbnail";

    public static final String ARG_TO = "to";

    public static final String ARG_TODATE = "todate";

    public static final String ARG_TODATE_TIME = ARG_TODATE + ".time";

    public static final String ARG_TONAME = "toname";

    public static final String ARG_TOPLEVEL = "toplevel";

    public static final String ARG_TYPE = "type";

    public static final String ARG_TYPEPATTERNS = "typepatterns";

    public static final String ARG_TYPE_GUESS = "type.guess";

    public static final String ARG_TYPE_FREEFORM = "type.freeform";

    public static final String ARG_TYPE_EXCLUDE = "type.exclude";

    public static final String ARG_URL = "url";

    public static final String ARG_MD5 = "md5";

    public static final String ATTR_MD5 = "md5";

    public static final String ARG_FILESIZE = "filesize";

    public static final String ARG_USER = "user";

    public static final String ARG_PASSWORD = "password";

    public static final String ARG_USER_PASSWORD = "user_password";

    public static final String ARG_USER_PASSWORD1 = "user_password1";

    public static final String ARG_USER_PASSWORD2 = "user_password2";

    public static final String ARG_USER_PASSWORDKEY = "user_passwordkey";

    public static final String ARG_USER_QUESTION = "user_question";

    public static final String ARG_USER_ROLES = "user_roles";

    public static final String ARG_VARIABLE = "variable";

    public static final String ARG_WAIT = "wait";

    public static final String ARG_WEST = "west";

    public static final String ARG_WHAT = "what";

    public static final String ARG_WIDTH = "width";

    public static final String ARG_YEAR = "year";

    public static final String TAG_ASSOCIATION = "association";

    public static final String TAG_ASSOCIATIONS = "associations";

    public static final String TAG_DESCRIPTION = "description";

    public static final String TAG_SERVICE = "service";

    public static final String TAG_EDGE = "edge";

    public static final String TAG_ENTRIES = "entries";

    public static final String TAG_WIKITEXT = "wikitext";

    public static final String TAG_ENTRY = "entry";

    public static final String TAG_GROUP = "group";

    public static final String TAG_GROUPS = "groups";

    public static final String TAG_METADATA = "metadata";

    public static final String TAG_METADATAHANDLER = "metadatahandler";

    public static final String TAG_NODE = "node";

    public static final String TAG_OUTPUTHANDLER = "outputhandler";

    public static final String TAG_RESPONSE = "response";

    public static final String TAG_TAG = "tag";

    public static final String TAG_TAGS = "tags";

    public static final String TAG_TYPE = "type";

    public static final String TAG_TYPES = "types";

    public static final String PROP_FTP_PORT = "ramadda.ftp.port";

    public static final String PROP_FTP_PASSIVEPORTS =
        "ramadda.ftp.passiveports";

    public static final String PROP_SHOWMAP = "ramadda.showmap";

    public static final String PROP_NOSTYLE = "nostyle";

    public static final String PROP_SEARCH_LUCENE_ENABLED =
        "ramadda.search.lucene.enabled";

    public static final String PROP_SEARCH_SHOW_METADATA =
        "ramadda.search.show.metadata";

    public static final String PROP_PROPERTIES = "ramadda.properties";

    public static final String PROP_BUILD_VERSION = "ramadda.build.version";

    public static final String PROP_BUILD_DATE = "ramadda.build.date";

    public static final String PROP_JAVA_VERSION = "java.version";

    public static final String PROP_ACCESS_ADMINONLY =
        "ramadda.access.adminonly";

    public static final String PROP_ACCESS_NOBOTS = "ramadda.access.nobots";

    public static final String PROP_ACCESS_NOGOOGLEBOT = "ramadda.access.nogooglebot";    

    public static final String PROP_ACCESS_REQUIRELOGIN =
        "ramadda.access.requirelogin";

    public static final String PROP_PASSWORD_OLDMD5 =
        "ramadda.password.oldmd5";

    public static final String PROP_ACCESS_ALLSSL = "ramadda.access.allssl";

    public static final String PROP_ADMIN = "ramadda.admin";

    public static final String PROP_ADMIN_INCLUDESQL =
        "ramadda.admin.includesql";

    public static final String ARG_SHUTDOWN_CONFIRM = "shutdown.confirm";

    public static final String ARG_MAP_ICONSONLY = "iconsonly";

    public static final String ARG_MAP_EXTRA = "map.extra";

    public static final String ARG_COPY_DEEP = "copydeep";
    public static final String ARG_COPY_SIZE_LIMIT = "sizelimit";
    public static final String ARG_EXCLUDES = "excludes";    
    public static final String ARG_COPY_DO_METADATA = "dometadata";

    public static final String PROP_ADMIN_EMAIL = "ramadda.admin.email";

    public static final String PROP_ADMIN_PHRASES = "ramadda.admin.phrases";

    public static final String PROP_ADMIN_SMTP = "ramadda.admin.smtp";

    public static final String PROP_REGISTER_KEY = "ramadda.register.key";

    public static final String PROP_SMTP_USER = "ramadda.admin.smtp.user";

    public static final String PROP_SMTP_PASSWORD =
        "ramadda.admin.smtp.password";

    public static final String PROP_API = "ramadda.api";

    public static final String PROP_DATE_FORMAT = "ramadda.date.format";

    public static final String PROP_DATE_SHORTFORMAT =
        "ramadda.date.shortformat";

    public static final String PROP_DB = "ramadda.db";

    public static final String PROP_DB_CANCACHE = "ramadda.db.cancache";

    public static final String PROP_DB_POOL_MAXACTIVE =
        "ramadda.db.pool.maxactive";

    public static final String PROP_DB_POOL_MAXIDLE =
        "ramadda.db.pool.maxidle";

    public static final String PROP_DB_POOL_TIMEUNTILCLOSED =
        "ramadda.db.pool.timeuntilclosed";

    public static final String PROP_DB_DERBY_HOME = "ramadda.db.derby.home";

    public static final String PROP_DEBUG = "ramadda.debug";

    public static final String PROP_DOWNLOAD_ASFILES =
        "ramadda.download.asfiles";

    public static final String PROP_DOWNLOAD_OK = "ramadda.download.ok";

    public static final String PROP_MINIFIED = "ramadda.minified";

    public static final String PROP_CDNOK = "ramadda.cdnok";

    public static final String PROP_TIMEZONE = "ramadda.timezone";

    public static final String PROP_ENTRY_TOP = "ramadda.entry.top";

    public static final String PROP_ENTRY_HEADER = "ramadda.entryheader";

    public static final String PROP_ENTRY_NAME = "ramadda.entryname";
    public static final String PROP_ENTRY_URL = "ramadda.entryurl";
    public static final String PROP_ENTRY_MENU = "ramadda.menu";                

    public static final String PROP_ENTRY_FOOTER = "ramadda.entryfooter";

    public static final String PROP_ENTRY_BREADCRUMBS =
        "ramadda.entry.breadcrumbs";

    public static final String PROP_ENTRY_POPUP =
        "ramadda.entry.popup";    

    public static final String PROP_FACEBOOK_CONNECT_KEY =
        "ramadda.facebook.connect.key";

    public static final String PROP_GOOGLEAPIKEYS = "ramadda.googleapikeys";

    public static final String PROP_HARVESTERS = "ramadda.harvesters";

    public static final String PROP_ALWAYS_HTTPS = "ramadda.always_https";

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

    public static final String PROP_HOSTNAME = "ramadda.hostname";

    public static final String PROP_HTML_FOOTER = "ramadda.html.footer";

    public static final String PROP_HTML_MIMEPROPERTIES =
        "ramadda.html.mimeproperties";

    public static final String PROP_HTML_TEMPLATE = "ramadda.html.template";

    public static final String PROP_HTML_TEMPLATES = "ramadda.html.templates";

    public static final String PROP_HTML_TEMPLATE_DEFAULT =
        "ramadda.html.template.default";

    public static final String PROP_HTML_URLBASE = "ramadda.html.urlbase";

    public static final String PROP_LANGUAGE = "ramadda.language";

    public static final String PROP_LANGUAGE_DEFAULT =
        "ramadda.language.default";

    public static final String PROP_LOCALFILEPATHS = "ramadda.localfilepaths";

    public static final String PROP_LOG_TOSTDERR = "ramadda.log.tostderr";

    public static final String PROP_METADATA = "ramadda.metadata";

    public static final String PROP_NAVSUBLINKS = "ramadda.navsublinks";

    public static final String PROP_OUTPUTHANDLERS = "ramadda.outputhandlers";

    public static final String PROP_PORT = "ramadda.port";
    public static final String PROP_SSLPORT = "ramadda.sslport";    
    public static final String PROP_EXTERNAL_PORT = "ramadda.external.port";    
    public static final String PROP_EXTERNAL_SSLPORT = "ramadda.external.sslport";    

    public static final String PROP_USE_FIXED_HOSTNAME =
        "ramadda.usefixedhostname";

    public static final String PROP_CORS_OK = "ramadda.cors.ok";

    public static final String PROP_RATINGS_ENABLE = "ramadda.ratings.enable";

    public static final String PROP_REPOSITORY_HOME = "ramadda_home";
    public static final String PROP_REPOSITORY_HOME_UPPER = "RAMADDA_HOME";    

    public static final String PROP_REPOSITORY_NAME =
        "ramadda.repository.name";

    public static final String PROP_REPOSITORY_SLUG =
        "ramadda.repository.slug";    

    public static final String PROP_REPOSITORY_DESCRIPTION =
        "ramadda.repository.description";

    public static final String PROP_REQUEST_PATTERN =
        "ramadda.request.pattern";

    public static final String PROP_SHOW_APPLET = "ramadda.html.showapplet";

    public static final String PROP_SSL_IGNORE = "ramadda.ssl.ignore";

    public static final String PROP_SSL_PORT = "ramadda.ssl.port";

    public static final String PROP_SSL_PASSWORD = "ramadda.ssl.password";

    public static final String PROP_SSL_KEYPASSWORD =
        "ramadda.ssl.keypassword";

    public static final String PROP_SSL_KEYSTORE = "ramadda.ssl.keystore";

    public static final String PROP_SSL_CERTALIAS = "ramadda.ssl.certalias";

    public static final String PROP_TYPES = "ramadda.types";

    public static final String PROP_UPLOAD_MAXSIZEGB =
        "ramadda.upload.maxsizegb";

    public static final String PROP_CACHE_TTL = "ramadda.cache.ttl";

    public static final String PROP_CACHE_MAXSIZEGB =
        "ramadda.cache.maxsizegb";

    public static final String PROP_ZIPOUTPUT_REGISTERED_MAXSIZEMB =
        "ramadda.zip.registered.maxsizemb";

    public static final String PROP_ZIPOUTPUT_ANONYMOUS_MAXSIZEMB =
        "ramadda.zip.anonymous.maxsizemb";

    public static final String PROP_USER_RESET_ID_SUBJECT =
        "ramadda.user.reset.id.subject";

    public static final String PROP_USER_RESET_ID_TEMPLATE =
        "ramadda.user.reset.id.template";

    public static final String PROP_USER_RESET_PASSWORD_SUBJECT =
        "ramadda.user.reset.password.subject";

    public static final String PROP_USER_RESET_PASSWORD_TEMPLATE =
        "ramadda.user.reset.password.template";

    public static final String PROP_VERSION = "ramadda.version";

    /**  */
    public static final String PROP_VERSION_MAJOR = "ramadda.version.major";

    /**  */
    public static final String PROP_VERSION_MINOR = "ramadda.version.minor";

    /**  */
    public static final String PROP_VERSION_PATCH = "ramadda.version.patch";

    public static final String TYPE_ANY = "any";

    public static final String TYPE_ASSOCIATION = "association";

    public static final String TYPE_FILE = "file";

    public static final String TYPE_GROUP = "group";

    public static final String ACTION_ADD = "action.add";

    public static final String ACTION_CLEAR = "action.clear";

    public static final String ACTION_COPY = "action.copy";

    public static final String ACTION_DELETE_ASK = "action.delete.ask";

    public static final String ACTION_DELETE_DOIT = "action.delete.doit";

    public static final String ACTION_EDIT = "action.edit";

    public static final String ACTION_MOVE = "action.move";

    public static final String ACTION_REMOVE = "action.remove";

    public static final String ACTION_SPLIT = "action.split";

    public static final String ACTION_START = "action.start";

    public static final String ACTION_STOP = "action.stop";

    public static final String ACTION_PASSWORDS_CLEAR =
        "action.passwords.clear";

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

    public static final String WHAT_ENTRIES = "entries";

    public static final String WHAT_TYPE = "type";

    public static final String WHAT_TAG = "tag";

    public static final String WHAT_METADATA = "metadata";

    public static final String WHAT_ASSOCIATION = "association";

    public static final String WHAT_USER = "user";

    public static final int DB_MAX_ROWS = 1000;

    public static final int DB_VIEW_ROWS = 100;

    public static final int VIEW_MAX_ROWS = DB_VIEW_ROWS;

    public static final String NODETYPE_ENTRY = "entry";

    public static final String NODETYPE_GROUP = "group";

    public static final String RESPONSE_XML = "xml";

    public static final String RESPONSE_TEXT = "text";

    public static final String RESPONSE_JSON = "json";

    public static final String CODE_OK = "ok";

    public static final String CODE_ERROR = "error";

    public static final String NEWLINE = "\n";

    public static final String BR = "<br>";

    public static final String HR = "<hr>";

    public static final String BLANK = "";

    public static final String MIME_XML = "text/xml";

    public static final String MIME_TEXT = "text/plain";

    public static final boolean DFLT_INHERITED = false;

    public static final String DEFAULT_SEARCH_SIZE = "100";

    public static final String ID_PREFIX_SYNTH = "synth:";

    //j++

    public static final String MSG_ACCESS_CHANGED = "Permissions Changed";

    public static final String MSG_ASSOCIATION_ADDED =
        "The association has been added";

    public static final String SERVICE_OPENDAP = "opendap";

    public static final String SERVICE_FILE = "file";

    public static long MEGA = 1000000;

    public static long GIGA = MEGA * 1000;

    public static final String PROP_PROXY_USER = "ramadda.proxy.user";

    public static final String PROP_PROXY_PASSWORD = "ramadda.proxy.password";

    public static final String PROP_PROXY_HOST = "ramadda.proxy.host";

    public static final String PROP_PROXY_PORT = "ramadda.proxy.port";

    public static final String PROP_ENCRYPT_PASSWORD =
        "ramadda.encrypt.password";

    public static final String PROP_ENCRYPT_CIPHER = "ramadda.encrypt.cipher";

    public static final String PROP_AWS_KEY = "ramadda.aws.key";
    public static final String PROP_S3_ENDPOINT = "ramadda.aws.endpoint";
    public static final String PROP_S3_ENDPOINT_REGION = "ramadda.aws.endpoint.region";        

    public static final String BREADCRUMB_SEPARATOR = "&raquo;";

    public static final String BREADCRUMB_SEPARATOR_PAD =
        "&nbsp;&raquo;&nbsp;";

    public static final String CSS_CLASS_ENTRY_TREE_ROW = "entry-tree-row";

    public static final String CSS_CLASS_ENTRY_LIST_ROW = "entry-list-row";

    public static final String CSS_CLASS_ENTRY_ROW_LABEL = "entry-row-label";

    public static final String CSS_CLASS_FOLDER_BLOCK =
        "ramadda-folder-block";

    public static final String CSS_CLASS_SERVER = "ramadda-server";

    public static final String CSS_CLASS_SERVER_BLOCK =
        "ramadda-server-block";

    public static final String CSS_CLASS_STACK = "ramadda-stack";

    public static final String CSS_CLASS_SMALLLINK = "ramadda-smalllink";

    public static final String CSS_CLASS_SMALLHELP = "ramadda-smallhelp";

    public static final String CSS_CLASS_MENUBAR = "ramadda-menubar";

    public static final String CSS_CLASS_MENUBUTTON = "ramadda-menubutton";

    public static final String CSS_CLASS_MENUBUTTON_SEPARATOR =
        "ramadda-menubutton-separator";

    public static final String CSS_CLASS_DATETIME = "ramadda-datetime";

    public static final String CSS_CLASS_MENUITEM_LINK =
        "ramadda-menuitem-link";

    public static final String CSS_CLASS_MENUITEM_SEPARATOR =
        "ramadda-menuitem-separator";

    public static final String CSS_CLASS_MENUITEM = "ramadda-menuitem";

    public static final String CSS_CLASS_MENU_GROUP = "ramadda-menugroup";

    public static final String CSS_CLASS_HIGHLIGHT = "ramadda-highlight";

    public static final String CSS_CLASS_ERROR_LABEL = "ramadda-error";

    public static final String CSS_CLASS_HEADING_1 = "ramadda-heading-1";

    public static final String CSS_CLASS_HEADING_2 = "ramadda-heading-2";

    public static final String CSS_CLASS_HEADING_2_LINK =
        "ramadda-heading-2-link";

    public static final String CSS_CLASS_POPUP = "ramadda-popup";

    public static final String CSS_CLASS_REQUIRED = "ramadda-required";

    public static final String CSS_CLASS_REQUIRED_LABEL =
        "ramadda-required-label";

    public static final String CSS_CLASS_REQUIRED_FIELD =
        "ramadda-required-field";

    public static final String CSS_CLASS_REQUIRED_DISABLED =
        "ramadda-required-disabled";

    public static final String CSS_CLASS_EARTH_NAV = "ramadda-earth-nav";

    public static final String CSS_CLASS_EARTH_LINK = "ramadda-earth-link";

    public static final String CSS_CLASS_EARTH_ENTRIES =
        "ramadda-earth-entries";

    public static final String CSS_CLASS_EARTH_CONTAINER =
        "ramadda-earth-container";

    public static final String CSS_CLASS_COMMENT_BLOCK =
        "ramadda-comment-block";

    public static final String CSS_CLASS_COMMENT_COMMENTER =
        "ramadda-comment-commenter";

    public static final String CSS_CLASS_COMMENT_DATE =
        "ramadda-comment-date";

    public static final String CSS_CLASS_COMMENT_INNER =
        "ramadda-comment-inner";

    public static final String CSS_CLASS_COMMENT_SUBJECT =
        "ramadda-comment-subject";

    public static final String CSS_CLASS_USER_FIELD = "ramadda-user-field";

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

    public static final String LABEL_NEW_ENTRY = "Pick a Type...";

    public static final String LABEL_ENTRY = "Entry";

    public static final String LABEL_ENTRIES = "Entries";

    public static final String LABEL_EMPTY_FOLDER = "This folder is empty";

    public static final String LABEL_NO_ENTRIES_FOUND = "No entries found";

    public static final String ARG_IMPORT_TYPE = "import.type";

    public static final String FILTER_IMAGE = "image";
    public static final String FILTER_IMAGE_OR_ATTACHMENT = "image_or_attachment";    

    public static final String FILTER_FILE = "file";

    public static final String FILTER_GEO = "geo";

    public static final String FILTER_FOLDER = "folder";

    public static final String FILTER_TYPE = "type:";

    public static final String FILTER_SUFFIX = "suffix:";

    public static final String FILTER_NAME = "name:";

    public static final String FILTER_ID = "id:";

    public static final String ORDERBY_NONE = "none";

    public static final String ORDERBY_DATE = "date";

    public static final String ORDERBY_FROMDATE = "fromdate";

    public static final String ORDERBY_TODATE = "todate";

    public static final String ORDERBY_CHANGEDATE = "changedate";

    public static final String ORDERBY_SIZE = "size";

    public static final String ORDERBY_TYPE = "type";

    /**  */
    public static final String ORDERBY_RELEVANT = "relevant";

    public static final String ORDERBY_ENTRYORDER = "entryorder";

    public static final String ORDERBY_CREATEDATE = "createdate";

    public static final String ORDERBY_NAME = "name";

    /**  */
    public static final String ORDERBY_NUMBER = "number";

    public static final String ORDERBY_MIXED = "mixed";

    public static final String MACRO_ROOT = "root";

    public static final RequestArgument REQUESTARG_NORTH =
        new RequestArgument("ramadda.arg.area.north");

    public static final RequestArgument REQUESTARG_WEST =
        new RequestArgument("ramadda.arg.area.west");

    public static final RequestArgument REQUESTARG_SOUTH =
        new RequestArgument("ramadda.arg.area.south");

    public static final RequestArgument REQUESTARG_EAST =
        new RequestArgument("ramadda.arg.area.east");

    public static final RequestArgument REQUESTARG_LATITUDE =
        new RequestArgument("ramadda.arg.latitude");

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

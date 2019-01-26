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

package org.ramadda.repository.output;


import org.ramadda.repository.Constants;


/**
 */
public interface WikiConstants {


    /** wiki page type */
    public static String TYPE_WIKIPAGE = "wikipage";


    /** _more_ */
    public static final String ATTR_ALIGN = "align";

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
    public static final String ATTR_LISTENTRIES = "listentries";

    /** _more_ */
    public static final String ATTR_LAYER = "layer";

    /** listwidth attribute */
    public static final String ATTR_LISTWIDTH = "listwidth";

    /** link attribute */
    public static final String ATTR_LINK = "link";

    /** attribute in the tabs tag */
    public static final String ATTR_USEDESCRIPTION = "usedescription";

    /** attribute in the tabs tag */
    public static final String ATTR_SHOWLINK = "showlink";

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


    /** include icon attribute */
    public static final String ATTR_INCLUDEICON = "includeicon";

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

    /** sort date attribute */
    public static final String SORT_DATE = "date";

    /** change date attribute */
    public static final String SORT_CHANGEDATE = "changedate";

    /** sort name attribute */
    public static final String SORT_NAME = "name";

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
    public static final String ATTR_THUMBNAIL = "thumbnail";

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

    /** _more_ */
    public static final String WIKI_TAG_EMBED = "embed";

    /** _more_ */
    public static final String WIKI_TAG_CHART = "chart";

    /** _more_ */
    public static final String WIKI_TAG_DISPLAY = "display";

    /** wiki import */
    public static final String WIKI_TAG_IMPORT = "import";

    /** the field property */
    public static final String WIKI_TAG_FIELD = "field";

    /** _more_ */
    public static final String WIKI_TAG_ROOT = "root";

    /** the calendar property */
    public static final String WIKI_TAG_CALENDAR = "calendar";

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


    /** _more_ */
    public static final String WIKI_TAG_SEARCH = "search";

    /** wiki import */
    public static final String WIKI_TAG_TREE = "tree";

    /** _more_ */
    public static final String WIKI_TAG_TREEVIEW = "treeview";

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

    /** _more_ */
    public static final String WIKI_TAG_APPLY = "apply";

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

    /** the slideshow property */
    public static final String WIKI_TAG_SLIDESHOW = "slideshow";

    /** wiki import */
    public static final String WIKI_TAG_GRID = "grid";

    /** wiki import */
    public static final String WIKI_TAG_TOOLBAR = "toolbar";

    /** wiki import */
    public static final String WIKI_TAG_BREADCRUMBS = "breadcrumbs";

    /** wiki import */
    public static final String WIKI_TAG_INFORMATION = "information";

    /** _more_ */
    public static final String WIKI_TAG_DOWNLOAD = "download";

    /** wiki import */
    public static final String WIKI_TAG_IMAGE = "image";

    /** _more_ */
    public static final String WIKI_TAG_STREETVIEW = "streetview";

    /** wiki import */
    public static final String WIKI_TAG_NAME = "name";

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

    /** _more_ */
    public static final String WIKI_TAG_SIMPLE = "simple";

    /** wiki import */
    public static final String WIKI_TAG_PROPERTIES = "properties";

    public static final String WIKI_TAG_PROPERTY = "property";

    /** _more_ */
    public static final String WIKI_TAG_LABEL = "label";


    /** wiki import */
    public static final String WIKI_TAG_LINKS = "links";

    /** link property */
    public static final String WIKI_TAG_LINK = "link";

    /** list property */
    public static final String WIKI_TAG_LIST = "list";

    /** wiki import */
    public static final String WIKI_TAG_ENTRYID = "entryid";

    /** wiki import */
    public static final String WIKI_TAG_LAYOUT = "layout";

    /** wiki import */
    public static final String WIKI_TAG_CHILDREN_GROUPS = "subgroups";

    /** wiki import */
    public static final String WIKI_TAG_CHILDREN_ENTRIES = "subentries";

    /** wiki import */
    public static final String WIKI_TAG_CHILDREN = "children";

    /** wiki import */
    public static final String WIKI_TAG_URL = "url";

    /** _more_ */

    public static final String WIKI_TAG_RESOURCE = "resource";

    /** _more_ */
    public static final String WIKI_TAG_TOOLS = "tools";

    /** Upload property */
    public static final String WIKI_TAG_UPLOAD = "upload";


    /** _more_ */
    public static final String FILTER_IMAGE = "image";

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
    public static final String FILTER_ID = "id:";

    /** property delimiter */
    public static final String PROP_DELIM = ":";

    /** the id for this */
    public static final String ID_THIS = "this";

    /** _more_ */
    public static final String ID_REMOTE = "remote:";

    /** _more_ */
    public static final String ID_ROOT = "root";

    /** _more_ */
    public static final String ID_CHILDREN = "children";

    /** _more_ */
    public static final String PREFIX_SEARCH = "search.";

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

    /** _more_ */
    public static final String ID_SEARCH = "search";

    /** _more_ */
    public static final String ID_SIBLINGS = "siblings";

    /** the id for my parent */
    public static final String ID_PARENT = "parent";

    /** _more_ */
    public static final String ID_ANCESTORS = "ancestors";

    /** the id for my grandparent */
    public static final String ID_GRANDPARENT = "grandparent";

    /** _more_ */
    public static final String ID_GRANDCHILDREN = "grandchildren";

    /** _more_ */
    public static final String ID_GREATGRANDCHILDREN = "greatgrandchildren";

    /** _more_ */
    public static final String ID_LINKS = "links";

    /** default label */
    public static final String LABEL_LINKS = "Actions";


}

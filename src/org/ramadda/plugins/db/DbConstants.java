/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.db;


import org.ramadda.repository.Constants;


/**
 *
 */

public interface DbConstants extends Constants {

    /** _more_ */
    public static final int IDX_DBID = 0;

    /** _more_ */
    public static final int IDX_DBUSER = 1;

    /** _more_ */
    public static final int IDX_DBCREATEDATE = 2;

    /** _more_ */
    public static final int IDX_DBPROPS = 3;

    /** _more_ */
    public static final int IDX_MAX_INTERNAL = 3;



    /** _more_ */
    public static final String PROP_ANONFORM_ENABLED = "anonform.enabled";

    /** _more_ */
    public static final String PROP_ANONFORM_MESSAGE = "anonform.message";

    /** _more_ */
    public static final int DEFAULT_MAX = 500;

    /** _more_ */
    public static final String ATTR_RSS_VERSION = "version";

    /** _more_ */
    public static final String TAG_DBVALUES = "dbvalues";

    /** _more_ */
    public static final String TAG_CSV = "csv";


    /** _more_ */
    public static final String OUTPUT_HTML = "html";

    /** _more_ */
    public static final String OUTPUT_CSV = "csv";

    /** _more_ */
    public static final String OUTPUT_JSON = "json";


    /** _more_ */
    public static final String VIEW_EDIT = "edit";


    /** _more_ */
    public static final String VIEW_NEW = "new";

    /** _more_ */
    public static final String VIEW_TABLE = "table";

    /** _more_ */
    public static final String VIEW_CALENDAR = "calendar";

    /** _more_ */
    public static final String VIEW_ICAL = "ical";

    /** _more_ */
    public static final String VIEW_TIMELINE = "timeline";

    /** _more_ */
    public static final String VIEW_MAP = "map";

    /** _more_ */
    public static final String VIEW_ADDRESSLABELS = "addresslabels";


    /** _more_ */
    public static final String VIEW_SEARCH = "search";

    /** _more_ */
    public static final String VIEW_GRID = "grid";

    /** _more_ */
    public static final String VIEW_CATEGORY = "category";

    /** _more_ */
    public static final String VIEW_CSV = "csv";

    /** _more_ */
    public static final String VIEW_JSON = "json";

    /** _more_ */
    public static final String VIEW_KML = "kml";

    /** _more_ */
    public static final String VIEW_RSS = "rss";


    /** _more_ */
    public static final String CSS_DB_HEADER = "dbheader";

    /** _more_ */
    public static final String CSS_DB_TABLEHEADER = "dbtableheader";

    /** _more_ */
    public static final String CSS_DB_TABLEHEADER_INNER =
        "dbtableheader_inner";

    /** _more_ */
    public static final String ARG_SEARCH_FROM = "searchfrom";

    /** _more_ */
    public static final String ARG_EXTRA_COLUMNS = "extracolumns";


    /** _more_ */
    public static final String ARG_FOR_PRINT = "forprint";

    public static final String  ARG_DB_SIMPLEMAP = "simplemap";

    public static final String  ARG_DB_MAPPROPS = "mapprops";

    /** _more_ */
    public static final String ARG_ENTRIES_PER_PAGE = "entriesperpage";
    public static final String ARG_NUMBER_ENTRIES = "numberentries";
    public static final String ARG_TILE_ENTRIES = "tileentries";    

    /** _more_ */
    public static final String ARG_AGG_PERCENT = "aggpercent";

    /** _more_ */
    public static final String ARG_DB_VIEW = "db.view";



    /** _more_ */
    public static final String ARG_DB_ITERATE = "dbiterate";

    /** _more_ */
    public static final String ARG_DB_ITERATE_VALUES = "dbiteratevalues";

    /** _more_ */
    public static final String ARG_DB_SHOWHEADER = "db.showheader";

    /** _more_ */
    public static final String ARG_VIEW = "view";

    /** _more_ */
    public static final String ARG_DB_ALL = "db.all";

    /** _more_ */
    public static final String ARG_DB_SHOW = "show";

    /** _more_ */
    public static final String ARG_DB_UNIQUE = "db_view";

    /** _more_ */
    public static final String ARG_ENUM_ICON = "db.icon";

    /** _more_ */
    public static final String ARG_ENUM_COLOR = "db.color";


    /** _more_ */
    public static final String ARG_DB_BULKCOL = "db.bulkcol";

    /** _more_ */
    public static final String ARG_DB_OR = "dbsearchor";

    /** _more_ */
    public static final String ARG_DB_BULK_TEXT = "db.bulk.text";

    /** _more_ */
    public static final String ARG_DB_BULK_FILE = "db.bulk.file";

    /** _more_ */
    public static final String ARG_DB_BULK_DELIMITER = "db.bulk.delimiter";

    /** _more_ */
    public static final String ARG_DB_BULK_NUKEIT = "db.bulk.nukeit";

    /** _more_ */
    public static final String ARG_DB_BULK_SKIP = "db.bulk.skip";

    /** _more_ */
    public static final String ARG_DB_BULK_LOCALFILE = "db.bulk.localfile";

    /** _more_ */
    public static final String ARG_DB_DO = "db.do";

    /** _more_ */
    public static final String ARG_DB_SEARCHNAME = "searchname";

    public static final String ARG_DB_SUBTITLE = "subtitle";    

    /** _more_ */
    public static final String ARG_DB_SEARCHDESC = "searchdesc";

    /** _more_ */
    public static final String ARG_DB_SEARCHID = "searchid";

    /** _more_ */
    public static final String ARG_DB_MACRO = "macro";

    /** _more_ */
    public static final String METADATA_SAVEDSEARCH = "db_saved_search";

    /** _more_ */
    public static final String ARG_DB_DOSAVESEARCH = "dosavesearch";

    /** _more_ */
    public static final String ARG_DB_GROUP_SORTDIR = "groupsortdir";

    /** _more_ */
    public static final String ARG_DB_GROUP_SORTBY = "groupsortby";


    /** _more_ */
    public static final String ARG_DB_SORTBY = "dbsortby";

    /** _more_ */
    public static final String ARG_DB_SORTDIR = "dbsortdir";


    /** _more_ */
    public static final String ARG_DB_SORTBY1 = "dbsortby1";

    /** _more_ */
    public static final String ARG_DB_SORTDIR1 = "dbsortdir1";


    /** _more_ */
    public static final String ARG_DB_OUTPUT = "dboutput";

    /** _more_ */
    public static final String ARG_DB_NEWFORM = "db.newform";

    /** _more_ */
    public static final String ARG_DB_EDITFORM = "db.editform";

    /** _more_ */
    public static final String ARG_DB_CSVFILE = "db.csvfile";

    /** _more_ */
    public static final String ARG_DB_SEARCHFORM = "db.searchform";

    /** _more_ */
    public static final String ARG_DB_SEARCH = "db.search";

    /** _more_ */
    public static final String ARG_DB_LIST = "db.list";

    /** _more_ */
    public static final String ARG_DB_EDITSQL = "db.editsql";

    /** _more_ */
    public static final String ARG_DB_SETPOS = "db.setpos";

    /** _more_ */
    public static final String ARG_DB_ENTRY = "db.entry";

    /** _more_ */
    public static final String ARG_DB_CREATE = "db.create";

    /** _more_ */
    public static final String ARG_DB_EDIT = "db.edit";

    /** _more_ */
    public static final String ARG_DB_COPY = "db.copy";

    /** _more_ */
    public static final String ARG_DB_COLUMN = "db.column";


    /** _more_ */
    public static final String ARG_DB_SETVALUE = "db.setvalue";

    /** _more_ */
    public static final String ARG_DB_TEST = "db.test";

    /** _more_ */
    public static final String ARG_DB_WHERECOLUMN = "db.wherecolumn";

    /** _more_ */
    public static final String ARG_DB_WHEREVALUE = "db.wherevalue";

    /** _more_ */
    public static final String ARG_DB_WHEREOP = "db.whereop";

    /** _more_ */
    public static final String ARG_DB_CONFIRM = "db.confirm";

    /** _more_ */
    public static final String ARG_DB_APPLY = "db.apply";



    /** _more_ */
    public static final String ARG_DB_DELETE = "db.delete";

    /** _more_ */
    public static final String ARG_DB_ACTION_CONFIRM = "db.action.confirm";

    /** _more_ */
    public static final String ARG_DB_ACTION = "db.action";

    /** _more_ */
    public static final String ARG_DBID = "dbid";

    /** _more_ */
    public static final String ARG_DBIDS = "dbids";

    /** _more_ */
    public static final String ARG_DBID_SELECTED = "dbid_selected";


    /** _more_ */
    public static final String ACTION_LIST = "db.list";

    /** _more_ */
    public static final String ACTION_SET_LATLON = "db.setlatlon";


    /** _more_ */
    public static final String ACTION_DELETE = "db.delete";

    /** _more_ */
    public static final String ACTION_DELETEALL = "db.deleteall";

    /** _more_ */
    public static final String ACTION_EMAIL = "db.email";

    /** _more_ */
    public static final String ACTION_CALENDAR = "db.calendar";

    /** _more_ */
    public static final String ACTION_MAP = "db.map";

    /** _more_ */
    public static final String ACTION_CSV = "db.csv";

    /** _more_ */
    public static final String ACTION_JSON = "db.json";


    /** _more_ */
    public static String ARG_EMAIL_FROMADDRESS = "email.fromaddress";

    /** _more_ */
    public static String ARG_EMAIL_TO = "email.to";

    /** _more_ */
    public static String ARG_EMAIL_FROMNAME = "email.fromname";

    /** _more_ */
    public static String ARG_EMAIL_SUBJECT = "email.subject";

    /** _more_ */
    public static String ARG_EMAIL_MESSAGE = "email.message";

    /** _more_ */
    public static String ARG_EMAIL_BCC = "email.bcc";

    /** _more_ */
    public static final String PROP_CAT_COLOR = "cat.color";

    /** _more_ */
    public static final String PROP_CAT_ICON = "cat.icon";



    /** _more_ */
    public static final String COL_DBID = "db_id";

    /** _more_ */
    public static final String COL_DBUSER = "db_user";

    /** _more_ */
    public static final String COL_DBCREATEDATE = "db_createdate";

    /** _more_ */
    public static final String COL_DBPROPS = "db_props";

}

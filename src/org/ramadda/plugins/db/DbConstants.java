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

package org.ramadda.plugins.db;


import org.ramadda.data.point.text.*;

import org.ramadda.data.record.*;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;



import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.map.*;
import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.CalendarOutputHandler;
import org.ramadda.repository.output.MapOutputHandler;
import org.ramadda.repository.output.OutputHandler;
import org.ramadda.repository.output.OutputType;
import org.ramadda.repository.output.RssOutputHandler;
import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.type.*;
import org.ramadda.util.Bounds;
import org.ramadda.util.FormInfo;
import org.ramadda.util.GoogleChart;

import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;

import org.ramadda.util.KmlUtil;
import org.ramadda.util.RssUtil;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.XmlUtils;
import org.ramadda.util.sql.*;

import org.ramadda.util.text.CsvUtil;
import org.ramadda.util.text.Filter;
import org.ramadda.util.text.Processor;
import org.ramadda.util.text.TextReader;


import org.w3c.dom.*;
import org.w3c.dom.*;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;



import java.io.*;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.sql.Statement;

import java.text.DecimalFormat;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import java.util.TimeZone;


/**
 *
 */

public interface DbConstants extends Constants {

    /** _more_ */
    public static final String PROP_ANONFORM_ENABLED = "anonform.enabled";

    /** _more_ */
    public static final String PROP_ANONFORM_MESSAGE = "anonform.message";

    /** _more_ */
    public static final int DEFAULT_MAX = DB_VIEW_ROWS;

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
    public static final String VIEW_SEARCH = "search";

    /** _more_ */
    public static final String VIEW_CHART = "chart";

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
    public static final String VIEW_STICKYNOTES = "stickynotes";

    /** _more_ */
    public static final String VIEW_RSS = "rss";


    /** _more_ */
    public static final String CSS_DB_HEADER = "dbheader";

    /** _more_ */
    public static final String CSS_DB_TABLEHEADER = "dbtableheader";

    /** _more_ */
    public static final String CSS_DB_TABLEHEADER_INNER =
        "dbtableheader_inner";

    /** _more_          */
    public static final String ARG_AGG_PERCENT = "aggpercent";

    /** _more_ */
    public static final String ARG_DB_VIEW = "db.view";

    /** _more_ */
    public static final String ARG_DB_SHOWHEADER = "db.showheader";

    /** _more_ */
    public static final String ARG_VIEW = "view";

    /** _more_ */
    public static final String ARG_DB_ALL = "db.all";

    public static final String ARG_DB_SHOW = "show";



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

    /** _more_          */
    public static final String ARG_DB_BULK_DELIMITER = "db.bulk.delimiter";

    /** _more_          */
    public static final String ARG_DB_BULK_SKIP = "db.bulk.skip";

    /** _more_ */
    public static final String ARG_DB_BULK_LOCALFILE = "db.bulk.localfile";

    /** _more_ */
    public static final String ARG_DB_DO = "db.do";

    /** _more_ */
    public static final String ARG_DB_SEARCHNAME = "searchname";

    /** _more_ */
    public static final String ARG_DB_SEARCHID = "searchid";

    /** _more_ */
    public static final String METADATA_SAVEDSEARCH = "db_saved_search";

    /** _more_ */
    public static final String ARG_DB_DOSAVESEARCH = "dosavesearch";

    /** _more_ */
    public static final String ARG_DB_SORTBY = "dbsortby";

    /** _more_ */
    public static final String ARG_DB_SORTDIR = "dbsortdir";

    /** _more_ */
    public static final String ARG_DB_OUTPUT = "dboutput";

    /** _more_ */
    public static final String ARG_DB_NEWFORM = "db.newform";

    /** _more_ */
    public static final String ARG_DB_CSVFILE = "db.csvfile";

    /** _more_ */
    public static final String ARG_DB_SEARCHFORM = "db.searchform";

    /** _more_ */
    public static final String ARG_DB_SEARCH = "db.search";

    /** _more_ */
    public static final String ARG_DB_LIST = "db.list";

    /** _more_ */
    public static final String ARG_DB_EDITFORM = "db.editform";

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
    public static final String ARG_DB_DELETE = "db.delete";

    /** _more_ */
    public static final String ARG_DB_DELETECONFIRM = "db.delete.confirm";

    /** _more_ */
    public static final String ARG_DB_ACTION = "db.action";

    /** _more_ */
    public static final String ARG_DB_STICKYLABEL = "db.stickylabel";


    /** _more_ */
    public static final String ARG_DBID = "dbid";

    /** _more_ */
    public static final String ARG_DBIDS = "dbids";

    /** _more_ */
    public static final String ARG_DBID_SELECTED = "dbid_selected";


    /** _more_ */
    public static final String ACTION_LIST = "db.list";


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
    public static final String PROP_STICKY_LABELS = "sticky.labels";


    /** _more_ */
    public static final String PROP_STICKY_POSX = "sticky.posx";

    /** _more_ */
    public static final String PROP_STICKY_POSY = "sticky.posy";

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

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

package org.ramadda.repository.type;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.*;


import org.ramadda.repository.map.*;

import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;

import org.ramadda.repository.output.WikiConstants;
import org.ramadda.repository.search.SearchManager;
import org.ramadda.repository.search.SpecialSearch;
import org.ramadda.repository.util.DateArgument;

import org.ramadda.repository.util.FileWriter;
import org.ramadda.repository.util.RequestArgument;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.service.Service;
import org.ramadda.service.ServiceInput;
import org.ramadda.service.ServiceOutput;
import org.ramadda.util.FileInfo;
import org.ramadda.util.FormInfo;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;
import org.ramadda.util.SelectionRectangle;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.Element;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provide the core services around the entry types.
 *
 *
 *
 * @author RAMADDA Development Team
 */
public class TypeHandler extends RepositoryManager {

    /** _more_ */
    static int xcnt;

    /** _more_ */
    public String myid = "typehandler-" + (xcnt++);

    /** _more_ */
    private String[] FIELDS_ENTRY = {
        ARG_NAME, ARG_DESCRIPTION, ARG_RESOURCE, ARG_CATEGORY, ARG_DATE,
        ARG_LOCATION
    };

    /** _more_ */
    private String[] FIELDS_NOENTRY = { ARG_NAME, ARG_RESOURCE,
                                        ARG_DESCRIPTION, ARG_DATE,
                                        ARG_LOCATION };



    /** _more_ */
    public static final String ID_DELIMITER = ":";

    /** _more_ */
    public static final String TARGET_ATTACHMENT = "attachment";

    /** _more_ */
    public static final String TARGET_CHILD = "child";

    /** _more_ */
    public static final String TARGET_SIBLING = "sibling";

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

    /** _more_ */
    public static final RequestArgument REQUESTARG_FROMDATE =
        new RequestArgument("ramadda.arg.fromdate");

    /** _more_ */
    public static final RequestArgument REQUESTARG_TODATE =
        new RequestArgument("ramadda.arg.todate");

    /** _more_ */
    public static final RequestArgument[] AREA_NWSE = { REQUESTARG_NORTH,
            REQUESTARG_WEST, REQUESTARG_SOUTH, REQUESTARG_EAST };


    /** _more_ */
    public static final String CATEGORY_DEFAULT = "Information";

    /** _more_ */
    public static final String TYPE_ANY = Constants.TYPE_ANY;

    /** _more_ */
    public static final String TYPE_GUESS = "guess";

    /** _more_ */
    public static final String TYPE_FINDMATCH = "findmatch";



    /** _more_ */
    public static final String TYPE_FILE = Constants.TYPE_FILE;

    /** _more_ */
    public static final String TYPE_GROUP = Constants.TYPE_GROUP;

    /** _more_ */
    public static final String TYPE_HOMEPAGE = "homepage";

    /** _more_ */
    public static final String TYPE_CONTRIBUTION = "contribution";



    /** _more_ */
    public static final String TAG_COLUMN = "column";

    /** _more_ */
    public static final String TAG_PROPERTY = "property";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_METADATA = "metadata";

    /** _more_ */
    public static final String ATTR_CHILDTYPES = "childtypes";


    /** _more_ */
    public static final String ATTR_PATTERN = "pattern";

    /** _more_ */
    public static final String ATTR_WIKI = "wiki";


    /** _more_ */
    public static final String ATTR_BUBBLE = "bubble";

    /** _more_ */
    public static final String ATTR_WIKI_INNER = "wiki_inner";

    /** _more_ */
    public static final String TAG_CHILDREN = "children";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String ATTR_CATEGORY = "category";

    /** _more_ */
    public static final String ATTR_SUPERCATEGORY = "supercategory";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_METADATA = "metadata";

    /** _more_ */
    public static final String ATTR_HANDLER = "handler";




    /** _more_ */
    public static final int MATCH_UNKNOWN = 0;

    /** _more_ */
    public static final int MATCH_TRUE = 1;

    /** _more_ */
    public static final int MATCH_FALSE = 2;

    /** _more_ */
    public static String DFLT_WIKI_HEADER =
        "{{name}}\n{{description box.class=\"entry-page-description\"}}";


    /** _more_ */
    public static final String PROP_FIELD_FILE_PATTERN = "field_file_pattern";


    /** _more_ */
    public static final String PROP_INGEST_LINKS = "ingestLinks";



    /** _more_ */
    public static final String ALL = "-all-";

    /** _more_ */
    public static final TwoFacedObject ALL_OBJECT = new TwoFacedObject(ALL,
                                                        ALL);

    /** _more_ */
    public static final TwoFacedObject NONE_OBJECT =
        new TwoFacedObject("None", "");



    /** _more_ */
    private static List<DateArgument> dateArgs;


    /** for debugging */
    static int cnt = 0;

    /** for debugging */
    int mycnt = cnt++;

    /** the type id */
    private String type;

    /** The entry types hierarchy */
    private TypeHandler parent;

    /** The entry types hierarchy */
    private List<TypeHandler> childrenTypes = new ArrayList<TypeHandler>();

    /** _more_ */
    private String description;

    /** _more_ */
    private String editHelp = "";

    /** _more_ */
    private String iconPath;

    /** _more_ */
    private String category = CATEGORY_DEFAULT;

    /** _more_ */
    private String superCategory = "";

    /** _more_ */
    private Hashtable properties = new Hashtable();

    /** every type has its own search service managed by the specialSearch */
    private SpecialSearch specialSearch;


    /**
     *   the pattern= attribute in types.xml. Used when trying to figure out what entry type
     *   to use for a file
     */
    private String filePattern;

    /**
     *   the field_file_pattern attribute in types.xml. Used when trying to figure out what entry type
     *   to use for a file and to set the entry values from
     */
    private Pattern fieldFilePattern;


    /** _more_ */
    private List<String> fieldPatternNames;


    /** the wiki tag in types.xml. If defined then use this as the default html display for entries of this type */
    private String wikiTemplate;

    /** _more_ */
    private String bubbleTemplate;

    /** _more_ */
    private String defaultChildrenEntries;

    /** _more_ */
    private String wikiTemplateInner;

    /** _more_ */
    private DecimalFormat latLonFormat = new DecimalFormat("##0.00");

    /** _more_ */
    private String defaultCategory;

    /** _more_ */
    private String displayTemplatePath;


    /** Should users be shown this type when doing a New Entry... */
    private boolean forUser = true;

    /** Default metadata types to show in Edit->Add Property menu */
    private List<String> metadataTypes;

    /** The default child entry types to show in the File->New menu */
    private List<String> childTypes;

    /** _more_ */
    private List<String[]> requiredMetadata = new ArrayList<String[]>();

    /** _more_ */
    private List<Service> services = new ArrayList<Service>();

    /** _more_ */
    private Entry synthTopLevelEntry;




    /**
     * ctor
     *
     * @param repository ramadda
     */
    public TypeHandler(Repository repository) {
        super(repository);
    }


    /**
     * ctor
     *
     * @param repository ramadda
     * @param entryNode types.xml node
     */
    public TypeHandler(Repository repository, Element entryNode) {
        this(repository);
        if (entryNode != null) {
            initTypeHandler(entryNode);
        }
    }


    /**
     * ctor
     *
     * @param repository ramadda
     * @param type the type
     */
    public TypeHandler(Repository repository, String type) {
        this(repository, type, "");
    }


    /**
     * ctor
     *
     * @param repository ramadda
     * @param type the type
     * @param description type description
     */
    public TypeHandler(Repository repository, String type,
                       String description) {
        this(repository, type, description, CATEGORY_DEFAULT);
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     * @param category _more_
     */
    public TypeHandler(Repository repository, String type,
                       String description, String category) {
        super(repository);
        this.type        = type;
        this.description = description;
        if (category != null) {
            this.category = category;
        }
    }




    /**
     * _more_
     *
     * @param node _more_
     *
     */
    public void initTypeHandler(Element node) {

        try {
            displayTemplatePath = Utils.getAttributeOrTag(node,
                    "displaytemplate", (String) null);

            category = Utils.getAttributeOrTag(node, ATTR_CATEGORY,
                    (String) null);
            if (category == null) {
                category = XmlUtil.getAttributeFromTree(node, ATTR_CATEGORY,
                        CATEGORY_DEFAULT);
            }
            iconPath = XmlUtil.getAttributeFromTree(node, "icon",
                    (String) null);
            superCategory = XmlUtil.getAttributeFromTree(node,
                    ATTR_SUPERCATEGORY, superCategory);
            filePattern = Utils.getAttributeOrTag(node, ATTR_PATTERN,
                    (String) null);
            editHelp = Utils.getAttributeOrTag(node, "edithelp", "");
            String tmp = Utils.getAttributeOrTag(node,
                             PROP_FIELD_FILE_PATTERN, (String) null);

            if (tmp != null) {
                this.fieldPatternNames = new ArrayList<String>();
                this.filePattern = Utils.extractPatternNames(tmp,
                        fieldPatternNames);
                //                System.out.println("File pattern:" + filePattern);
                this.fieldFilePattern = Pattern.compile(this.filePattern);
            }


            wikiTemplate = Utils.getAttributeOrTag(node, ATTR_WIKI,
                    (String) null);

            bubbleTemplate = Utils.getAttributeOrTag(node, ATTR_BUBBLE,
                    (String) null);

            wikiTemplateInner = Utils.getAttributeOrTag(node,
                    ATTR_WIKI_INNER, (String) null);


            defaultChildrenEntries = Utils.getAttributeOrTag(node,
                    TAG_CHILDREN, (String) null);


            List metadataNodes = XmlUtil.findChildren(node, TAG_METADATA);
            for (int i = 0; i < metadataNodes.size(); i++) {
                Element metadataNode = (Element) metadataNodes.get(i);
                requiredMetadata.add(new String[] {
                    XmlUtil.getAttribute(metadataNode, ATTR_ID),
                    XmlUtil.getAttribute(metadataNode, "label",
                                         (String) null) });
            }

            List serviceNodes = XmlUtil.findChildren(node,
                                    Service.TAG_SERVICE);
            for (int i = 0; i < serviceNodes.size(); i++) {
                Element serviceNode = (Element) serviceNodes.get(i);
                services.add(new Service(getRepository(), serviceNode));
            }

            metadataTypes = StringUtil.split(Utils.getAttributeOrTag(node,
                    ATTR_METADATA,
                    EnumeratedMetadataHandler.TYPE_TAG + ","
                    + ContentMetadataHandler.TYPE_THUMBNAIL + ","
                    + ContentMetadataHandler.TYPE_ALIAS), ",", true, true);

            childTypes = StringUtil.split(Utils.getAttributeOrTag(node,
                    ATTR_CHILDTYPES, ""));
            setType(Utils.getAttributeOrTag(node, ATTR_DB_NAME, (type == null)
                    ? ""
                    : type));
            if (getType().indexOf(".") > 0) {
                //            System.err.println("DOT TYPE: " + getType());
            }

            forUser = Utils.getAttributeOrTag(node, ATTR_FORUSER,
                    XmlUtil.getAttributeFromTree(node, ATTR_FORUSER,
                        forUser));

            setProperties(node);
            if ( !Utils.stringDefined(description)) {
                setDescription(Utils.getAttributeOrTag(node,
                        ATTR_DB_DESCRIPTION, getType()));
            }

            String superType = Utils.getAttributeOrTag(node, ATTR_SUPER,
                                   (String) null);
            if (superType != null) {
                parent = getRepository().getTypeHandler(superType);
                if (parent == null) {
                    throw new IllegalArgumentException(
                        "Cannot find parent type:" + superType);
                }
                parent.addChildTypeHandler(this);
            }

            String llf = getTypeProperty("location.format", (String) null);
            if (llf != null) {
                latLonFormat = new DecimalFormat(llf);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }


    }

    /**
     * _more_
     *
     * @return _more_
     */
    private List<String> getMetadataTypes() {
        if (metadataTypes == null) {
            metadataTypes = StringUtil.split(
                EnumeratedMetadataHandler.TYPE_TAG + ","
                + ContentMetadataHandler.TYPE_THUMBNAIL, ",", true, true);
        }

        return metadataTypes;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void getTextCorpus(Entry entry, Appendable sb) throws Exception {}


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getJson(Request request) throws Exception {
        List<String> items = new ArrayList<String>();
        items.add("id");
        items.add(Json.quote(getType()));
        items.add("entryCount");
        int cnt = getEntryUtil().getEntryCount(this);
        items.add("" + cnt);
        items.add("label");
        items.add(Json.quote(getLabel()));
        items.add("isgroup");
        items.add("" + isGroup());

        List<String> cols    = new ArrayList<String>();
        List<Column> columns = getColumns();
        if (columns != null) {
            for (Column column : columns) {
                cols.add(column.getJson(request));
            }
        }
        items.add("columns");
        items.add(Json.list(cols));

        String icon = request.getAbsoluteUrl(
                          getIconUrl(getIconProperty(ICON_FOLDER_CLOSED)));
        items.add("icon");
        items.add(Json.quote(icon));
        items.add("category");
        items.add(Json.quote(getCategory()));

        return Json.map(items);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param items _more_
     * @param attrs _more_
     *
     * @throws Exception _more_
     */
    public void addToJson(Request request, Entry entry, List<String> items,
                          List<String> attrs)
            throws Exception {}


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     * @param topProps _more_
     *
     * @return _more_
     */
    public String getUrlForWiki(Request request, Entry entry, String tag,
                                Hashtable props, List<String> topProps) {
        return null;
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiTemplate(Request request, Entry entry)
            throws Exception {
        if (wikiTemplate != null) {
            return wikiTemplate;
        }
        if (getParent() != null) {
            return getParent().getWikiTemplate(request, entry);
        }

        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getBubbleTemplate(Request request, Entry entry)
            throws Exception {

        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                "content.mapbubble", true);
        if (metadataList != null) {
            //type-1, apply to this-2, name pattern - 3, wiki-4
            Metadata theMetadata = null;
            for (Metadata metadata : metadataList) {
                if (Misc.equals(metadata.getAttr(2), "false")) {
                    if (metadata.getEntryId().equals(entry.getId())) {
                        continue;
                    }
                }
                String types = metadata.getAttr(1);
                if ((types == null) || (types.trim().length() == 0)) {
                    theMetadata = metadata;

                    break;
                }
                for (String type : StringUtil.split(types, ",", true, true)) {
                    if (type.equals("file") && !entry.isGroup()) {
                        theMetadata = metadata;

                        break;
                    }
                    if (type.equals("folder") && entry.isGroup()) {
                        theMetadata = metadata;

                        break;
                    }
                    if (entry.getTypeHandler().isType(type)) {
                        theMetadata = metadata;

                        break;
                    }
                }
            }

            if (theMetadata != null) {
                return theMetadata.getAttr(4);
            }
        }

        if (bubbleTemplate != null) {
            return bubbleTemplate;
        }
        if (getParent() != null) {
            return getParent().getBubbleTemplate(request, entry);
        }

        return null;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiText _more_
     *
     * @return _more_
     */
    public String preProcessWikiText(Request request, Entry entry,
                                     String wikiText) {
        return wikiText;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWikiTemplateInner() {
        return wikiTemplateInner;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getTotalNumberOfValues() {
        int cnt = getNumberOfMyValues();
        if (parent != null) {
            cnt += parent.getTotalNumberOfValues();
        }

        return cnt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getNumberOfMyValues() {
        return 0;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public Object[] getEntryValues(Entry entry) {
        Object[] values = entry.getValues();
        if (values == null) {
            values = this.makeEntryValues(new Hashtable());
            entry.setValues(values);
        }

        return values;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param index _more_
     * @param value _more_
     */
    public void setEntryValue(Entry entry, int index, Object value) {
        getEntryValues(entry)[index] = value;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param index _more_
     *
     * @return _more_
     */
    public Object getEntryValue(Entry entry, int index) {
        return getEntryValues(entry)[index];
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param columnName _more_
     *
     * @return _more_
     */
    public Object getEntryValue(Entry entry, String columnName) {
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getValuesOffset() {
        if (parent != null) {
            return parent.getTotalNumberOfValues();
        }

        return 0;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Comment> getComments(Request request, Entry entry)
            throws Exception {
        return null;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     */
    public int getDefaultQueryLimit(Request request, Entry entry) {
        return DB_MAX_ROWS;
    }


    /**
     * _more_
     *
     * @param tableNames _more_
     */
    public void getTableNames(List<String> tableNames) {
        String tableName = getTableName();
        if ( !tableNames.contains(tableName)) {
            tableNames.add(tableName);
        }
        //        for(TypeHandler child: childrenTypes) {
        //            child.getTableNames(tableNames);
        //        }
        if (getParent() != null) {
            getParent().getTableNames(tableNames);
        }
    }

    /**
     * _more_
     *
     * @param types _more_
     */
    public void getChildTypes(List<String> types) {
        if ( !types.contains(getType())) {
            types.add(getType());
        }
        for (TypeHandler child : childrenTypes) {
            child.getChildTypes(types);
        }
    }

    /**
     * _more_
     *
     * @param child _more_
     */
    public void addChildTypeHandler(TypeHandler child) {
        if ( !childrenTypes.contains(child)) {
            childrenTypes.add(child);
        }
    }


    /**
     *  Get the Parent property.
     *
     *  @return The Parent
     */
    public TypeHandler getParent() {
        return parent;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TypeHandler getTypeHandlerForCopy(Entry entry) throws Exception {
        return this;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param oldEntry _more_
     * @param newEntry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Resource getResourceForCopy(Request request, Entry oldEntry,
                                       Entry newEntry)
            throws Exception {
        Resource newResource = new Resource(oldEntry.getResource());
        if (newResource.isFile()) {
            String newFileName =
                getStorageManager().getFileTail(
                    oldEntry.getResource().getTheFile().getName());
            String newFile =
                getStorageManager().copyToStorage(
                    request, oldEntry.getTypeHandler().getResourceInputStream(
                        oldEntry), getRepository().getGUID() + "_"
                                   + newFileName).toString();
            newResource.setPath(newFile);
        }

        return newResource;
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean addToMap(Request request, Entry entry, MapInfo map)
            throws Exception {
        if (parent != null) {
            return parent.addToMap(request, entry, map);
        }

        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldShowPolygonInMap() {
        return false;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param map _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean addToMapSelector(Request request, Entry entry, MapInfo map)
            throws Exception {
        return true;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryText(Entry entry) {
        return entry.getDescription();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param properties _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getTextForWiki(Request request, Entry entry,
                                 Hashtable properties)
            throws Exception {
        return entry.getDescription();
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void childEntryChanged(Entry entry, boolean isNew)
            throws Exception {}

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void metadataChanged(Request request, Entry entry)
            throws Exception {}

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String getTypePermissionName(String type) {
        if (type.equals(Permission.ACTION_TYPE1)) {
            return "Type specific 1";
        }

        return "Type specific 2";
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param sb _more_
     */
    public void handleNoEntriesHtml(Request request, Entry entry,
                                    Appendable sb) {
        if ( !Utils.stringDefined(entry.getDescription())
                && getType().equals(TYPE_GROUP)) {
            Utils.append(sb,
                         HtmlUtils.tag(HtmlUtils.TAG_I, "",
                                       msg(LABEL_EMPTY_FOLDER)));
        }
    }


    /**
     * _more_
     *
     * @param tableName _more_
     *
     * @return _more_
     */
    public boolean shouldExportTable(String tableName) {
        return true;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void initAfterDatabaseImport() throws Exception {}


    /**
     * _more_
     */
    public void clearCache() {
        columnEnumValues = new Hashtable<String, HashSet>();
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream getResourceInputStream(Entry entry) throws Exception {
        return new BufferedInputStream(
            getStorageManager().getFileInputStream(getFileForEntry(entry)));
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        if (parent != null) {
            return parent.getHtmlDisplay(request, entry);
        }

        return null;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param wikiTemplate _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getInnerWikiContent(Request request, Entry entry,
                                      String wikiTemplate)
            throws Exception {
        return null;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryAccess(Request request, Entry entry)
            throws Exception {
        return new Result("Error",
                          new StringBuilder("Entry access not defined"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param action _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryActionUrl(Request request, Entry entry,
                                    String action)
            throws Exception {
        return request.makeUrl(getRepository().URL_ENTRY_ACTION, ARG_ENTRYID,
                               entry.getId(), ARG_ACTION, action);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        if (parent != null) {
            return parent.processEntryAction(request, entry);
        }

        return new Result("Error", new StringBuilder("Unknown entry action"));
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param tabTitles _more_
     * @param tabContents _more_
     */
    public void addToInformationTabs(Request request, Entry entry,
                                     List<String> tabTitles,
                                     List<String> tabContents) {}


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public boolean isDefaultHtmlOutput(Request request) {
        return Misc.equals(
            OutputHandler.OUTPUT_HTML.getId(),
            request.getString(ARG_OUTPUT, OutputHandler.OUTPUT_HTML.getId()));
    }



    /**
     * _more_
     *
     * @param request The request
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry group,
                                 List<Entry> subGroups, List<Entry> entries)
            throws Exception {
        if (parent != null) {
            return parent.getHtmlDisplay(request, group, subGroups, entries);
        }

        return null;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getInlineHtml(Request request, Entry entry)
            throws Exception {
        if (parent != null) {
            return parent.getInlineHtml(request, entry);
        }

        return null;
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public boolean canBeCreatedBy(Request request) {
        if (parent != null) {
            return parent.canBeCreatedBy(request);
        }

        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean adminOnly() {
        if (parent != null) {
            return parent.adminOnly();
        }

        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        if (parent != null) {
            return parent.isSynthType();
        }

        return false;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param mainEntry _more_
     * @param ancestor _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getSynthIds(Request request, Entry mainEntry,
                                    Entry ancestor, String synthId)
            throws Exception {
        if (parent != null) {
            return parent.getSynthIds(request, mainEntry, ancestor, synthId);
        }

        throw new IllegalArgumentException(
            "getSynthIds  not implemented for type:" + getType()
            + " in class:" + getClass().getName());
    }

    /**
     * _more_
     *
     * @param request The request
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        if (parent != null) {
            return parent.makeSynthEntry(request, parentEntry, id);
        }

        throw new IllegalArgumentException(
            "makeSynthEntry  not implemented: type=" + getType() + " class:"
            + getClass().getName());
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getSynthTopLevelEntry() throws Exception {
        if (synthTopLevelEntry == null) {
            synthTopLevelEntry = doMakeSynthTopLevelEntry();
        }

        return synthTopLevelEntry;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry doMakeSynthTopLevelEntry() throws Exception {
        Entry parentEntry = new Entry(this, true);
        parentEntry.setUser(getUserManager().getLocalFileUser());
        //Add metadata to hide the menubar

        getMetadataManager().addMetadata(
            parentEntry,
            new Metadata(
                getRepository().getGUID(), parentEntry.getId(),
                ContentMetadataHandler.TYPE_PAGESTYLE, true, "", "true", "",
                "", ""));

        parentEntry.putTransientProperty("showinbreadcrumbs", "false");
        parentEntry.setName(getLabel());
        parentEntry.setId(ID_PREFIX_SYNTH + getType());
        parentEntry.setParentEntry(getEntryManager().getRootEntry());

        return parentEntry;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param parentEntry _more_
     * @param entryNames _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry parentEntry,
                                List<String> entryNames)
            throws Exception {
        if (parent != null) {
            return parent.makeSynthEntry(request, parentEntry, entryNames);
        }

        throw new IllegalArgumentException("makeSynthEntry  not implemented:"
                                           + getType() + " "
                                           + getClass().getName());
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getTypeProperty(String name, boolean dflt) {
        return getProperty((Entry) null, name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getTypeProperty(String name, int dflt) {
        return getProperty((Entry) null, name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    protected void setTypeProperty(String name, String value) {
        properties.put(name, value);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getTypeProperty(String name, String dflt) {
        return getProperty((Entry) null, name, dflt);
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(Entry entry, String name) {
        return getProperty(entry, name, null);
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(Entry entry, String name, String dflt) {
        String result = (String) properties.get(name);
        if (result != null) {
            return result;
        }
        if (parent != null) {
            return parent.getProperty(entry, name, dflt);
        }

        return dflt;
    }

    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getProperty(Entry entry, String name, int dflt) {
        String s = getProperty(entry, name, null);
        if (s == null) {
            return dflt;
        }

        return Integer.parseInt(s);
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(Entry entry, String name, boolean dflt) {
        String s = getProperty(entry, name, Boolean.toString(dflt));

        return s.equals("true");
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void putProperty(String name, String value) {
        properties.put(name, value);
        if (parent != null) {
            //            parent.putProperty(name, value);
        }
    }




    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getFormLabel(Entry entry, String arg, String dflt) {
        return getProperty(entry, "form." + arg + ".label", dflt);
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param arg _more_
     *
     * @return _more_
     */
    public boolean okToShowInForm(Entry entry, String arg) {
        return okToShowInForm(entry, arg, true);
    }

    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean okToShowInForm(Entry entry, String arg, boolean dflt) {
        String key   = "form." + arg + ".show";
        String value = getProperty(entry, key, "" + dflt);

        return value.equals("true");
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean okToShowInHtml(Entry entry, String arg, boolean dflt) {
        String key   = "html." + arg + ".show";
        String value = getProperty(entry, key, "" + dflt);
        /*
        if(arg.equals("owner")) {
            System.err.println("Type:" + type);
            System.err.println("Props:" + properties);
            System.err.println("Owner:" + key + " " + value);
            System.err.println("Props:" + properties);
        }
        */

        return value.equals("true");
    }



    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param arg _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getFormDefault(Entry entry, String arg, String dflt) {
        String prop = getProperty(entry, "form." + arg + ".default");
        if (prop == null) {
            return dflt;
        }

        return prop;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Entry(id, this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean returnToEditForm() {
        if (parent != null) {
            return parent.returnToEditForm();
        }

        return false;
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param node _more_
     * @param files _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromXml(Request request, Entry entry,
                                       Element node,
                                       Hashtable<String, File> files)
            throws Exception {
        if (parent != null) {
            parent.initializeEntryFromXml(request, entry, node, files);
        }


    }


    /**
     * _more_
     *
     * @param s _more_
     * @param idList _more_
     *
     * @return _more_
     */
    public String convertIdsFromImport(String s, List<String[]> idList) {
        if ( !Utils.stringDefined(s)) {
            return s;
        }
        String pattern = "[^-]+-[^-]+-[^-]+-[^-]+-[^-]+";
        for (String[] tuple : idList) {
            String oldId = tuple[0];
            if ((oldId == null) || (oldId.length() == 0)) {
                continue;
            }
            String newId = tuple[1];
            //Make sure we only replace GUIDs
            s = s.replaceAll(oldId, newId);
        }

        return s;
    }

    /**
     * _more_
     *
     * @param newEntry _more_
     * @param idList _more_
     *
     * @return _more_
     */
    public boolean convertIdsFromImport(Entry newEntry,
                                        List<String[]> idList) {
        String desc = newEntry.getDescription();
        if ((desc != null) && (desc.length() > 0)) {
            String converted = convertIdsFromImport(desc, idList);
            if ( !converted.equals(desc)) {
                newEntry.setDescription(converted);

                return true;
            }
        }

        return false;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param fileWriter _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryNode(Request request, Entry entry,
                               FileWriter fileWriter, Element node)
            throws Exception {
        if (parent != null) {
            parent.addToEntryNode(request, entry, fileWriter, node);
        }
    }



    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !(obj.getClass().equals(getClass()))) {
            return false;
        }

        return Misc.equals(type, ((TypeHandler) obj).getType());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getNodeType() {
        if (parent != null) {
            return parent.getNodeType();
        }

        return NODETYPE_ENTRY;
    }



    /**
     * _more_
     *
     * @param newType _more_
     *
     * @return _more_
     */
    public boolean canChangeTo(TypeHandler newType) {
        return true;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public Entry changeType(Request request, Entry entry) throws Exception {
        //Recreate the entry. This will fill in any extra entry type db tables
        entry = getEntryManager().getEntry(request, entry.getId());
        //Then initialize it, e.g., point data type will read the file and set the entry values, etc.
        initializeNewEntry(request, entry, false);
        //        Object[] values =  getEntryValues(entry);
        //        System.err.println("type:" + this);
        //        for(int i=0;i<values.length;i++) {
        //            System.err.println("value[" + i +"] = " + values[i]);
        //        }
        //Now store the changes
        getEntryManager().updateEntry(request, entry);

        return entry;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getType() {
        return type;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isType(String type) {
        if (this.type.equals(type)) {
            return true;
        }
        if (parent != null) {
            return parent.isType(type);
        }

        return false;
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public final Entry createEntryFromDatabase(ResultSet results)
            throws Exception {
        return createEntryFromDatabase(results, false);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param parent _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromForm(Request request, Entry entry,
                                        Entry parent, boolean newEntry)
            throws Exception {
        if (this.parent != null) {
            this.parent.initializeEntryFromForm(request, entry, parent,
                    newEntry);
        }






    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param firstCall _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromHarvester(Request request, Entry entry,
                                             boolean firstCall)
            throws Exception {
        if (firstCall) {
            initializeNewEntry(request, entry, false);
        }
        if (this.parent != null) {
            this.parent.initializeEntryFromHarvester(request, entry, false);
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntryFromDatabase(Entry entry) throws Exception {
        if (parent != null) {
            parent.initializeEntryFromDatabase(entry);
        }
    }

    /**
     * This gets called after the entry has been created and everything has been stored into the database
     *
     * @param request The request
     * @param entry _more_
     * @param fromImport _more_
     */
    public void doFinalEntryInitialization(Request request, Entry entry,
                                           boolean fromImport) {
        //Clear the column value cache?

        if (fromImport) {
            return;
        }
        if (request == null) {
            return;
        }
        try {
            //Check if there is a default set of children entries
            if (defaultChildrenEntries != null) {
                Element root = XmlUtil.getRoot(defaultChildrenEntries);
                List<Entry> newEntries =
                    getEntryManager().processEntryXml(request, root, entry,
                        new Hashtable<String, File>());
            }


            if (requiredMetadata.size() == 0) {
                return;
            }
            Hashtable<String, Metadata> existingMetadata =
                new Hashtable<String, Metadata>();
            List<Metadata> metadataList = new ArrayList<Metadata>();
            for (String[] idLabel : requiredMetadata) {
                MetadataHandler handler =
                    getMetadataManager().findMetadataHandler(idLabel[0]);
                if (handler != null) {
                    handler.handleForm(request, entry,
                                       getRepository().getGUID(), "",
                                       existingMetadata, metadataList, true);

                }
            }
            for (Metadata metadata : metadataList) {
                getMetadataManager().insertMetadata(metadata);
            }


            //            getEntryManager().setBoundsFromChildren(request, entry.getParentEntry());
            //            getEntryManager().setTimeFromChildren(request, entry.getParentEntry(), null);

        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean anySuperTypesOfThisType() {
        Class       myClass = getClass();
        TypeHandler handler = this.parent;
        while (handler != null) {
            //            System.err.println("parent:" + handler.getClass().getName());
            if (handler.getClass().isAssignableFrom(myClass)) {
                //                System.err.println("Have super class");
                return true;
            }
            handler = parent;
        }

        //        System.err.println("Don't Have super class");
        return false;
    }


    /**
     * Does this type match the file being harvester
     *
     * @param fullPath _more_
     * @param name _more_
     *
     * @return is this one of my files
     */
    public boolean canHandleResource(String fullPath, String name) {
        if (filePattern == null) {
            return false;
        }
        //        System.err.println("Pattern:" + filePattern + " name:" + name );

        //If the pattern has file delimiters then use the whole path
        if (filePattern.indexOf("/") >= 0) {
            if (fullPath.matches(filePattern)) {
                return true;
            }
        } else {
            if (fieldFilePattern != null) {
                //                System.err.println ("checking pattern:" + filePattern +" name :" + name);
            }

            //Else, just use the name
            if (name.matches(filePattern)) {
                return true;
            } else if (name.toLowerCase().matches(filePattern)) {
                return true;
            } else {
                //                System.err.println ("no match");
            }
        }

        return false;
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     */
    public String getDefaultEntryName(String path) {
        return IOUtil.getFileTail(path);
    }

    /**
     * _more_
     *
     * @param results _more_
     * @param abbreviated _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public final Entry createEntryFromDatabase(ResultSet results,
            boolean abbreviated)
            throws Exception {
        if (parent != null) {}

        //id,type,name,desc,group, user,file,createdata,fromdate,todate
        int             col        = 3;
        String          id         = results.getString(1);
        Entry           entry      = createEntry(id);
        DatabaseManager dbm        = getDatabaseManager();
        Date            createDate = null;

        String          entryId    = results.getString(col++);
        String          name       = results.getString(col++);
        String          parentId   = results.getString(col++);

        Entry           parent = getEntryManager().findGroup(null, parentId);
        entry.initEntry(entryId, name, parent, getUserManager()
            .findUser(results
                .getString(col++), true), new Resource(getStorageManager()
                .resourceFromDB(results.getString(col++)), results
                .getString(col++), results.getString(col++), results
                .getLong(col++)), results.getString(col++), (createDate =
                    dbm.getDate(results, col++)).getTime(), dbm
                        .getDate(results, col++, createDate).getTime(), dbm
                        .getDate(results, col++).getTime(), dbm
                        .getDate(results, col++).getTime(), null);
        entry.setSouth(results.getDouble(col++));
        entry.setNorth(results.getDouble(col++));
        entry.setEast(results.getDouble(col++));
        entry.setWest(results.getDouble(col++));
        entry.setAltitudeTop(results.getDouble(col++));
        entry.setAltitudeBottom(results.getDouble(col++));

        if ( !abbreviated) {
            initializeEntryFromDatabase(entry);
        }

        return entry;
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param root _more_
     * @param extraXml _more_
     * @param metadataType _more_
     */
    public void addMetadataToXml(Entry entry, Element root,
                                 Appendable extraXml, String metadataType) {}

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param html _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String processDisplayTemplate(Request request, Entry entry,
                                         String html)
            throws Exception {
        String name      = getEntryName(entry);
        String shortName = (name.length() > 40)
                           ? name.substring(0, 39) + "..."
                           : name;
        html = html.replace("${" + ARG_NAME + "}", name);
        html = html.replace("${name.short}", shortName);
        html = html.replace("${" + ARG_LABEL + "}", entry.getLabel());
        html = html.replace("${" + ARG_DESCRIPTION + "}",
                            entry.getDescription());
        html = html.replace("${" + ARG_CREATEDATE + "}",
                            getDateHandler().formatDate(request, entry,
                                entry.getCreateDate()));
        html = html.replace("${" + ARG_CHANGEDATE + "}",
                            getDateHandler().formatDate(request, entry,
                                entry.getChangeDate()));
        html = html.replace("${" + ARG_FROMDATE + "}",
                            getDateHandler().formatDate(request, entry,
                                entry.getStartDate()));
        html = html.replace("${" + ARG_TODATE + "}",
                            getDateHandler().formatDate(request, entry,
                                entry.getEndDate()));
        html = html.replace("${" + ARG_CREATOR + "}",
                            entry.getUser().getLabel());

        return html;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param request The request
     * @param showDescription _more_
     * @param showResource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuilder getEntryContent(Request request, Entry entry,
                                         boolean showDescription,
                                         boolean showResource)
            throws Exception {

        StringBuilder sb     = new StringBuilder();
        OutputType    output = request.getOutput();
        if (true) {
            if (displayTemplatePath != null) {
                String html =
                    getRepository().getResource(displayTemplatePath);

                return new StringBuilder(processDisplayTemplate(request,
                        entry, html));
            }
            if (request.get(WikiConstants.ATTR_SHOWTITLE, true)) {
                HtmlUtils.sectionHeader(
                    sb, getPageHandler().getEntryHref(request, entry));
            }
            sb.append(HtmlUtils.formTable());
            sb.append(getInnerEntryContent(entry, request, null, output,
                                           showDescription, showResource,
                                           true));
            sb.append(HtmlUtils.formTableClose());
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}

        return sb;

    }


    /**
     * _more_
     *
     * @param columnName _more_
     *
     * @return _more_
     */
    public Column findColumn(String columnName) {
        return null;
    }


    /**
     * _more_
     *
     * @param entryNode _more_
     */
    protected void setProperties(Element entryNode) {
        //        boolean debug = type.equals("type_fred_series");
        boolean debug = false;

        if (debug) {
            System.err.println("set Properties");
        }
        List propertyNodes = XmlUtil.findChildren(entryNode, TAG_PROPERTY);

        for (int propIdx = 0; propIdx < propertyNodes.size(); propIdx++) {
            Element propertyNode = (Element) propertyNodes.get(propIdx);
            if (XmlUtil.hasAttribute(propertyNode, ATTR_VALUE)) {
                if (debug) {
                    System.err.println(
                        "\t" + XmlUtil.getAttribute(propertyNode, ATTR_NAME)
                        + "="
                        + XmlUtil.getAttribute(propertyNode, ATTR_NAME));
                }
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getAttribute(propertyNode, ATTR_VALUE));
            } else {
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getChildText(propertyNode));
            }
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param services _more_
     *
     */
    public void getServiceInfos(Request request, Entry entry,
                                List<ServiceInfo> services) {
        for (OutputHandler handler : getRepository().getOutputHandlers()) {
            handler.getServiceInfos(request, entry, services);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param harvester _more_
     * @param args _more_
     * @param sb _more_
     * @param files _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean processCommandView(org.ramadda.repository.harvester
            .CommandHarvester.CommandRequest request, Entry entry,
                org.ramadda.repository.harvester.CommandHarvester harvester,
                List<String> args, Appendable sb, List<FileInfo> files)
            throws Exception {
        StringBuilder html = new StringBuilder();
        String url = request.getRequest().getAbsoluteUrl(
                         getRepository().getHtmlOutputHandler().getImageUrl(
                             request.getRequest(), entry, false));
        html.append(harvester.getEntryHeader(request, entry));
        if (isImage(entry)) {
            html.append(HtmlUtils.img(url, ""));
        }
        html.append(entry.getDescription());
        sb.append(html);

        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isGroup() {
        return false;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param request The request
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, Entry entry, List<Link> links)
            throws Exception {

        if (parent != null) {
            parent.getEntryLinks(request, entry, links);

            return;
        }


        boolean isGroup = entry.isGroup();
        boolean canDoNew = isGroup
                           && getAccessManager().canDoAction(request, entry,
                               Permission.ACTION_NEW);

        if (canDoNew) {
            links.add(
                new Link(
                    request.makeUrl(
                        getRepository().URL_ENTRY_FORM, ARG_GROUP,
                        entry.getId(), ARG_TYPE,
                        TYPE_GROUP), ICON_FOLDER_ADD, "New Folder",
                                     OutputType.TYPE_FILE));
            links.add(
                new Link(
                    request.makeUrl(
                        getRepository().URL_ENTRY_FORM, ARG_GROUP,
                        entry.getId(), ARG_TYPE, TYPE_FILE), ICON_ENTRY_ADD,
                            "New File", OutputType.TYPE_FILE));
            links.add(new Link(request.makeUrl(getRepository().URL_ENTRY_NEW,
                    ARG_GROUP, entry.getId()), ICON_NEW, LABEL_NEW_ENTRY,
                        OutputType.TYPE_FILE | OutputType.TYPE_TOOLBAR));
            links.add(makeHRLink(OutputType.TYPE_FILE));

        }


        //We don't actually prevent an export - just don't show the link in the menu
        if ( !request.getUser().getAnonymous()) {
            links.add(
                new Link(
                    HtmlUtils.url(
                        getRepository().URL_ENTRY_EXPORT.toString() + "/"
                        + IOUtil.stripExtension(
                            Entry.encodeName(
                                getEntryName(
                                    entry))) + ".zip", new String[] {
                                        ARG_ENTRYID,
                                        entry.getId() }), ICON_EXPORT,
                                        "Export", OutputType.TYPE_FILE));


        }

        //Add an import link if they have the right privileges
        if (canDoNew) {
            links.add(
                new Link(
                    request.makeUrl(
                        getRepository().URL_ENTRY_IMPORT, ARG_GROUP,
                        entry.getId()), ICON_IMPORT, "Import",
                                        OutputType.TYPE_FILE));
            links.add(makeHRLink(OutputType.TYPE_FILE));
        }



        if (canDoNew) {


            List<String> pastTypes =
                (List<String>) getSessionManager().getSessionProperty(
                    request, ARG_TYPE);
            HashSet seen   = new HashSet();
            boolean didone = addTypes(request, entry, links, childTypes,
                                      seen);
            didone |= addTypes(request, entry, links, pastTypes, seen);


            didone |= addTypesFromEntries(request, entry, links,
                                          entry.getSubGroups(), seen);
            didone |= addTypesFromEntries(request, entry, links,
                                          entry.getSubEntries(), seen);




            if (didone) {
                links.add(makeHRLink(OutputType.TYPE_FILE));
            }
        }



        links.add(
            new Link(
                HtmlUtils.url(
                    getRepository().URL_ENTRY_LINKS.toString(),
                    new String[] { ARG_ENTRYID,
                                   entry
                                   .getId() }), "/icons/application-detail.png",
                                       "All Actions", OutputType.TYPE_FILE));

        links.add(makeHRLink(OutputType.TYPE_FILE));


        if ( !canDoNew && isGroup
                && getAccessManager().canDoAction(request, entry,
                    Permission.ACTION_UPLOAD)) {
            links.add(
                new Link(
                    request.makeUrl(
                        getRepository().URL_ENTRY_UPLOAD, ARG_GROUP,
                        entry.getId()), ICON_UPLOAD, "Upload a File",
                                        OutputType.TYPE_FILE
                                        | OutputType.TYPE_TOOLBAR));
        }


        if (getAccessManager().canEditEntry(request, entry)) {
            links.add(
                new Link(
                    request.entryUrl(getRepository().URL_ENTRY_FORM, entry),
                    ICON_EDIT, "Edit " + LABEL_ENTRY,
                    OutputType.TYPE_EDIT /* | OutputType.TYPE_TOOLBAR*/));

            //NOTE: Don't add the direct link because the auth token is added
            if (false && getEntryManager().isAnonymousUpload(entry)) {
                links.add(
                    new Link(
                        request.entryUrl(
                            getRepository().URL_ENTRY_CHANGE, entry,
                            ARG_JUSTPUBLISH, "true"), ICON_PUBLISH,
                                "Make " + LABEL_ENTRY + " Public",
                                OutputType.TYPE_EDIT
                /*| OutputType.TYPE_TOOLBAR*/
                ));
            }

            links.add(
                new Link(
                    request.entryUrl(
                        getMetadataManager().URL_METADATA_FORM,
                        entry), ICON_METADATA_EDIT, "Edit Properties",
                                OutputType.TYPE_EDIT));

            if (getMetadataTypes().size() > 0) {
                links.add(makeHRLink(OutputType.TYPE_EDIT));
            }

            links.add(
                new Link(
                    request.entryUrl(
                        getMetadataManager().URL_METADATA_ADDFORM,
                        entry), ICON_METADATA_ADD, "Add Property...",
                                OutputType.TYPE_EDIT));

            if (getMetadataTypes().size() > 0) {
                for (String metadataType : getMetadataTypes()) {
                    MetadataType type =
                        getMetadataManager().findType(metadataType);
                    links.add(
                        new Link(
                            request.entryUrl(
                                getMetadataManager().URL_METADATA_ADDFORM,
                                entry, ARG_METADATA_TYPE,
                                metadataType), ICON_METADATA_ADD,
                                    msg("Add") + " " + type.getName(),
                                    OutputType.TYPE_EDIT));
                }
                links.add(makeHRLink(OutputType.TYPE_EDIT));
            }



            if (getAccessManager().canSetAccess(request, entry)) {
                links.add(
                    new Link(
                        request.entryUrl(
                            getRepository().URL_ACCESS_FORM,
                            entry), ICON_ACCESS, "Access",
                                    OutputType.TYPE_EDIT));
            }


            links.add(
                new Link(
                    request.entryUrl(
                        getRepository().URL_ENTRY_EXTEDIT,
                        entry), "/icons/sitemap.png", "Extended Edit",
                                OutputType.TYPE_EDIT));

        }

        if (getAccessManager().canDoAction(request, entry,
                                           Permission.ACTION_DELETE)) {
            links.add(
                new Link(
                    request.entryUrl(
                        getRepository().URL_ENTRY_DELETE,
                        entry), ICON_DELETE, "Delete " + LABEL_ENTRY,
                                OutputType.TYPE_EDIT
            /*| OutputType.TYPE_TOOLBAR*/
            ));

        }

        Link downloadLink = getEntryDownloadLink(request, entry);
        if (downloadLink != null) {
            links.add(downloadLink);
        }


        if (getRepository().getCommentsEnabled()) {
            if (getRepository().isReadOnly()) {
                links.add(
                    new Link(
                        request.entryUrl(
                            getRepository().URL_COMMENTS_SHOW,
                            entry), ICON_COMMENTS, "View Comments",
                                    OutputType.TYPE_VIEW));
            } else {
                links.add(
                    new Link(
                        request.entryUrl(
                            getRepository().URL_COMMENTS_SHOW,
                            entry), ICON_COMMENTS, "Add/View Comments",
                                    OutputType.TYPE_TOOLBAR));
            }
        }
        if ((request.getUser() != null)
                && !request.getUser().getAnonymous()) {
            links.add(
                new Link(
                    request.entryUrlWithArg(
                        getRepository().URL_ENTRY_COPY, entry,
                        ARG_FROM), ICON_MOVE, "Move/Copy/Link",
                                   OutputType.TYPE_EDIT));
        }
    }





    /**
     * _more_
     *
     * @param mask _more_
     *
     * @return _more_
     */
    private Link makeHRLink(int mask) {
        Link hr = new Link(true);
        hr.setLinkType(mask);

        return hr;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     * @param entries _more_
     * @param seen _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean addTypesFromEntries(Request request, Entry entry,
                                        List<Link> links,
                                        List<Entry> entries,
                                        HashSet<String> seen)
            throws Exception {
        if (entries == null) {
            return false;
        }
        List<String> types = new ArrayList<String>();
        for (Entry e : entries) {
            String type = e.getTypeHandler().getType();
            if ( !seen.contains(type)) {
                types.add(type);
            }
        }

        return addTypes(request, entry, links, types, seen);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param links _more_
     * @param types _more_
     * @param seen _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean addTypes(Request request, Entry entry, List<Link> links,
                             List<String> types, HashSet<String> seen)
            throws Exception {
        if (types == null) {
            return false;
        }
        boolean didone = false;
        for (String type : types) {
            if (type.equals(TYPE_FILE) || type.equals(TYPE_GROUP)) {
                continue;
            }
            if (seen.contains(type)) {
                continue;
            }
            seen.add(type);
            didone = true;
            TypeHandler typeHandler = getRepository().getTypeHandler(type);
            String      icon        = typeHandler.getIconProperty(null);
            if (icon == null) {
                icon = ICON_ENTRY_ADD;
            }
            links.add(
                new Link(
                    request.makeUrl(
                        getRepository().URL_ENTRY_FORM, ARG_GROUP,
                        entry.getId(), ARG_TYPE, type), icon,
                            "New " + typeHandler.getDescription(),
                            OutputType.TYPE_FILE));
        }

        return didone;
    }



    /**
     * _more_
     *
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getIconProperty(String dflt) {
        String icon = iconPath;
        if (icon == null) {
            icon = getTypeProperty("icon", (String) null);
            if (icon == null) {
                icon = dflt;
            }
        }

        return icon;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTypeIconUrl() {
        String icon = getIconProperty(null);
        if (icon != null) {
            icon = getIconUrl(icon);
        }

        return icon;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        if (parent != null) {
            return parent.canDownload(request, entry);
        }

        if ( !entry.isFile()) {
            return false;
        }

        return true;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getPathForEntry(Request request, Entry entry)
            throws Exception {
        Resource resource = entry.getResource();
        String   path     = Utils.normalizeTemplateUrl(resource.getPath());

        return path;
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public File getFileForEntry(Entry entry) {
        return entry.getResource().getTheFile();
    }



    /** _more_ */
    private HashSet seenIt = new HashSet();


    /**
     * _more_
     *
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Link getEntryDownloadLink(Request request, Entry entry)
            throws Exception {
        return getEntryDownloadLink(request, entry, "Download File");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param label _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Link getEntryDownloadLink(Request request, Entry entry,
                                     String label)
            throws Exception {
        if ( !getAccessManager().canDownload(request, entry)) {
            /*
            if(!entry.isGroup() && !seenIt.contains(entry.getId())) {
                seenIt.add(entry.getId());
                getLogManager().logInfoAndPrint("cannot download:" + entry);
                Resource resource = entry.getResource();
                getLogManager().logInfoAndPrint("\tresource:" + resource +
                " type:" + resource.getType() +
                " exists:" +  resource.getTheFile().exists() +
                " the file:" + resource.getTheFile());

            }
            */
            return null;
        }
        String size = " ("
                      + formatFileLength(entry.getResource().getFileSize())
                      + ")";

        String fileTail = getStorageManager().getFileTail(entry);
        fileTail = HtmlUtils.urlEncodeExceptSpace(fileTail);

        return new Link(getEntryManager().getEntryResourceUrl(request,
                entry), ICON_FETCH, msg(label) + size,
                        OutputType.TYPE_FILE | OutputType.TYPE_IMPORTANT);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param request The request
     * @param typeHandler _more_
     * @param output _more_
     * @param showDescription _more_
     * @param showResource _more_
     * @param linkToDownload _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuilder getInnerEntryContent(Entry entry, Request request,
            TypeHandler typeHandler, OutputType output,
            boolean showDescription, boolean showResource,
            boolean linkToDownload)
            throws Exception {

        if (typeHandler == null) {
            typeHandler = this;
        }
        if (parent != null) {
            return parent.getInnerEntryContent(entry, request, typeHandler,
                    output, showDescription, showResource, linkToDownload);
        }

        boolean showDate = typeHandler.okToShowInHtml(entry, ARG_DATE, true);
        boolean showCreateDate = showDate
                                 && typeHandler.okToShowInHtml(entry,
                                     "createdate", true);

        boolean entryIsImage = isImage(entry);
        boolean showImage    = false;
        if (showResource && entryIsImage) {
            if (entry.getResource().isFile()
                    && getAccessManager().canDownload(request, entry)) {
                showImage = true;
            } else if (entry.getResource().isUrl()) {
                showImage = true;
            }
        }


        StringBuilder sb = new StringBuilder();
        if (true || output.equals(OutputHandler.OUTPUT_HTML)) {
            OutputHandler outputHandler =
                getRepository().getOutputHandler(request);
            String nextPrev = StringUtil.join("",
                                  outputHandler.getNextPrevLinks(request,
                                      entry, output));

            if (showDescription) {
                String desc = entry.getDescription();
                if ((desc != null) && (desc.length() > 0)
                        && ( !isWikiText(desc))) {
                    sb.append(
                        formEntry(
                            request, msgLabel("Description"),
                            getEntryManager().getEntryText(
                                request, entry, desc)));
                }
            }
            //            sb.append(
            //                "<tr><td width=20%><div style=\"height:1px;\"></div></td><td width=75%></td></tr>");



            String createdDisplayMode =
                getPageHandler().getCreatedDisplayMode();
            boolean showCreated = true;
            if (createdDisplayMode.equals("none")) {
                showCreated = false;
            } else if (createdDisplayMode.equals("admin")) {
                showCreated = request.getUser().getAdmin();
            } else if (createdDisplayMode.equals("user")) {
                showCreated = !request.isAnonymous();
            } else if (createdDisplayMode.equals("all")) {
                showCreated = true;
            } else {
                showCreated = false;
            }



            /**
             * boolean isPdf = entry.getResource().getPath().endsWith(".pdf");
             * if(showResource && isPdf) {
             *   if(getAccessManager().canDownload(request, entry)) {
             *       String getFileUrl = getEntryResourceUrl(request, entry);
             *       String embed = HtmlUtils.tag(HtmlUtils.TAG_OBJECT,
             *                                    HtmlUtils.attrs(
             *                                                    HtmlUtils.ATTR_TYPE,"application/pdf",
             *                                                    HtmlUtils.ATTR_SRC, getFileUrl,
             *                                                    HtmlUtils.ATTR_WIDTH, "600",
             *                                                    HtmlUtils.ATTR_HEIGHT, "1000"),
             *                                    msg("PDF view not supported"));
             *       sb.append(HtmlUtils.col(embed, " colspan=2 "));
             *   }
             * }
             */

            if (showResource && entryIsImage) {
                String width = "600";
                if (request.isMobile()) {
                    width = "250";
                }
                String img    = null;
                String imgUrl = null;
                if (entry.getResource().isFile()
                        && getAccessManager().canDownload(request, entry)) {
                    imgUrl = getEntryResourceUrl(request, entry);
                    img    = HtmlUtils.img(imgUrl, "", "width=" + width);
                } else if (entry.getResource().isUrl()) {
                    try {
                        imgUrl = getPathForEntry(request, entry);
                        img    = HtmlUtils.img(imgUrl, "", "width=" + width);
                    } catch (Exception exc) {
                        sb.append("Error getting path:" + entry.getResource()
                                  + " " + exc);
                    }
                    //                    imgUrl = entry.getResource().getPath();

                }
                if (img != null) {
                    String outer = HtmlUtils.href(imgUrl, img,
                                       HtmlUtils.cssClass("popup_image"));
                    //                    sb.append(HtmlUtils.col(img, " colspan=2 "));
                    sb.append(HtmlUtils.col(outer, " colspan=2 "));
                    getWikiManager().addImagePopupJS(request, null, sb,
                            new Hashtable());
                }
            }


            Resource resource      = entry.getResource();
            String   resourceLink  = resource.getPath();

            String   resourceLabel = null;
            if (resource.isUrl()) {
                resourceLabel = "URL";
            } else if (resource.isFileType()) {
                resourceLabel = "File";
            } else {
                resourceLabel = "Resource (" + resource.getType() + ")";
            }
            resourceLabel = msgLabel(resourceLabel);
            if (resourceLink.length() > 0) {
                if (entry.getResource().isUrl()) {
                    try {
                        resourceLink = getPathForEntry(request, entry);
                        resourceLink = HtmlUtils.href(resourceLink,
                                resourceLink);
                    } catch (Exception exc) {
                        sb.append("Error:" + exc);
                    }
                } else if (entry.getResource().isFile()) {
                    resourceLink =
                        getStorageManager().getFileTail(resourceLink);
                    //Not sure why we were doing this but it screws up chinese characters
                    //                    resourceLink =
                    //                        HtmlUtils.urlEncodeExceptSpace(resourceLink);
                    resourceLabel = msgLabel("File");
                    if (getAccessManager().canDownload(request, entry)) {
                        resourceLink = resourceLink + HtmlUtils.space(2)
                                       + HtmlUtils.href(
                                           getEntryResourceUrl(
                                               request, entry), HtmlUtils.img(
                                                   getIconUrl(ICON_DOWNLOAD),
                                                       msg("Download"), ""));

                    } else {
                        resourceLink = resourceLink + HtmlUtils.space(2)
                                       + "(" + msg("restricted") + ")";
                    }
                }
                if (entry.getResource().getFileSize() > 0) {
                    resourceLink =
                        resourceLink + HtmlUtils.space(2)
                        + formatFileLength(entry.getResource().getFileSize());
                }
                if (showImage) {
                    /*                    String nextPrev = HtmlUtils.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                                                     entry, ARG_PREVIOUS,
                                                                     "true"), getIconUrl(ICON_LEFT),
                                                    msg("View Previous")) +
                        HtmlUtils.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                                       entry, ARG_NEXT,
                                                       "true"), getIconUrl(ICON_LEFT),
                                                       msg("View Next"));*/
                    /* remove the nextPrev buttons - who uses them?
                resourceLink = nextPrev + HtmlUtils.space(1)
                               + resourceLink;
                */

                }


                sb.append(formEntry(request, resourceLabel, resourceLink));

            }
            //Only show the created by and type when the user is logged in
            if ( !showImage) {
                if (typeHandler.okToShowInHtml(entry, ARG_TYPE, true)) {
                    sb.append(formEntry(request, msgLabel("Kind"),
                                        getFileTypeDescription(request,
                                            entry)));
                }
            }

            if ( !request.isAnonymous()) {
                if (showCreateDate) {
                    sb.append(formEntry(request, msgLabel("Created"),
                                        getDateHandler().formatDate(request,
                                            entry, entry.getCreateDate())));

                    if (entry.getCreateDate() != entry.getChangeDate()) {
                        sb.append(
                            formEntry(
                                request, msgLabel("Modified"),
                                getDateHandler().formatDate(
                                    request, entry, entry.getChangeDate())));

                    }
                }

                if (showCreated
                        && typeHandler.okToShowInHtml(entry, "owner", true)) {
                    String userSearchLink =
                        HtmlUtils
                            .href(HtmlUtils
                                .url(request
                                    .makeUrl(getRepository()
                                        .URL_USER_PROFILE), ARG_USER_ID,
                                            entry.getUser().getId()), entry
                                                .getUser()
                                                .getLabel(), "title=\"View user profile\"");


                    String linkMsg =
                        msg(
                        "Search for entries of this type created by the user");
                    String userLinkId = HtmlUtils.getUniqueId("userlink_");
                    userSearchLink = HtmlUtils
                        .href(getSearchManager().URL_SEARCH_TYPE + "/"
                              + entry.getTypeHandler().getType() + "?"
                              + ARG_USER_ID + "=" + entry.getUser().getId()
                              + "&" + SearchManager.ARG_SEARCH_SUBMIT
                              + "=true", entry.getUser().getLabel(), HtmlUtils
                                  .id(userLinkId) + HtmlUtils
                                  .cssClass("entry-type-search") + HtmlUtils
                                  .attr(HtmlUtils
                                      .ATTR_ALT, msg(linkMsg)) + HtmlUtils
                                          .attr(HtmlUtils
                                              .ATTR_TITLE, linkMsg));


                    sb.append(formEntry(request, msgLabel("Created by"),
                                        userSearchLink));
                    /*
                      String tt = JQuery.select(JQuery.id(userLinkId)) +".tooltip({content: function() {return 'tooltip';}});\n";
                      sb.append(HtmlUtils.comment("user tooltip"));
                      sb.append(HtmlUtils.script("\n$(function() {\n" + tt +"\n});\n"));
                      System.err.println(tt);
                    */
                }
            }


            boolean hasDataDate = false;

            if (Math.abs(entry.getCreateDate() - entry.getStartDate())
                    > 60000) {
                hasDataDate = true;
            } else if (Math.abs(entry.getCreateDate() - entry.getEndDate())
                       > 60000) {
                hasDataDate = true;
            }


            if (showDate && hasDataDate) {
                if (entry.getEndDate() != entry.getStartDate()) {
                    String startDate = getDateHandler().formatDate(request,
                                           entry, entry.getStartDate());
                    String endDate = getDateHandler().formatDate(request,
                                         entry, entry.getEndDate());
                    String searchUrl =
                        HtmlUtils
                            .url(request
                                .makeUrl(getRepository().getSearchManager()
                                    .URL_ENTRY_SEARCH), Misc
                                        .newList(ARG_DATA_DATE + "."
                                            + ARG_FROM, startDate,
                                                ARG_DATA_DATE + "." + ARG_TO,
                                                endDate));
                    sb.append(formEntry(request, msgLabel("Start Date"),
                                        startDate));
                    sb.append(formEntry(request, msgLabel("End Date"),
                                        endDate));
                } else {
                    boolean showTime = typeHandler.okToShowInForm(entry,
                                           "time", true);
                    StringBuilder dateSB = new StringBuilder();
                    dateSB.append(getDateHandler().formatDate(request, entry,
                            entry.getStartDate()));


                    if (typeHandler.okToShowInForm(entry, ARG_TODATE)
                            && (entry.getEndDate() != entry.getStartDate())) {
                        dateSB.append(" - ");
                        dateSB.append(getDateHandler().formatDate(request,
                                entry, entry.getEndDate()));
                    }
                    String formLabel = msgLabel(getFormLabel(entry, ARG_DATE,
                                           "Date"));
                    sb.append(formEntry(request, formLabel,
                                        dateSB.toString()));
                }
            }




            String category = entry.getCategory();
            if ( !entry.getTypeHandler().hasDefaultCategory()
                    && (category != null) && (category.length() > 0)) {
                sb.append(formEntry(request, msgLabel("Data Type"),
                                    entry.getCategory()));
            }

            //Don't show the latlon in the html
            boolean showMap = false;
            if (showMap) {
                if (entry.hasLocationDefined()) {
                    sb.append(formEntry(request, msgLabel("Location"),
                                        formatLocation(entry.getSouth(),
                                            +entry.getEast())));
                } else if (entry.hasAreaDefined()) {
                    /*
                    String img =
                        HtmlUtils.img(request.makeUrl(getRepository().URL_GETMAP,
                            ARG_SOUTH, "" + entry.getSouth(), ARG_WEST,
                            "" + entry.getWest(), ARG_NORTH,
                            "" + entry.getNorth(), ARG_EAST,
                            "" + entry.getEast()));
                    //                    sb.append(HtmlUtils.formEntry(msgLabel("Area"), img));
                    String areaHtml = "<table><tr align=center><td>"
                                      + entry.getNorth()
                                      + "</td></tr><tr align=center><td>"
                                      + entry.getWest() + "  "
                                      + entry.getEast()
                                      + "</td></tr><tr align=center><td>"
                                      + entry.getSouth()
                                      + "</td></tr></table>";
                    sb.append(HtmlUtils.formEntry(msgLabel("Area"), areaHtml));
                    */
                }
            }
            if (entry.hasAltitude() && (entry.getAltitude() != 0)) {
                sb.append(formEntry(request, msgLabel("Elevation"),
                                    "" + entry.getAltitude()));
            }


        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}

        return sb;

    }

    /**
     * _more_
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public String formatLocation(double lat, double lon) {
        if (latLonFormat != null) {
            synchronized (latLonFormat) {
                return latLonFormat.format(lat) + "/"
                       + latLonFormat.format(lon);
            }
        }

        return Misc.format(lat) + "/" + Misc.format(lon);
    }



    /**
     * _more_
     *
     * @param desc _more_
     *
     * @return _more_
     */
    public static boolean isWikiText(String desc) {
        if (desc == null) {
            return false;
        }

        return (desc.trim().startsWith("<wiki_inner>")
                || desc.trim().startsWith("<wiki>"));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryResourceHref(Request request, Entry entry)
            throws Exception {
        if ( !getAccessManager().canDownload(request, entry)) {
            /*
            if(!entry.isGroup() && !seenIt.contains(entry.getId())) {
                seenIt.add(entry.getId());
                getLogManager().logInfoAndPrint("cannot download:" + entry);
                Resource resource = entry.getResource();
                getLogManager().logInfoAndPrint("\tresource:" + resource +
                " type:" + resource.getType() +
                " exists:" +  resource.getTheFile().exists() +
                " the file:" + resource.getTheFile());

            }
            */
            return null;
        }
        String size = " ("
                      + formatFileLength(entry.getResource().getFileSize())
                      + ")";

        String fileTail = getStorageManager().getFileTail(entry);
        fileTail = HtmlUtils.urlEncodeExceptSpace(fileTail);

        return HtmlUtils.href(
            getEntryManager().getEntryResourceUrl(request, entry),
            HtmlUtils.img(getRepository().getIconUrl(ICON_FETCH)) + " "
            + entry.getName());
    }



    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryResourceUrl(Request request, Entry entry)
            throws Exception {
        return getEntryManager().getEntryResourceUrl(request, entry);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean okToSetNewNameDefault() {
        return true;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeNewEntry(Request request, Entry entry,boolean fromImport)
            throws Exception {

        if (parent != null) {
            parent.initializeNewEntry(request, entry,fromImport);
        }


        //Check if we're supposed to extract urls from the file
        if (getTypeProperty(PROP_INGEST_LINKS, "false").equals("true")
                && entry.getResource().isFile()) {
            String contents = IOUtil.readContents(
                                  getStorageManager().getFileInputStream(
                                      entry.getFile()));
            for (String url :
                    Utils.extractPatterns(contents,
                                          "(https?://[^\"' \\),]+)")) {
                getMetadataManager().addMetadata(entry,
                        new Metadata(getRepository().getGUID(),
                                     entry.getId(),
                                     ContentMetadataHandler.TYPE_URL, false,
                                     url, url, "", "", ""));
            }
        }



        //Do we extract metadata from the file path
        if (fieldFilePattern != null) {
            String  path    = entry.getResource().getPath();
            Matcher matcher = fieldFilePattern.matcher(path);
            if ( !matcher.find()) {
                matcher = fieldFilePattern.matcher(path.toLowerCase());
                if ( !matcher.find()) {
                    //                System.err.println("no match:"  + entry.getResource().getPath());
                    return;
                }
            }
            Object[] values    = getEntryValues(entry);
            String   year      = null;
            String   month     = null;
            String   day       = null;
            String   julianDay = null;

            //            System.err.println("match:" + entry.getResource().getPath());
            for (int i = 0; i < fieldPatternNames.size(); i++) {
                String columnName = fieldPatternNames.get(i);
                String value      = matcher.group(i + 1);
                Column column     = getColumn(columnName);
                if (column == null) {
                    if (columnName.equals("year")) {
                        year = value;

                        continue;
                    }
                    if (columnName.equals("month")) {
                        month = value;

                        continue;
                    }
                    if (columnName.equals("day")) {
                        day = value;

                        continue;
                    }
                    if (columnName.equals("julian_day")) {
                        julianDay = value;

                        continue;
                    }

                    System.err.println("Unknown column:" + columnName);

                    continue;
                }
                column.setValue(entry, values, value);
            }

            if ((year != null) && entry.sameDate()) {
                Date date = null;
                if (month != null) {
                    if (day != null) {
                        date = new SimpleDateFormat("yyyy-MM-dd").parse(year
                                + "-" + month + "-" + day);
                    } else {
                        date = new SimpleDateFormat("yyyy-MM").parse(year
                                + "-" + month);
                    }
                } else if (julianDay != null) {
                    GregorianCalendar gc = new GregorianCalendar();
                    gc.set(GregorianCalendar.YEAR, Integer.parseInt(year));
                    gc.set(GregorianCalendar.DAY_OF_YEAR,
                           Integer.parseInt(julianDay));
                    date = gc.getTime();
                }
                if (date != null) {
                    entry.setStartDate(date.getTime());
                    entry.setEndDate(date.getTime());
                }
            }
        }

        //Now run the services
        for (Service service : services) {
            if ( !service.isEnabled()) {
                continue;
            }
            try {
                File         workDir = getStorageManager().createProcessDir();
                ServiceInput serviceInput = new ServiceInput(workDir, entry);
                System.err.println("execing command: " + this);
                ServiceOutput output =
                    service.evaluate(getRepository().getTmpRequest(),
                                     serviceInput, null);
                if ( !output.isOk()) {
                    System.err.println("service not ok");

                    continue;
                }


                //Defer to the entry's type handler
                System.err.println("calling handleServiceResults:"
                                   + entry.getTypeHandler());
                entry.getTypeHandler().handleServiceResults(request, entry,
                        service, output);
            } catch (Exception exc) {
                getLogManager().logError(
                    "ERROR: TypeHandler calling service:" + service + "\n",
                    exc);
            }
        }




    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param service _more_
     * @param output _more_
     *
     * @throws Exception _more_
     */
    public void handleServiceResults(Request request, Entry entry,
                                     Service service, ServiceOutput output)
            throws Exception {
        if (parent != null) {
            parent.handleServiceResults(request, entry, service, output);

            return;
        }


        String namePattern = service.getNamePattern();
        if (namePattern != null) {
            String results = output.getResults();
            String name    = StringUtil.findPattern(results, namePattern);
            if (name != null) {
                entry.setName(name);
            }
            System.err.println("name= " + name);
        }


        String descriptionPattern = service.getDescriptionPattern();
        if (descriptionPattern != null) {
            String results = output.getResults();
            String description = StringUtil.findPattern(results,
                                     descriptionPattern);
            if (description != null) {
                entry.setDescription(description);
            }
            System.err.println("description= " + description);
        }


        List<Entry> entries = output.getEntries();
        if (entries.size() == 0) {
            return;
        }

        String target = service.getTarget();
        if (target == null) {
            //            System.err.println("TypeHandler: No target for service:" + service);
            return;
        }
        if (target.equals(TARGET_ATTACHMENT)) {
            for (Entry serviceEntry : entries) {
                String fileName = getStorageManager().copyToEntryDir(entry,
                                      serviceEntry.getFile()).getName();


                String mtype = isImage(serviceEntry)
                               ? ContentMetadataHandler.TYPE_THUMBNAIL
                               : ContentMetadataHandler.TYPE_ATTACHMENT;
                Metadata metadata = new Metadata(getRepository().getGUID(),
                                        entry.getId(), mtype, false,
                                        fileName, null, null, null, null);
                getMetadataManager().addMetadata(entry, metadata);
            }
        } else if (target.equals(TARGET_SIBLING)
                   || target.equals(TARGET_CHILD)) {
            for (Entry serviceEntry : entries) {
                File   f    = serviceEntry.getFile();
                String name = f.getName();
                f = getStorageManager().copyToStorage(request, f,
                        getStorageManager().getStorageFileName(f.getName()));



                TypeHandler typeHandler = null;
                if (service.getTargetType() != null) {
                    typeHandler = getRepository().getTypeHandler(
                        service.getTargetType());
                }
                if (typeHandler == null) {
                    typeHandler = getEntryManager().findDefaultTypeHandler(
                        f.toString());
                }
                Entry parent = target.equals(TARGET_CHILD)
                               ? entry
                               : entry.getParentEntry();
                Entry newEntry = getEntryManager().addFileEntry(request, f,
                                     parent, name, request.getUser(),
                                     typeHandler, null);

                getRepository().addAuthToken(request);
                getAssociationManager().addAssociation(request, entry,
                        newEntry, "derived", "derived");
            }
        } else {
            System.err.println("Unknown target:" + target);
        }
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean isImage(Entry entry) {
        if (isType("type_image")) {
            return true;
        }
        if (entry.getResource().isImage()) {
            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public String getUploadedFile(Request request) {
        return request.getUploadedFile(ARG_FILE);
    }

    /**
     * _more_
     *
     * @param newEntry _more_
     * @param oldEntry _more_
     *
     * @throws Exception _more_
     */
    public void initializeCopiedEntry(Entry newEntry, Entry oldEntry)
            throws Exception {
        if (parent != null) {
            parent.initializeCopiedEntry(newEntry, oldEntry);
        }
    }


    /**
     * _more_
     *
     * @param longName _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getListTypes(boolean longName) {
        return new ArrayList<TwoFacedObject>();

    }

    /**
     * _more_
     *
     * @param request The request
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processList(Request request, String what) throws Exception {
        return new Result("Error",
                          new StringBuilder(msgLabel("Unknown listing type")
                                            + what));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return Tables.ENTRIES.NAME;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String cleanQueryString(String s) {
        s = s.replace("\r\n", " ");
        s = StringUtil.stripAndReplace(s, "'", "'", "'dummy'");

        return s;
    }




    /**
     * _more_
     *
     * @param request The request
     * @param what _more_
     * @param clause _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(Request request, String what, Clause clause,
                            String extra)
            throws Exception {
        List<Clause> clauses = new ArrayList<Clause>();
        clauses.add(clause);

        return select(request, what, clauses, extra);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param what _more_
     * @param clauses _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(Request request, String what,
                            List<Clause> clauses, String extra)
            throws Exception {

        clauses = new ArrayList<Clause>(clauses);


        //We do the replace because (for some reason) any CRNW screws up the pattern matching
        String       whatString   = cleanQueryString(what);
        String       extraString  = cleanQueryString(extra);

        List<String> myTableNames = new ArrayList<String>();
        getTableNames(myTableNames);

        List<String> tableNames = (List<String>) Misc.toList(new String[] {
                                      Tables.ENTRIES.NAME,
                                      Tables.METADATA.NAME,
                                      Tables.USERS.NAME,
                                      Tables.ASSOCIATIONS.NAME });
        tableNames.addAll(myTableNames);
        HashSet seenTables = new HashSet();

        List    tables     = new ArrayList();
        boolean didEntries = false;
        boolean didMeta    = false;

        int     cnt        = 0;
        for (String tableName : tableNames) {
            String pattern = ".*[, =\\(]+" + tableName + "\\..*";
            if (Clause.isColumnFromTable(clauses, tableName)
                    || whatString.matches(pattern)
                    || (extraString.matches(pattern))) {
                tables.add(tableName);
                if (tableName.equals(Tables.ENTRIES.NAME)) {
                    didEntries = true;
                } else if (tableName.equals(Tables.METADATA.NAME)) {
                    didMeta = true;
                } else if (myTableNames.contains(tableName)) {
                    seenTables.add(tableName);
                }
            }
            cnt++;
        }

        if (didMeta) {
            tables.add(Tables.METADATA.NAME);
            didEntries = true;
        }


        int metadataCnt = 0;

        while (true) {
            String subTable = Tables.METADATA.NAME + "_" + metadataCnt;
            metadataCnt++;
            if ( !Clause.isColumnFromTable(clauses, subTable)) {
                break;
            }
            tables.add(Tables.METADATA.NAME + " " + subTable);
        }

        if (didEntries) {
            List<String> typeList = (List<String>) request.get(ARG_TYPE,
                                        new ArrayList());
            typeList.remove(TYPE_ANY);
            if (typeList.size() > 0) {
                List<String> types = new ArrayList<String>();
                for (String type : typeList) {
                    TypeHandler typeHandler =
                        getRepository().getTypeHandler(type);
                    if (typeHandler == null) {
                        //Force the bad type
                        types.add(type);

                        continue;
                    }
                    typeHandler.getChildTypes(types);
                }
                String typeString;
                if (request.get(ARG_TYPE_EXCLUDE, false)) {
                    typeString = "!" + StringUtil.join(",!", types);
                } else {
                    typeString = StringUtil.join(",", types);
                }
                if ( !Clause.isColumn(clauses, Tables.ENTRIES.COL_TYPE)) {
                    addOrClause(Tables.ENTRIES.COL_TYPE, typeString, clauses);
                }
            }
        }


        if (isOrSearch(request)) {
            Clause clause = Clause.or(clauses);
            clauses = new ArrayList<Clause>();
            clauses.add(clause);
        }

        //The join
        if (didEntries) {
            for (String otherTableName : myTableNames) {
                if (seenTables.contains(otherTableName)
                        && !Tables.ENTRIES.NAME.equalsIgnoreCase(
                            otherTableName)) {
                    clauses.add(0, Clause.join(Tables.ENTRIES.COL_ID,
                            otherTableName + ".id"));
                }
            }
        }

        //        System.err.println("clauses:" + clauses);


        int max = request.get(ARG_MAX, DB_MAX_ROWS);

        return getDatabaseManager().select(what, tables, Clause.and(clauses),
                                           extra, max);

    }



    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, Appendable sb,
                               Entry parentEntry, Entry entry,
                               FormInfo formInfo)
            throws Exception {

        try {
            sb.append(
                HtmlUtils.formEntry(
                    "", getWikiManager().wikifyEntry(request, (entry != null)
                    ? entry
                    : parentEntry, editHelp)));

            addBasicToEntryForm(request, sb, parentEntry, entry, formInfo,
                                this);
            addSpecialToEntryForm(request, sb, parentEntry, entry, formInfo,
                                  this);

            if ((entry != null) && request.getUser().getAdmin()
                    && okToShowInForm(entry, "owner", true)) {
                sb.append(formEntry(request, msgLabel("Owner"),
                                    HtmlUtils.input(ARG_USER_ID,
                                        ((entry != null)
                                         ? entry.getUser().getId()
                                         : ""), HtmlUtils.SIZE_20) + " "
                                         + msg("Optionally specify an owner")));
            }
        } catch (Exception exc) {
            StringBuilder tmp = new StringBuilder();
            tmp.append(
                getPageHandler().showDialogError(
                    "An error has occurred:" + exc));

            if ((request.getUser() != null) && request.getUser().getAdmin()) {
                Throwable inner = LogUtil.getInnerException(exc);
                tmp.append(
                    HtmlUtils.pre(
                        HtmlUtils.entityEncode(LogUtil.getStackTrace(exc)),
                        "style='max-height:300px;overflow-y:auto;'"));
            }
            sb.append(HtmlUtils.formEntry("", tmp.toString()));
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     * @param widget _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFormWidget(Request request, Entry entry, Column column,
                                String widget)
            throws Exception {
	if(true)
	    return widget;

        return HtmlUtils.hbox(widget, getFormHelp(request, entry, column));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFormHelp(Request request, Entry entry, Column column)
            throws Exception {
        return HtmlUtils.inset(column.getSuffix(), 5);
    }


    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception _more_
     */
    public void addSpecialToEntryForm(Request request, Appendable sb,
                                      Entry parentEntry, Entry entry,
                                      FormInfo formInfo,
                                      TypeHandler sourceTypeHandler)
            throws Exception {
        if (parent != null) {
            parent.addSpecialToEntryForm(request, sb, parentEntry, entry,
                                         formInfo, sourceTypeHandler);

            return;
        }
    }


    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    public void addSpatialToEntryForm(Request request, Appendable sb,
                                      Entry parentEntry, Entry entry,
                                      FormInfo formInfo)
            throws Exception {

        MapOutputHandler mapOutputHandler =
            (MapOutputHandler) getRepository().getOutputHandler(
                MapOutputHandler.OUTPUT_MAP.getId());
        if (okToShowInForm(entry, ARG_LOCATION, false)) {
            String lat = "";
            String lon = "";
            if (entry != null) {
                if (entry.hasNorth()) {
                    lat = "" + entry.getNorth();
                }
                if (entry.hasWest()) {
                    lon = "" + entry.getWest();
                }
            }
            String locationWidget = msgLabel("Latitude") + " "
                                    + HtmlUtils.input(
                                        ARG_LOCATION_LATITUDE, lat,
                                        HtmlUtils.SIZE_6) + "  "
                                            + msgLabel("Longitude") + " "
                                            + HtmlUtils.input(
                                                ARG_LOCATION_LONGITUDE, lon,
                                                HtmlUtils.SIZE_6);

            String[] nwse = new String[] { lat, lon };
            //            sb.append(formEntry(request, msgLabel("Location"),  locationWidget));
            MapInfo map = getMapManager().createMap(request, entry, true,
                              getMapManager().getMapProps(request, entry,
                                  null));
            String mapSelector = map.makeSelector(ARG_LOCATION, true, nwse,
                                     "", "");
            sb.append(formEntry(request, msgLabel("Location"), mapSelector));

        } else if (okToShowInForm(entry, ARG_AREA)) {
            addAreaWidget(request, entry, sb, formInfo);
        }



        if (okToShowInForm(entry, ARG_ALTITUDE, false)) {
            String altitude = "";
            if ((entry != null) && entry.hasAltitude()) {
                altitude = "" + Misc.format(entry.getAltitude());
            }
            sb.append(formEntry(request, "Altitude:",
                                HtmlUtils.input(ARG_ALTITUDE, altitude,
                                    HtmlUtils.SIZE_10)));
        } else if (okToShowInForm(entry, ARG_ALTITUDE_TOP, false)) {
            String altitudeTop    = "";
            String altitudeBottom = "";
            if (entry != null) {
                if (entry.hasAltitudeTop()) {
                    altitudeTop = "" + Misc.format(entry.getAltitudeTop());
                }
                if (entry.hasAltitudeBottom()) {
                    altitudeBottom =
                        "" + Misc.format(entry.getAltitudeBottom());
                }
            }
            sb.append(formEntry(request, "Altitude Range:",
                                HtmlUtils.input(ARG_ALTITUDE_BOTTOM,
                                    altitudeBottom,
                                    HtmlUtils.SIZE_10) + " - "
                                        + HtmlUtils.input(ARG_ALTITUDE_TOP,
                                            altitudeTop,
                                            HtmlUtils.SIZE_10) + " "
                                                + msg("meters")));
        }



    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param sb _more_
     * @param formInfo _more_
     *
     * @throws Exception _more_
     */
    public void addAreaWidget(Request request, Entry entry, Appendable sb,
                              FormInfo formInfo)
            throws Exception {
        String[]                  nwse  = null;
        Hashtable<String, String> props = null;


        if (entry != null) {
            props = getMapManager().getMapProps(request, entry, props);
            nwse  = new String[] { entry.hasNorth()
                                   ? "" + entry.getNorth()
                                   : "", entry.hasWest()
                                         ? "" + entry.getWest()
                                         : "", entry.hasSouth()
                    ? "" + entry.getSouth()
                    : "", entry.hasEast()
                          ? "" + entry.getEast()
                          : "", };

        }  /*else if (parentEntry != null) {
              don't popuplate the form with the parent location
                if (parentEntry.hasAreaDefined()) {
                nwse = new String[] { "" + parentEntry.getNorth(),
                "" + parentEntry.getWest(),
                "" + parentEntry.getSouth(),
                "" + parentEntry.getEast() };
                } else if (parentEntry.hasLocationDefined()) {
                nwse = new String[] { "" + parentEntry.getNorth(),
                "" + parentEntry.getWest(),
                "" + parentEntry.getSouth(),
                "" + parentEntry.getEast() };
                }
         }
             */
        String extraMapStuff = "";
        MapInfo map = getRepository().getMapManager().createMap(request,
                          entry, true, props);
        addToMapSelector(request, entry, map);
        String mapSelector = map.makeSelector(ARG_AREA, true, nwse, "", "")
                             + extraMapStuff;
        sb.append(formEntry(request, msgLabel("Location"), mapSelector));
    }



    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addDateToEntryForm(Request request, Appendable sb,
                                   Entry entry)
            throws Exception {

        String dateHelp = " (e.g., 2007-12-11 00:00:00)";
        /*        String fromDate = ((entry != null)
                           ? getDateHandler().formatDate(request,entry,entry.getStartDate())
                           : BLANK);
        String toDate = ((entry != null)
                         ? getDateHandler().formatDate(request, entry, entry.getEndDate())
                         : BLANK);*/

        String  timezone  = ((entry == null)
                             ? null
                             : getEntryUtil().getTimezone(entry));

        Date[]  dateRange = getDefaultDateRange(request, entry);
        Date    fromDate  = dateRange[0];
        Date    toDate    = dateRange[1];

        boolean showTime  = okToShowInForm(entry, "time", true);
        if (okToShowInForm(entry, ARG_DATE)) {
            String setTimeCbx = "";
            /*
            if (okToShowInForm(entry, "settimerange") && entry != null && entry.isGroup()) {
                setTimeCbx = HtmlUtils.checkbox(
                                                ARG_SETTIMEFROMCHILDREN, "true",
                                                false) + " "
                    + msg("Set time range from children");
            }
            */

            if ( !okToShowInForm(entry, ARG_TODATE)) {
                sb.append(
                    formEntry(
                        request,
                        msgLabel(getFormLabel(entry, ARG_DATE, "Date")),
                        getDateHandler().makeDateInput(
                            request, ARG_FROMDATE, "entryform", fromDate,
                            timezone, showTime) + " " + setTimeCbx));

            } else {
                sb.append(
                    formEntry(
                        request,
                        msgLabel(
                            getFormLabel(
                                entry, ARG_DATE,
                                "Date Range")), getDateHandler()
                                    .makeDateInput(
                                        request, ARG_FROMDATE, "entryform",
                                        fromDate, timezone,
                                        showTime) + HtmlUtils.space(1)
                                            + HtmlUtils
                                                .img(getIconUrl(
                                                    ICON_RANGE)) + HtmlUtils
                                                        .space(1) +
                //                        " <b>--</b> " +
                getDateHandler().makeDateInput(request, ARG_TODATE,
                        "entryform", toDate, timezone,
                        showTime) + HtmlUtils.space(2) + " " + setTimeCbx));
            }

        }


    }


    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     */
    public Date[] getDefaultDateRange(Request request, Entry entry) {
        Date fromDate = ((entry != null)
                         ? new Date(entry.getStartDate())
                         : null);
        Date toDate   = ((entry != null)
                         ? new Date(entry.getEndDate())
                         : null);

        return new Date[] { fromDate, toDate };
    }




    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param parentEntry _more_
     * @param entry _more_
     * @param formInfo _more_
     * @param sourceTypeHandler _more_
     *
     * @throws Exception _more_
     */
    public void addBasicToEntryForm(Request request, Appendable sb,
                                    Entry parentEntry, Entry entry,
                                    FormInfo formInfo,
                                    TypeHandler sourceTypeHandler)
            throws Exception {

        String size = HtmlUtils.SIZE_70;

        boolean forUpload = (entry == null)
                            && getType().equals(TYPE_CONTRIBUTION);

        if (forUpload) {
            sb.append(formEntry(request, msgLabel("Your Name"),
                                HtmlUtils.input(ARG_CONTRIBUTION_FROMNAME,
                                    "", size)));
            sb.append(formEntry(request, msgLabel("Your Email"),
                                HtmlUtils.input(ARG_CONTRIBUTION_FROMEMAIL,
                                    "", size)));
        }


        String[] whatList = (entry == null)
                            ? FIELDS_NOENTRY
                            : FIELDS_ENTRY;
        String   domId;
        for (String what : whatList) {
            if (what.equals(ARG_NAME)) {
                if ( !forUpload && okToShowInForm(entry, ARG_NAME)) {
                    domId = HtmlUtils.getUniqueId("entryinput");
                    formInfo.addMaxSizeValidation("Name", domId,
                            EntryManager.MAX_NAME_LENGTH);
                    String nameDefault = request.getString(ARG_NAME,
                                             getFormDefault(entry, ARG_NAME,
                                                 ""));
                    sb.append(
                        formEntry(
                            request,
                            msgLabel(getFormLabel(entry, ARG_NAME, "Name")),
                            HtmlUtils.input(ARG_NAME, ((entry != null)
                            ? entry.getName()
                            : nameDefault), size + HtmlUtils.id(domId))));
                } else {
                    String nameDefault = getFormDefault(entry, ARG_NAME,
                                             null);
                    if (nameDefault != null) {
                        sb.append(HtmlUtils.hidden(ARG_NAME, nameDefault));
                    }
                }

                continue;
            }

            if (what.equals(ARG_DESCRIPTION)) {
                if (okToShowInForm(entry, ARG_DESCRIPTION)) {
                    String desc = "";
                    int rows = getProperty(
                                   entry, "form.description.rows",
                                   getRepository().getProperty(
                                       "ramadda.edit.rows", 5));
                    boolean isWiki = getProperty(entry,
                                         "form.description.iswiki", false);

                    if (entry != null) {
                        desc = entry.getDescription();
                    }
                    if (desc.length() > 100) {
                        rows = rows * 2;
                    }
                    if (isWiki) {
                        addWikiEditor(request, entry, sb, formInfo,
                                      ARG_DESCRIPTION + "_editor",
                                      ARG_DESCRIPTION, desc, "Description",
                                      false,
                                      EntryManager.MAX_DESCRIPTION_LENGTH);
                    } else {
                        desc = desc.trim();
                        boolean isTextWiki = isWikiText(desc);
                        if (desc.startsWith("<wiki>")) {
                            desc = desc.substring(6).trim();
                        }
                        String        cbxId  = "iswiki";
                        String        textId = ARG_DESCRIPTION;
                        String        wikiId = ARG_WIKITEXT + "_editor";
                        StringBuilder tmpSB  = new StringBuilder();
                        String cbx = HtmlUtils.checkbox(
                                         ARG_ISWIKI, "true", isTextWiki,
                                         HtmlUtils.id(cbxId)
                                         + HtmlUtils.title(
                                             "Wikify text")) + HtmlUtils.getIconImage(
                                                 getRepository().getIconUrl(
                                                     ICON_WIKI), "title",
                                                         "Wikify text");

                        HtmlUtils.open(tmpSB, "div",
                                       HtmlUtils.attrs("style", isTextWiki
                                ? ""
                                : "display:none;", "id", wikiId + "_block"));

                        addWikiEditor(request, entry, tmpSB, formInfo,
                                      wikiId, ARG_WIKITEXT, desc, null,
                                      false,
                                      EntryManager.MAX_DESCRIPTION_LENGTH);
                        HtmlUtils.close(tmpSB, "div");
                        HtmlUtils.open(tmpSB, "div",
                                       HtmlUtils.attrs("style", !isTextWiki
                                ? ""
                                : "display:none;", "id", textId + "_block"));
                        tmpSB.append(HtmlUtils.textArea(ARG_DESCRIPTION,
                                desc, rows, HtmlUtils.id(textId)));
                        HtmlUtils.script(tmpSB,
                                         "HtmlUtils.initWikiEditor("
                                         + HtmlUtils.squote(wikiId) + ","
                                         + HtmlUtils.squote(textId) + ","
                                         + HtmlUtils.squote(cbxId) + ");");

                        HtmlUtils.close(tmpSB, "div");
                        sb.append(formEntryTop(request,
                                getFormLabel(entry, ARG_DESCRIPTION,
                                             "Description:<br>"
                                             + cbx), tmpSB.toString()));
                    }
                }

                continue;
            }

            if (what.equals(ARG_RESOURCE)) {

                boolean showFile = okToShowInForm(entry, ARG_FILE);
                boolean showLocalFile = showFile
                                        && request.getUser().getAdmin()
                                        && okToShowInForm(entry,
                                            ARG_SERVERFILE);
                boolean showUrl          = (forUpload
                                            ? false
                                            : okToShowInForm(entry, ARG_URL));

                boolean showResourceForm = okToShowInForm(entry,
                                               ARG_RESOURCE);


                if (showResourceForm) {
                    boolean showDownload = showFile
                                           && okToShowInForm(entry,
                                               ARG_RESOURCE_DOWNLOAD);
                    List<String> tabTitles  = new ArrayList<String>();
                    List<String> tabContent = new ArrayList<String>();
                    String urlLabel = getFormLabel(entry, ARG_URL, "URL");
                    String fileLabel = getFormLabel(entry, ARG_FILE, "File");
                    if (showFile) {
                        String formContent = HtmlUtils.fileInput(ARG_FILE,
                                                 size);
                        tabTitles.add(msg(fileLabel));
                        tabContent.add(HtmlUtils.inset(formContent, 8));
                    }
                    if (showUrl) {
                        String url = "";
                        if ((entry != null) && entry.getResource().isUrl()) {
                            url = entry.getResource().getPath();
                        }
                        String download = !showDownload
                                          ? ""
                                          : HtmlUtils.space(1)
                                            + HtmlUtils
                                                .checkbox(
                                                    ARG_RESOURCE_DOWNLOAD) + HtmlUtils
                                                        .space(1) + msg(
                                                            "Download");
                        String formContent = HtmlUtils.input(ARG_URL, url,
                                                 size) + "&nbsp;" + download;
                        tabTitles.add(urlLabel);
                        tabContent.add(HtmlUtils.inset(formContent, 8));
                    }

                    if (showLocalFile) {
                        StringBuilder localFilesSB = new StringBuilder();
                        localFilesSB.append(HtmlUtils.formTable());
                        localFilesSB.append(
                            HtmlUtils.formEntry(
                                msgLabel("File or directory"),
                                HtmlUtils.input(ARG_SERVERFILE, "", size)
                                + " "
                                + msg("Note: If a directory then all files will be added")));
                        localFilesSB.append(
                            HtmlUtils.formEntry(
                                msgLabel("Pattern"),
                                HtmlUtils.input(
                                    ARG_SERVERFILE_PATTERN, "",
                                    HtmlUtils.SIZE_10)));
                        localFilesSB.append(HtmlUtils.formTableClose());
                        if (okToShowInForm(entry, "filesonserver", true)) {
                            tabTitles.add(msg("Files on Server"));
                            tabContent.add(
                                HtmlUtils.inset(localFilesSB.toString(), 8));
                        }
                    }



                    String extras = getFileExtras(request, entry);


                    /*datePatternWidget*/

                    String extra =
                        HtmlUtils.makeShowHideBlock(msg("More..."), extras,
                            false);
                    if (forUpload || !showDownload) {
                        extra = "";
                    }

                    if ( !okToShowInForm(entry, "resource.extra")) {
                        extra = "";
                    }

                    if (entry == null) {
                        if (tabTitles.size() > 1) {
                            sb.append(formEntryTop(request,
                                    msgLabel("Resource"),
                                    OutputHandler.makeTabs(tabTitles,
                                        tabContent, true) + extra));
                        } else if (tabTitles.size() == 1) {
                            sb.append(formEntry(request,
                                    tabTitles.get(0) + ":",
                                    tabContent.get(0) + extra));
                        }
                    } else {
                        //                if (entry.getResource().isFile()) {
                        //If its the admin then show the full path
                        if (showFile
                                && Utils.stringDefined(
                                    entry.getResource().getPath())) {
                            if (request.getUser().getAdmin()) {
                                sb.append(formEntry(request,
                                        msgLabel("Resource"),
                                        entry.getResource().getPath()));
                            } else {
                                sb.append(
                                    formEntry(
                                        request, msgLabel("Resource"),
                                        getStorageManager().getFileTail(
                                            entry)));
                            }
                        }
                        if (showFile) {
                            if (tabTitles.size() > 1) {
                                if (showFile) {
                                    sb.append(formEntryTop(request,
                                            msgLabel("New Resource"),
                                            OutputHandler.makeTabs(tabTitles,
                                                tabContent, true) + extra));
                                }
                            } else if (tabTitles.size() == 1) {
                                sb.append(formEntry(request,
                                        tabTitles.get(0) + ":",
                                        tabContent.get(0) + extra));
                            }
                        } else {
                            if (tabTitles.size() > 1) {
                                sb.append(formEntryTop(request,
                                        msgLabel("New Resource"),
                                        OutputHandler.makeTabs(tabTitles,
                                            tabContent, true) + extra));
                            } else if (tabTitles.size() == 1) {
                                sb.append(formEntry(request,
                                        tabTitles.get(0) + ":",
                                        tabContent.get(0) + extra));
                            }
                        }


                        if (showFile) {
                            if (entry.getResource().isStoredFile()) {
                                String formContent =
                                    HtmlUtils.fileInput(ARG_FILE, size);
                                /*                            sb.append(
                                                              formEntry(request,
                                                              msgLabel("Upload new file"), formContent));
                                */
                            }
                        }
                        /*                } else {
                                          sb.append(formEntry(request,msgLabel("Resource"),
                                          entry.getResource().getPath()));
                                          }*/
                    }

                    continue;
                }

                if (what.equals(ARG_CATEGORY)) {

                    if ( !hasDefaultCategory()
                            && okToShowInForm(entry, ARG_CATEGORY, false)) {
                        String selected = "";
                        if (entry != null) {
                            selected = entry.getCategory();
                        }
                        List   types  = getRepository().getDefaultCategorys();
                        String widget = ((types.size() > 1)
                                         ? HtmlUtils.select(
                                             ARG_CATEGORY_SELECT, types,
                                             selected) + HtmlUtils.space(1)
                                                 + msgLabel("Or")
                                         : "") + HtmlUtils.input(
                                             ARG_CATEGORY);
                        sb.append(formEntry(request, msgLabel("Data Type"),
                                            widget));
                    }

                }

                continue;
            }


            if (what.equals(ARG_DATE)) {
                addDateToEntryForm(request, sb, entry);

                continue;
            }
            if (what.equals(ARG_LOCATION)) {
                addSpatialToEntryForm(request, sb, parentEntry, entry,
                                      formInfo);

                continue;
            }
        }


        if (entry == null) {
            for (String[] idLabel : requiredMetadata) {
                MetadataHandler handler =
                    getMetadataManager().findMetadataHandler(idLabel[0]);
                if (handler != null) {
                    if (idLabel[1] != null) {
                        request.putExtraProperty(
                            MetadataType.PROP_METADATA_LABEL, idLabel[1]);
                    }
                    handler.makeAddForm(request, null,
                                        handler.findType(idLabel[0]), sb);
                    request.removeExtraProperty(
                        MetadataType.PROP_METADATA_LABEL);
                    sb.append("<tr><td colspan=2><hr></td></tr>");
                }
            }
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFileExtras(Request request, Entry entry)
            throws Exception {
        String addMetadata =
            HtmlUtils.checkbox(
                ARG_METADATA_ADD, "true",
                Misc.equals(
                    getFormDefault(entry, ARG_METADATA_ADD, "false"),
                    "false")) + HtmlUtils.space(1) + msg("Add properties")
                              + HtmlUtils.space(1)
                              + HtmlUtils.checkbox(ARG_METADATA_ADDSHORT)
                              + HtmlUtils.space(1)
                              + msg("Just spatial/temporal properties");

        List datePatterns = new ArrayList();
        datePatterns.add(new TwoFacedObject("", BLANK));
        for (int i = 0; i < DateUtil.DATE_PATTERNS.length; i++) {
            datePatterns.add(DateUtil.DATE_FORMATS[i]);
        }

        String unzipWidget = HtmlUtils.checkbox(ARG_FILE_UNZIP, "true", true)
                             + HtmlUtils.space(1) + msg("Unzip archive")
                             + " "
                             + HtmlUtils.checkbox(ARG_FILE_PRESERVEDIRECTORY,
                                 "true", false) + HtmlUtils.space(1)
                                     + msg("Make folders from archive");
        String makeNameWidget = HtmlUtils.checkbox(ARG_MAKENAME, "true",
                                    true) + HtmlUtils.space(1)
                                          + msg("Make name from filename");

        String deleteFileWidget = ((entry != null) && entry.isFile())
                                  ? HtmlUtils.checkbox(ARG_DELETEFILE,
                                      "true", false) + HtmlUtils.space(1)
                                          + msg("Delete file")
                                  : "";


        /*
          String datePatternWidget = msgLabel("Date pattern")
          + HtmlUtils.space(1)
          + HtmlUtils.select(ARG_DATE_PATTERN,
          datePatterns) + " ("
          + msg("Use file name") + ")";

        */

        String datePatternWidget =
            msgLabel("Date pattern") + HtmlUtils.space(1)
            + HtmlUtils.input(ARG_DATE_PATTERN,
                request.getString(ARG_DATE_PATTERN,
                    "")) + " (e.g., yyyy_MM_dd, yyyyMMdd_hhMM, etc. )";



        String extraMore = "";

        if ((entry == null) && getType().equals(TYPE_FILE)) {
            extraMore = HtmlUtils.checkbox(ARG_TYPE_GUESS, "true", true)
                        + " " + msg("Figure out the type") + HtmlUtils.br();
        }


        String extras = extraMore + addMetadata + HtmlUtils.br()
                        + unzipWidget + HtmlUtils.br() + makeNameWidget
                        + HtmlUtils.br() + deleteFileWidget;

        return extras;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiEditorSidebar(Request request, Entry entry)
            throws Exception {
        return "";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String[]> getWikiEditLinks() {
        return null;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param type _more_
     * @param target _more_
     *
     * @throws Exception _more_
     */
    public void addToSelectMenu(Request request, Entry entry,
                                StringBuilder sb, String type, String target)
            throws Exception {}



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param buttons _more_
     * @param textAreaId _more_
     */
    public void addToWikiToolbar(Request request, Entry entry,
                                 StringBuilder buttons, String textAreaId) {}



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param text _more_
     *
     * @throws Exception _more_
     */
    public void addReadOnlyWikiEditor(Request request, Entry entry,
                                      Appendable sb, String text)
            throws Exception {
        String dummyId = Utils.getGuid();
        addWikiEditor(request, entry, sb, null, dummyId, "", text, null,
                      true, 0);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param formInfo _more_
     * @param editorId _more_
     * @param hiddenId _more_
     * @param text _more_
     * @param label _more_
     * @param readOnly _more_
     * @param length _more_
     *
     * @throws Exception _more_
     */
    public void addWikiEditor(Request request, Entry entry, Appendable sb,
                              FormInfo formInfo, String editorId,
                              String hiddenId, String text, String label,
                              boolean readOnly, int length)
            throws Exception {
        if (text.startsWith("<wiki>")) {
            text = text.substring(6).trim();
        }
        String sidebar = "";
        if ( !readOnly) {
            sidebar = getWikiEditorSidebar(request, entry);
            String buttons =
                getRepository().getWikiManager().makeWikiEditBar(request,
                    entry, editorId);
            if (label != null) {
                sb.append("<tr><td colspan=2>");
                sb.append(HtmlUtils.b(msgLabel(label)));
                sb.append(HtmlUtils.br());
            }
            sb.append(buttons);
        }
        if ((length > 0) && (formInfo != null)) {
            formInfo.addMaxSizeValidation(label, hiddenId, length);
        }
        text = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        String textWidget = HtmlUtils.div(text,
                                          HtmlUtils.id(editorId)
                                          + HtmlUtils.style("height:500px;")
                                          + HtmlUtils.attr("class",
                                              "ace_editor"));
        sb.append(HtmlUtils.hidden(hiddenId, "", HtmlUtils.id(hiddenId)));
        if (Utils.stringDefined(sidebar)) {
            sb.append(
                HtmlUtils.table(
                    HtmlUtils.row(
                        HtmlUtils.cols(
                            new String[] { HtmlUtils.td(textWidget),
                                           HtmlUtils.td(sidebar,
                                           " width=10%") }))));
        } else {
            sb.append(textWidget);
        }
        if (request.getExtraProperty("didace") == null) {
            request.putExtraProperty("didace", "true");
            HtmlUtils.importJS(
                sb, getRepository().getHtdocsUrl("/lib/ace/src-min/ace.js"));
            if ((formInfo != null) && !readOnly) {
                formInfo.appendExtraJS("HtmlUtil.handleAceEditorSubmit();\n");
            }
        }

        sb.append(HtmlUtils.script("HtmlUtil.initAceEditor('"
                                   + ((formInfo == null)
                                      ? "null"
                                      : formInfo.getId()) + "','" + editorId
                                      + "','" + hiddenId + "');"));
        if (label != null) {
            sb.append("</td></tr>");
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Column> getColumns() {
        return null;
    }

    /**
     * _more_
     *
     * @param columnName _more_
     *
     * @return _more_
     */
    public Column getColumn(String columnName) {
        List<Column> columns = getColumns();
        if (columns == null) {
            return null;
        }
        for (Column c : columns) {
            if (c.getName().equalsIgnoreCase(columnName)) {
                return c;
            }
        }

        return null;
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param propertyValue _more_
     * @param delimiter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getColumnEnumerationProperties(Column column,
            String propertyValue, String delimiter)
            throws Exception {
        if (propertyValue.startsWith("file:")) {
            //replace any macros {name} is the type id without the leading type_
            propertyValue = propertyValue.replace("${type}",
                    type).replace("${name}", type.replace("type_", ""));
            propertyValue = getStorageManager().readSystemResource(
                propertyValue.substring("file:".length()));
            delimiter = "\n";
        }
        List<String> tmp = StringUtil.split(propertyValue, delimiter, true,
                                            true);

        return tmp;
    }


    /**
     * _more_
     *
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public final String getIconUrl(Request request, Entry entry)
            throws Exception {
        return null;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryIconUrl(Request request, Entry entry)
            throws Exception {

        String icon = entry.getIcon();
        if (icon != null) {
            return getIconUrl(icon);
        }


        icon = getIconProperty(null);
        if (icon != null) {
            return getIconUrl(icon);
        }



        if (entry.isGroup()) {
            if (getAccessManager().hasPermissionSet(entry,
                    Permission.ACTION_VIEWCHILDREN)) {
                if ( !getAccessManager().canDoAction(request, entry,
                        Permission.ACTION_VIEWCHILDREN)) {
                    return getIconUrl(ICON_FOLDER_CLOSED_LOCKED);
                }
            }

            return getIconUrl(ICON_FOLDER_CLOSED);
        }
        Resource resource = entry.getResource();
        String   path     = resource.getPath();

        return getIconUrlFromPath(path);
    }


    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getIconUrlFromPath(String path) throws Exception {
        String img = ICON_FILE;
        if (path != null) {
            String suffix = IOUtil.getFileExtension(path.toLowerCase());
            String prop   = getRepository().getProperty("file.icon" + suffix);
            if (prop != null) {
                img = prop;
            }
        }

        return getIconUrl(img);
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getLabelFromPath(String path) throws Exception {
        if (path != null) {
            String suffix = IOUtil.getFileExtension(path.toLowerCase());
            String label  = getRepository().getProperty("file.label"
                                + suffix);
            if (label != null) {
                return label;
            }
        }

        return getTypeProperty("file.label", (String) null);
    }

    /**
     * _more_
     *
     * @param path _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getUrlFromPath(String path) throws Exception {
        if (path != null) {
            String suffix = IOUtil.getFileExtension(path.toLowerCase());
            String url    = getRepository().getProperty("file.url" + suffix);
            if (url != null) {
                return url;
            }
        }

        return getTypeProperty("file.url", (String) null);
    }




    /**
     * _more_
     *
     * @param request The request
     * @param sb _more_
     * @param type _more_
     */
    public void addTextSearch(Request request, Appendable sb, String type) {
        try {
            String name           = (String) request.getString(ARG_TEXT, "");
            String searchMetaData = " ";
            /*HtmlUtils.checkbox(ARG_SEARCHMETADATA,
                                        "true",
                                        request.get(ARG_SEARCHMETADATA,
                                        false)) + " "
                                        + msg("Search metadata");*/

            String searchExact = " "
                                 + HtmlUtils.checkbox(ARG_EXACT, "true",
                                     request.get(ARG_EXACT, false)) + " "
                                         + msg("Match exactly");
            String extra = HtmlUtils.p() + searchExact + searchMetaData;
            if (getDatabaseManager().supportsRegexp()) {
                extra = HtmlUtils.checkbox(
                    ARG_ISREGEXP, "true",
                    request.get(ARG_ISREGEXP, false)) + " "
                        + msg("Use regular expression");

                extra = HtmlUtils.makeToggleInline(msg("More..."), extra,
                        false);
            } else {
                extra = "";
            }

            sb.append(formEntry(request, msgLabel("Text"),
                                HtmlUtils.input(ARG_TEXT, name,
                                    HtmlUtils.id("searchinput")
                                    + HtmlUtils.SIZE_50
                                    + " autocomplete='off' autofocus ") + " "
                                        + extra));
            sb.append("<div id=searchpopup class=ramadda-popup></div>");
            sb.append(
                HtmlUtils.script(
                    "Utils.searchSuggestInit('searchinput'," + ((type == null)
                    ? "null"
                    : "'" + type + "'") + ");"));
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * _more_
     *
     * @param request The request
     * @param formBuffer _more_
     * @param fieldsToShow _more_
     *
     * @throws Exception _more_
     */
    public void addToSpecialSearchForm(Request request,
                                       Appendable formBuffer,
                                       HashSet<String> fieldsToShow)
            throws Exception {
        if (parent != null) {
            parent.addToSpecialSearchForm(request, formBuffer, fieldsToShow);
        }
    }


    /**
     * _more_
     *
     * @param request The request
     * @param titles _more_
     * @param contents _more_
     * @param where _more_
     * @param advancedForm _more_
     * @param showText _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, List<String> titles,
                                List<String> contents, List<Clause> where,
                                boolean advancedForm, boolean showText)
            throws Exception {


        if (parent != null) {
            parent.addToSearchForm(request, titles, contents, where,
                                   advancedForm, showText);

            return;
        }

        String type = null;
        if (request.defined(ARG_TYPE)) {
            TypeHandler typeHandler = getRepository().getTypeHandler(request);
            type = typeHandler.getType();
            if ( !typeHandler.isAnyHandler()) {
                //                typeHandlers.clear();
                //                typeHandlers.add(typeHandler);
            }
        }


        /*
        if(minDate==null || maxDate == null) {
            Statement stmt = select(request,
                                           SqlUtil.comma(
                                                         SqlUtil.min(Tables.ENTRIES.COL_FROMDATE),
                                                         SqlUtil.max(
                                                                     Tables.ENTRIES.COL_TODATE)), where);

            ResultSet dateResults = stmt.getResultSet();
            if (dateResults.next()) {
                if (dateResults.getDate(1) != null) {
                    if(minDate == null)
                        minDate = SqlUtil.getDateString("" + dateResults.getDate(1));
                    if(maxDate == null)
                        maxDate = SqlUtil.getDateString("" + dateResults.getDate(2));
                }
            }
            }
*/

        //        minDate = "";
        //        maxDate = "";


        StringBuilder basicSB    = new StringBuilder(HtmlUtils.formTable());
        StringBuilder advancedSB = new StringBuilder(HtmlUtils.formTable());


        if (showText) {
            addTextSearch(request, basicSB, type);
        }
        if (request.defined(ARG_USER_ID)) {
            basicSB.append(formEntry(request, msgLabel("User"),
                                     HtmlUtils.input(ARG_USER_ID,
                                         request.getString(ARG_USER_ID,
                                             ""))));
        }

        List<TypeHandler> typeHandlers = getRepository().getTypeHandlers();
        if (true || (typeHandlers.size() > 1)) {
            List tmp = new ArrayList();
            for (TypeHandler typeHandler : typeHandlers) {
                if ( !typeHandler.getForUser()) {
                    continue;
                }
                tmp.add(new TwoFacedObject(msg(typeHandler.getLabel()),
                                           typeHandler.getType()));
            }
            TwoFacedObject anyTfo = new TwoFacedObject(TYPE_ANY, TYPE_ANY);
            if ( !tmp.contains(anyTfo)) {
                tmp.add(0, anyTfo);
            }
            List typeList = request.get(ARG_TYPE, new ArrayList());
            typeList.remove(TYPE_ANY);

            String typeSelect = HtmlUtils.select(ARG_TYPE, tmp, typeList,
                                    (advancedForm
                                     ? " MULTIPLE SIZE=4 "
                                     : ""));
            String groupCbx = (advancedForm
                               ? HtmlUtils.checkbox(ARG_TYPE_EXCLUDE, "true",
                                   request.get(ARG_TYPE_EXCLUDE,
                                       false)) + HtmlUtils.space(1)
                                           + msg("Exclude")
                               : "");

            basicSB.append(
                formEntry(
                    request, msgLabel("Kind"),
                    typeSelect + HtmlUtils.space(1)
                    + HtmlUtils.submitImage(
                        getRepository().getIconUrl(ICON_SEARCH),
                        "submit_type",
                        msg("Show search form with this type"),
                        "") + HtmlUtils.space(1) + groupCbx));
        } else if (typeHandlers.size() == 1) {
            basicSB.append(HtmlUtils.hidden(ARG_TYPE,
                                            typeHandlers.get(0).getType()));
            basicSB.append(
                formEntry(
                    request, msgLabel("Kind"),
                    msg(typeHandlers.get(0).getDescription())));
        }

        for (DateArgument arg : DateArgument.SEARCH_ARGS) {
            addDateSearch(getRepository(), request, basicSB, arg, false);
        }


        if (advancedForm || request.defined(ARG_GROUP)) {
            String groupArg = (String) request.getString(ARG_GROUP, "");
            String searchChildren = " "
                                    + HtmlUtils.checkbox(ARG_GROUP_CHILDREN,
                                        "true",
                                        request.get(ARG_GROUP_CHILDREN,
                                            false)) + " ("
                                                + msg("Search sub-folders")
                                                + ")";
            if (groupArg.length() > 0) {
                basicSB.append(HtmlUtils.hidden(ARG_GROUP, groupArg));
                Entry group = getEntryManager().findGroup(request, groupArg);
                if (group != null) {
                    basicSB.append(formEntry(request, msgLabel("Folder"),
                                             group.getFullName() + "&nbsp;"
                                             + searchChildren));

                }
            } else {

                /**
                 * Statement stmt =
                 *   select(request,
                 *          SqlUtil.distinct(Tables.ENTRIES.COL_PARENT_GROUP_ID),
                 *          where, "");
                 *
                 * List<Entry> groups =
                 *   getRepository().getGroups(SqlUtil.readString(stmt, 1));
                 * getDatabaseManager().closeAndReleaseStatement(stmt);
                 *
                 * if (groups.size() > 1) {
                 *   List groupList = new ArrayList();
                 *   groupList.add(ALL_OBJECT);
                 *   for (Entry group : groups) {
                 *       groupList.add(
                 *           new TwoFacedObject(group.getFullName(), group.getId()));
                 *   }
                 *   String groupSelect = HtmlUtils.select(ARG_GROUP,
                 *                            groupList, null, 100);
                 *   advancedSB.append(formEntry(request,msgLabel("Folder"),
                 *           groupSelect + searchChildren));
                 * } else if (groups.size() == 1) {
                 *   advancedSB.append(HtmlUtils.hidden(ARG_GROUP,
                 *           groups.get(0).getId()));
                 *   advancedSB.append(formEntry(request,msgLabel("Folder"),
                 *           groups.get(0).getFullName() + searchChildren));
                 * }
                 */
            }
            advancedSB.append("\n");
        }


        if (advancedForm) {
            String             radio = getSpatialSearchTypeWidget(request);
            SelectionRectangle bbox  = getSelectionBounds(request);
            MapInfo map = getRepository().getMapManager().createMap(request,
                              null, true, null);

            String mapSelector = map.makeSelector(ARG_AREA, true,
                                     bbox.getStringArray(), "", radio);
            basicSB.append(formEntry(request, msgLabel("Area"), mapSelector));
            basicSB.append("\n");

            addSearchField(request, ARG_FILESUFFIX, basicSB);

        }


        basicSB.append(getSearchManager().makeOutputSettings(request));

        /*
        if (collection != null) {
            basicSB.append(formEntry(request,msgLabel("Collection"),
                    collectionSelect));
                    }*/


        basicSB.append(HtmlUtils.formTableClose());
        advancedSB.append(HtmlUtils.formTableClose());

        titles.add(msg("Type, date, space"));
        contents.add(basicSB.toString());

    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param request The request
     * @param basicSB _more_
     * @param arg _more_
     * @param showTime _more_
     *
     * @throws Exception _more_
     */
    public static void addDateSearch(Repository repository, Request request,
                                     Appendable basicSB, DateArgument arg,
                                     boolean showTime)
            throws Exception {
        List dateTypes = new ArrayList();
        dateTypes.add(new TwoFacedObject(msg("Contained by range"),
                                         DATE_SEARCHMODE_CONTAINEDBY));
        dateTypes.add(new TwoFacedObject(msg("Overlaps range"),
                                         DATE_SEARCHMODE_OVERLAPS));
        dateTypes.add(new TwoFacedObject(msg("Contains range"),
                                         DATE_SEARCHMODE_CONTAINS));

        String dateSelectValue;
        List   dateSelect = new ArrayList();
        dateSelect.add(new TwoFacedObject("---", "none"));
        dateSelect.add(new TwoFacedObject(msg("Last hour"), "-1 hour"));
        dateSelect.add(new TwoFacedObject(msg("Last 3 hours"), "-3 hours"));
        dateSelect.add(new TwoFacedObject(msg("Last 6 hours"), "-6 hours"));
        dateSelect.add(new TwoFacedObject(msg("Last 12 hours"), "-12 hours"));
        dateSelect.add(new TwoFacedObject(msg("Last day"), "-1 day"));
        dateSelect.add(new TwoFacedObject(msg("Last week"), "-7 days"));
        dateSelect.add(new TwoFacedObject(msg("Last 2 weeks"), "-14 days"));
        dateSelect.add(new TwoFacedObject(msg("Last month"), "-1 month"));



        if (request.exists(arg.getRelative())) {
            dateSelectValue = request.getString(arg.getRelative(), "");
        } else {
            dateSelectValue = "none";
        }

        String dateSelectInput = HtmlUtils.select(arg.getRelative(),
                                     dateSelect, dateSelectValue);
        String minDate = request.getDateSelect(arg.getFrom(), (String) null);
        String maxDate = request.getDateSelect(arg.getTo(), (String) null);


        String dateTypeValue = request.getString(arg.getMode(),
                                   DATE_SEARCHMODE_DEFAULT);
        String dateTypeInput = HtmlUtils.select(arg.getMode(), dateTypes,
                                   dateTypeValue);

        String noDataMode = request.getString(ARG_DATE_NODATAMODE, "");
        String noDateInput = HtmlUtils.checkbox(ARG_DATE_NODATAMODE,
                                 VALUE_NODATAMODE_INCLUDE,
                                 noDataMode.equals(VALUE_NODATAMODE_INCLUDE));
        String dateExtra;
        if (arg.getHasRange()) {
            dateExtra = HtmlUtils.makeToggleInline(msg("More..."),
                    HtmlUtils.br() + HtmlUtils.formTable(new String[] {
                msgLabel("Search for data whose time is"), dateTypeInput,
                msgLabel("Or search relative"), dateSelectInput, "",
                noDateInput + HtmlUtils.space(1)
                + msg("Include entries with no data times")
            }), false);
        } else {
            dateExtra = HtmlUtils.makeToggleInline(msg("More..."),
                    HtmlUtils.br()
                    + HtmlUtils.formTable(new String[] {
                        msgLabel("Or search relative"),
                        dateSelectInput }), false);


        }

        String fromField = repository.getDateHandler().makeDateInput(request,
                               arg.getFrom(), "searchform", null, null,
                               showTime);
        String toField = repository.getDateHandler().makeDateInput(request,
                             arg.getTo(), "searchform", null, null, showTime);
        /*
        basicSB.append(RepositoryManager.formEntryTop(request,
                                                      msgLabel(arg.getLabel()),
                                                       + HtmlUtils.space(1)
                        + HtmlUtils.img(repository.getIconUrl(ICON_RANGE))
                        + HtmlUtils.space(1)
                                                      +  + dateExtra));
        */

        basicSB.append(RepositoryManager.formEntryTop(request,
                msgLabel(arg.getLabel() + " - From"), fromField));
        basicSB.append(RepositoryManager.formEntryTop(request,
                msgLabel("To"), toField));
        basicSB.append(RepositoryManager.formEntryTop(request, "",
                dateExtra));

    }


    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public static String getSpatialSearchTypeWidget(Request request) {
        String radio = HtmlUtils.radio(
                           ARG_AREA_MODE, VALUE_AREA_OVERLAPS,
                           request.getString(
                               ARG_AREA_MODE, VALUE_AREA_OVERLAPS).equals(
                               VALUE_AREA_OVERLAPS)) + msg("Overlaps")
                                   + HtmlUtils.space(3)
                                   + HtmlUtils.radio(
                                       ARG_AREA_MODE, VALUE_AREA_CONTAINS,
                                       request.getString(
                                           ARG_AREA_MODE,
                                           VALUE_AREA_OVERLAPS).equals(
                                               VALUE_AREA_CONTAINS)) + msg(
                                                   "Contained by");

        return radio;
    }


    /**
     * _more_
     *
     * @param request The request
     * @param what _more_
     * @param sb _more_
     */
    public void addSearchField(Request request, String what, Appendable sb) {
        if (what.equals(ARG_FILESUFFIX)) {
            Utils.append(sb,
                         formEntry(request, msgLabel("File Suffix"),
                                   HtmlUtils.input(ARG_FILESUFFIX, "",
                                       " size=\"8\" ")));
        }
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isAnyHandler() {
        return getType().equals(TypeHandler.TYPE_ANY);
    }





    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Clause> assembleWhereClause(Request request)
            throws Exception {
        return assembleWhereClause(request, new StringBuilder());
    }



    /**
     * _more_
     *
     * @param request The request
     * @param searchCriteria _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Clause> assembleWhereClause(Request request,
                                            Appendable searchCriteria)
            throws Exception {

        //        Misc.printStack("Assemble where clause", 10);

        if (parent != null) {
            return parent.assembleWhereClause(request, searchCriteria);
        }

        List<Clause> where    = new ArrayList<Clause>();
        List         typeList = request.get(ARG_TYPE, new ArrayList());
        typeList.remove(TYPE_ANY);
        if (typeList.size() > 0) {
            if (request.get(ARG_TYPE_EXCLUDE, false)) {
                addCriteria(request, searchCriteria, "Entry Type!=",
                            StringUtil.join(",", typeList));
            } else {
                addCriteria(request, searchCriteria, "Entry Type=",
                            StringUtil.join(",", typeList));
            }
        }

        if (request.defined(ARG_RESOURCE)) {
            addCriteria(request, searchCriteria, "Resource=",
                        request.getString(ARG_RESOURCE, ""));
            String resource = request.getString(ARG_RESOURCE, "");
            resource = getStorageManager().resourceFromDB(resource);
            addOrClause(Tables.ENTRIES.COL_RESOURCE, resource, where);
        }

        if (request.defined(ARG_CATEGORY)) {
            addCriteria(request, searchCriteria, "Category=",
                        request.getString(ARG_CATEGORY, ""));
            addOrClause(Tables.ENTRIES.COL_DATATYPE,
                        request.getString(ARG_CATEGORY, ""), where);
        }

        if (request.defined(ARG_USER_ID)) {
            addCriteria(request, searchCriteria, "User=",
                        request.getString(ARG_USER_ID, ""));
            addOrClause(Tables.ENTRIES.COL_USER_ID,
                        request.getString(ARG_USER_ID, ""), where);
        }

        /**
         * if (request.defined(ARG_COLLECTION)) {
         *   Entry collectionEntry = getEntryManager().getEntry(request,
         *                               request.getString(ARG_COLLECTION,
         *                                   ""));
         *   if (collectionEntry != null) {
         *       addCriteria(request,searchCriteria, "Collection=",
         *                   collectionEntry.getName());
         *   } else {
         *       addCriteria(request,searchCriteria, "Collection=", "Unknown");
         *   }
         *   addOrClause(Tables.ENTRIES.COL_TOP_GROUP_ID,
         *               request.getString(ARG_COLLECTION, ""), where);
         *               }
         */

        if (request.defined(ARG_FILESUFFIX)) {
            addCriteria(request, searchCriteria, "File Suffix=",
                        request.getString(ARG_FILESUFFIX, ""));
            List<Clause> clauses = new ArrayList<Clause>();
            for (String tok :
                    (List<String>) StringUtil.split(
                        request.getString(ARG_FILESUFFIX, ""), ",", true,
                        true)) {
                clauses.add(Clause.like(Tables.ENTRIES.COL_RESOURCE,
                                        "%" + tok));
            }
            if (clauses.size() == 1) {
                where.add(clauses.get(0));
            } else {
                where.add(Clause.or(clauses));
            }
        }


        if (request.defined(ARG_GROUP)) {
            String  groupId = (String) request.getString(ARG_GROUP,
                                  "").trim();

            boolean doNot   = groupId.startsWith("!");
            if (doNot) {
                groupId = groupId.substring(1);
            }
            if (groupId.endsWith("%")) {
                System.err.println("%%");
                Entry group = getEntryManager().findGroup(request,
                                  groupId.substring(0, groupId.length() - 1));
                if (group != null) {
                    addCriteria(request, searchCriteria, "Folder=",
                                group.getName());
                }
                where.add(Clause.like(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                      groupId));
            } else {
                List<String> toks = StringUtil.split(groupId, "|", true,
                                        true);
                if (toks.size() > 1) {
                    List<Clause> ors = new ArrayList<Clause>();
                    for (String tok : toks) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                          tok));
                    }
                    where.add(Clause.or(ors));
                } else {
                    Entry group = getEntryManager().findGroup(request);
                    if (group == null) {
                        throw new IllegalArgumentException(
                            msgLabel("Could not find folder") + groupId);
                    }
                    addCriteria(request, searchCriteria, "Folder" + (doNot
                            ? "!="
                            : "="), group.getName());
                    String searchChildren =
                        (String) request.getString(ARG_GROUP_CHILDREN,
                            (String) null);
                    if (Misc.equals(searchChildren, "true")) {
                        Clause sub = (doNot
                                      ? Clause.notLike(
                                          Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                          group.getId() + Entry.IDDELIMITER
                                          + "%")
                                      : Clause.like(
                                          Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                          group.getId() + Entry.IDDELIMITER
                                          + "%"));
                        Clause equals = (doNot
                                         ? Clause
                                             .neq(Tables.ENTRIES
                                                 .COL_PARENT_GROUP_ID, group
                                                 .getId())
                                         : Clause
                                             .eq(Tables.ENTRIES
                                                 .COL_PARENT_GROUP_ID, group
                                                 .getId()));
                        where.add(Clause.or(sub, equals));
                    } else {
                        if (doNot) {
                            where.add(
                                Clause.neq(
                                    Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                    group.getId()));
                        } else {
                            where.add(
                                Clause.eq(
                                    Tables.ENTRIES.COL_PARENT_GROUP_ID,
                                    group.getId()));
                        }
                    }
                }
            }
        }


        List<Clause> dateClauses = new ArrayList<Clause>();
        for (DateArgument arg : getDateArgs()) {
            Date[] dateRange = request.getDateRange(arg.getFrom(),
                                   arg.getTo(), arg.getRelative(),
                                   new Date());
            if ((dateRange[0] != null) || (dateRange[1] != null)) {
                Date date1 = dateRange[0];
                Date date2 = dateRange[1];
                if (arg.forCreateDate() || arg.forChangeDate()) {
                    String column = arg.forCreateDate()
                                    ? Tables.ENTRIES.COL_CREATEDATE
                                    : Tables.ENTRIES.COL_CHANGEDATE;
                    if (date1 != null) {
                        addCriteria(request, searchCriteria,
                                    msg(arg.getLabel()) + ">=", date1);
                        dateClauses.add(Clause.ge(column, date1));
                    }
                    if (date2 != null) {
                        addCriteria(request, searchCriteria,
                                    msg(arg.getLabel()) + "<=", date2);
                        dateClauses.add(Clause.le(column, date2));
                    }

                    continue;
                }

                if (date1 == null) {
                    date1 = date2;
                }
                if (date2 == null) {
                    date2 = date1;
                }


                String dateSearchMode = request.getString(arg.getMode(),
                                            DATE_SEARCHMODE_DEFAULT);
                if (dateSearchMode.equals(DATE_SEARCHMODE_OVERLAPS)) {
                    addCriteria(request, searchCriteria, "To&nbsp;Date&gt;=",
                                date1);
                    addCriteria(request, searchCriteria,
                                "From&nbsp;Date&lt;=", date2);
                    dateClauses.add(Clause.le(Tables.ENTRIES.COL_FROMDATE,
                            date2));
                    dateClauses.add(Clause.ge(Tables.ENTRIES.COL_TODATE,
                            date1));
                } else if (dateSearchMode.equals(
                        DATE_SEARCHMODE_CONTAINEDBY)) {
                    addCriteria(request, searchCriteria,
                                "From&nbsp;Date&gt;=", date1);
                    addCriteria(request, searchCriteria, "To&nbsp;Date&lt;=",
                                date2);
                    dateClauses.add(Clause.ge(Tables.ENTRIES.COL_FROMDATE,
                            date1));
                    dateClauses.add(Clause.le(Tables.ENTRIES.COL_TODATE,
                            date2));
                } else {
                    //DATE_SEARCHMODE_CONTAINS
                    addCriteria(request, searchCriteria,
                                "From&nbsp;Date&lt;=", date1);
                    addCriteria(request, searchCriteria, "To&nbsp;Date&gt;=",
                                date2);
                    dateClauses.add(Clause.le(Tables.ENTRIES.COL_FROMDATE,
                            date1));
                    dateClauses.add(Clause.ge(Tables.ENTRIES.COL_TODATE,
                            date2));
                }
            }


            String noDataMode = request.getString(ARG_DATE_NODATAMODE, "");
            if (noDataMode.equals(VALUE_NODATAMODE_INCLUDE)
                    && (dateClauses.size() > 0)) {
                Clause dateClause = Clause.and(dateClauses);
                dateClauses = new ArrayList<Clause>();
                Clause allEqualClause =
                    Clause.and(
                        Clause.join(
                            Tables.ENTRIES.COL_CREATEDATE,
                            Tables.ENTRIES.COL_FROMDATE), Clause.join(
                                Tables.ENTRIES.COL_FROMDATE,
                                Tables.ENTRIES.COL_TODATE));

                dateClauses.add(allEqualClause);
                dateClauses.add(Clause.or(dateClause, allEqualClause));
                addCriteria(request, searchCriteria, "Include no data times",
                            "");
            }


        }






        if (dateClauses.size() > 1) {
            where.add(Clause.and(dateClauses));
        } else if (dateClauses.size() == 1) {
            where.add(dateClauses.get(0));
        }


        boolean contains = !(request.getString(
                               ARG_AREA_MODE, VALUE_AREA_OVERLAPS).equals(
                               VALUE_AREA_OVERLAPS));

        String[] areaCols = { Tables.ENTRIES.COL_NORTH,
                              Tables.ENTRIES.COL_WEST,
                              Tables.ENTRIES.COL_SOUTH,
                              Tables.ENTRIES.COL_EAST };
        boolean[]          areaLE    = { true, false, false, true };
        String[]           areaNames = { "North", "West", "South", "East" };
        Clause             areaClause;
        SelectionRectangle bbox = getSelectionBounds(request);
        bbox.normalizeLongitude();
        List<Clause> areaClauses = new ArrayList<Clause>();
        List<SelectionRectangle> rectangles =
            new ArrayList<SelectionRectangle>();

        /*
   160                 20
    +------------------+
 ---------+---------+---------+------------
       180/-180     0      180/-180
        */



        if (bbox.allDefined()) {
            addCriteria(request, searchCriteria, (contains
                    ? "Area contained by "
                    : "Area overlaps"), bbox.getNorth() + " "
                                        + bbox.getWest() + " "
                                        + bbox.getSouth() + " "
                                        + bbox.getEast());
        }

        //Check for a search crossing the dateline
        if (bbox.crossesDateLine()) {
            rectangles.add(new SelectionRectangle(bbox.getNorth(),
                    bbox.getWest(), bbox.getSouth(), 180));
            rectangles.add(new SelectionRectangle(bbox.getNorth(), -180,
                    bbox.getSouth(), bbox.getEast()));
        } else {
            rectangles.add(bbox);
        }


        for (SelectionRectangle rectangle : rectangles) {
            List<Clause> areaExpressions = new ArrayList<Clause>();

            if ( !contains) {
                if (rectangle.hasNorth()) {
                    areaClause = Clause.le(Tables.ENTRIES.COL_SOUTH,
                                           rectangle.getNorth());
                    areaExpressions.add(
                        Clause.and(
                            getSpatialDefinedClause(
                                Tables.ENTRIES.COL_NORTH), areaClause));
                }
                if (rectangle.hasSouth()) {
                    areaClause = Clause.ge(Tables.ENTRIES.COL_NORTH,
                                           rectangle.getSouth());
                    areaExpressions.add(
                        Clause.and(
                            getSpatialDefinedClause(
                                Tables.ENTRIES.COL_SOUTH), areaClause));
                }

                if (rectangle.hasWest()) {
                    areaClause = Clause.ge(Tables.ENTRIES.COL_EAST,
                                           rectangle.getWest());
                    areaExpressions.add(
                        Clause.and(
                            getSpatialDefinedClause(Tables.ENTRIES.COL_EAST),
                            areaClause));
                }
                if (rectangle.hasEast()) {
                    areaClause = Clause.le(Tables.ENTRIES.COL_WEST,
                                           rectangle.getEast());
                    areaExpressions.add(
                        Clause.and(
                            getSpatialDefinedClause(Tables.ENTRIES.COL_WEST),
                            areaClause));
                }
            } else {
                double[] values = rectangle.getValues();
                for (int i = 0; i < 4; i++) {
                    if (Double.isNaN(values[i])) {
                        continue;
                    }
                    double areaValue = values[i];
                    areaClause = areaLE[i]
                                 ? Clause.le(areaCols[i], areaValue)
                                 : Clause.ge(areaCols[i], areaValue);
                    areaExpressions.add(
                        Clause.and(
                            getSpatialDefinedClause(areaCols[i]),
                            areaClause));
                }
            }
            if (areaExpressions.size() > 0) {
                areaClauses.add(Clause.and(areaExpressions));
            }
        }



        if (areaClauses.size() == 1) {
            //            System.err.println("Single:" + areaClauses.get(0));
            where.add(areaClauses.get(0));
        } else if (areaClauses.size() > 1) {
            //            System.err.println("Multiple:" + areaClauses);
            where.add(Clause.or(areaClauses));
        }



        Hashtable args        = request.getArgs();
        String metadataPrefix = ARG_METADATA_ATTR1 + "_";
        Hashtable<String, List<Metadata>> typeMap = new Hashtable<String,
                                                        List<Metadata>>();
        List<String> types = new ArrayList<String>();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(metadataPrefix)) {
                continue;
            }
            if ( !request.defined(arg)) {
                continue;
            }
            String type = arg.substring(ARG_METADATA_ATTR1.length() + 1);
            List[] urlArgs = new List[] {
                                 request.get(ARG_METADATA_ATTR1 + "_" + type,
                                             new ArrayList<String>()),
                                 request.get(ARG_METADATA_ATTR2 + "_" + type,
                                             new ArrayList<String>()),
                                 request.get(ARG_METADATA_ATTR3 + "_" + type,
                                             new ArrayList<String>()),
                                 request.get(ARG_METADATA_ATTR4 + "_" + type,
                                             new ArrayList<String>()) };

            int index = 0;
            while (true) {
                boolean  ok         = false;
                String[] valueArray = { "", "", "", "" };
                for (int valueIdx = 0; valueIdx < urlArgs.length;
                        valueIdx++) {
                    if (index < urlArgs[valueIdx].size()) {
                        ok = true;
                        valueArray[valueIdx] =
                            (String) urlArgs[valueIdx].get(index);
                    }
                }
                if ( !ok) {
                    break;
                }
                index++;

                Metadata metadata = new Metadata(type, valueArray[0],
                                        valueArray[1], valueArray[2],
                                        valueArray[3], "");


                metadata.setInherited(request.get(ARG_METADATA_INHERITED
                        + "." + type, false));
                List<Metadata> values = typeMap.get(type);
                if (values == null) {
                    typeMap.put(type, values = new ArrayList<Metadata>());
                    types.add(type);
                }
                values.add(metadata);
            }
        }



        List<Clause> metadataAnds = new ArrayList<Clause>();
        for (int typeIdx = 0; typeIdx < types.size(); typeIdx++) {
            String         type     = types.get(typeIdx);
            List<Metadata> values   = typeMap.get(type);
            List<Clause>   attrOrs  = new ArrayList<Clause>();
            String         subTable = Tables.METADATA.NAME + "_" + typeIdx;
            for (Metadata metadata : values) {
                String       tmp      = "";
                List<Clause> attrAnds = new ArrayList<Clause>();
                for (int attrIdx = 1; attrIdx <= 4; attrIdx++) {
                    String attr = metadata.getAttr(attrIdx);
                    if (attr.trim().length() > 0) {
                        attrAnds.add(Clause.eq(subTable + ".attr" + attrIdx,
                                attr));
                        tmp = tmp + ((tmp.length() == 0)
                                     ? ""
                                     : " &amp; ") + attr;
                    }
                }

                Clause attrClause = Clause.and(attrAnds);
                attrOrs.add(attrClause);
                MetadataHandler handler =
                    getRepository().getMetadataManager().findMetadataHandler(
                        type);
                MetadataType metadataType = handler.findType(type);
                if (metadataType != null) {
                    addCriteria(request, searchCriteria,
                                metadataType.getLabel() + "=", tmp);
                }
            }

            List<Clause> subClauses = new ArrayList<Clause>();
            subClauses.add(Clause.join(subTable + ".entry_id",
                                       Tables.ENTRIES.COL_ID));
            subClauses.add(Clause.eq(subTable + ".type", type));
            subClauses.add(Clause.or(attrOrs));
            metadataAnds.add(Clause.and(subClauses));
        }

        if (metadataAnds.size() > 0) {
            if (isOrSearch(request)) {
                where.add(Clause.or(metadataAnds));
                //                System.err.println ("metadata:" +Clause.or(metadataAnds));
            } else {
                where.add(Clause.and(metadataAnds));
                //                System.err.println ("metadata:" +Clause.and(metadataAnds));
            }
        }



        String[] textArgs = { ARG_NAME, ARG_DESCRIPTION };
        String[] columns = { Tables.ENTRIES.COL_NAME,
                             Tables.ENTRIES.COL_DESCRIPTION };

        for (int textIdx = 0; textIdx < textArgs.length; textIdx++) {
            String value = request.getString(textArgs[textIdx],
                                             (String) null);
            if ( !Utils.stringDefined(value)) {
                continue;
            }
            if ( !request.get(ARG_EXACT, false)) {
                value = "%" + value + "%";
                where.add(
                    getDatabaseManager().makeLikeTextClause(
                        columns[textIdx], value, false));
            } else {
                where.add(Clause.eq(columns[textIdx], value, false));
            }

        }


        String textToSearch = (String) request.getString(ARG_TEXT, "").trim();
        //A hook to allow the database manager do its own text search based on the dbms type
        if (textToSearch.length() > 0) {
            addTextDbSearch(request, textToSearch, searchCriteria, where);
        }

        return where;
    }



    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public static SelectionRectangle getSelectionBounds(Request request) {
        String[] argPrefixes = { ARG_AREA, ARG_BBOX };
        double[] bbox = { Double.NaN, Double.NaN, Double.NaN, Double.NaN };
        for (String argPrefix : argPrefixes) {
            if (request.defined(argPrefix)) {
                List<String> toks =
                    StringUtil.split(request.getString(argPrefix, ""), ",",
                                     true, true);
                //n,w,s,e
                if (toks.size() == 4) {
                    for (int i = 0; i < 4; i++) {
                        bbox[i] = Double.parseDouble(toks.get(i));
                    }
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (request.defined(AREA_NWSE[i])) {
                bbox[i] = request.get(AREA_NWSE[i], 0.0);
            }
        }

        return new SelectionRectangle(bbox[0], bbox[1], bbox[2], bbox[3]);

    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    private Clause getSpatialDefinedClause(String column) {
        return Clause.neq(column, new Double(Entry.NONGEO));
    }



    /**
     * _more_
     *
     * @param request The request
     * @param group _more_
     * @param entries _more_
     * @param subGroups _more_
     * @param select _more_
     *
     * @throws Exception _more_
     */
    public void getChildrenEntries(Request request, Entry group,
                                   List<Entry> entries,
                                   List<Entry> subGroups, SelectInfo select)
            throws Exception {
        List<String> ids = getEntryManager().getChildIds(request, group,
                               select);
        List<Entry> myEntries   = new ArrayList<Entry>();
        List<Entry> mySubGroups = new ArrayList<Entry>();
        for (String id : ids) {
            Entry entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
                continue;
            }
            if (getEntryManager().handleEntryAsGroup(entry)) {
                mySubGroups.add(entry);
            } else {
                myEntries.add(entry);
            }
        }
        subGroups.addAll(postProcessEntries(request, mySubGroups));
        entries.addAll(postProcessEntries(request, myEntries));


    }

    /**
     * _more_
     *
     * @param request The request
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> postProcessEntries(Request request,
                                          List<Entry> entries)
            throws Exception {
        return entries;
    }



    /**
     * _more_
     *
     * @param request The request
     * @param textToSearch _more_
     * @param searchCriteria _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addTextDbSearch(Request request, String textToSearch,
                                Appendable searchCriteria, List<Clause> where)
            throws Exception {
        boolean doName = true;
        boolean doDesc = true;
        boolean doFile = true;
        if (textToSearch.startsWith("name:")) {
            doDesc       = false;
            doFile       = false;
            textToSearch = textToSearch.substring("name:".length());
        } else if (textToSearch.startsWith("description:")) {
            doName       = false;
            doFile       = false;
            textToSearch = textToSearch.substring("description:".length());
        } else if (textToSearch.startsWith("file:")) {
            doName       = false;
            doDesc       = false;
            textToSearch = textToSearch.substring("file:".length());
        }

        addTextDbSearch(request, textToSearch, searchCriteria, where, doName,
                        doDesc, doFile);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param textToSearch _more_
     * @param searchCriteria _more_
     * @param where _more_
     * @param doName _more_
     * @param doDesc _more_
     * @param doFile _more_
     *
     * @throws Exception _more_
     */
    public void addTextDbSearch(Request request, String textToSearch,
                                Appendable searchCriteria,
                                List<Clause> where, boolean doName,
                                boolean doDesc, boolean doFile)
            throws Exception {

        DatabaseManager dbm = getDatabaseManager();
        textToSearch = textToSearch.replaceAll("%20", " ");
        List<Clause> textOrs = new ArrayList<Clause>();
        for (String textTok :
                (List<String>) StringUtil.split(textToSearch, ",", true,
                    true)) {
            boolean doLike   = false;
            boolean doRegexp = false;
            if (request.get(ARG_ISREGEXP, false)) {
                doRegexp = true;
                addCriteria(request, searchCriteria, "Text regexp:",
                            textToSearch);
            } else if ( !request.get(ARG_EXACT, false)) {
                addCriteria(request, searchCriteria, "Text like", textTok);
                List tmp = StringUtil.split(textTok, ",", true, true);
                //                textTok = "%" + StringUtil.join("%,%", tmp) + "%";
                doLike = true;
            } else {
                addCriteria(request, searchCriteria, "Text =", textToSearch);
            }
            //            System.err.println (doLike +" toks:" + nameToks);
            List<Clause> ands      = new ArrayList<Clause>();
            boolean searchMetadata = request.get(ARG_SEARCHMETADATA, false);
            searchMetadata = false;
            String[]     attrCols = { Tables.METADATA.COL_ATTR1  /*,
                                        Tables.METADATA.COL_ATTR2,
                                        Tables.METADATA.COL_ATTR3,
                                        Tables.METADATA.COL_ATTR4*/
            };
            List<String> nameToks = Utils.splitWithQuotes(textTok);
            for (String nameTok : nameToks) {
                boolean nameDoLike = doLike;
                boolean doNot      = nameTok.startsWith("!");
                if (doNot) {
                    nameTok = nameTok.substring(1);
                }

                boolean doEquals = nameTok.startsWith("=");
                if (doEquals) {
                    nameTok    = nameTok.substring(1);
                    nameDoLike = false;
                }

                if (nameDoLike) {
                    nameTok = "%" + nameTok + "%";
                }
                List<Clause> ors     = new ArrayList<Clause>();
                List<Column> columns = getColumns();
                if (columns != null) {
                    for (Column column : columns) {
                        if (column.getCanSearch()
                                && column.getCanSearchText()
                                && column.isString()) {
                            ors.add(
                                dbm.makeLikeTextClause(
                                    column.getFullName(), nameTok, doNot));
                        }
                    }
                }

                if (searchMetadata) {
                    List<Clause> metadataOrs = new ArrayList<Clause>();
                    for (String attrCol : attrCols) {
                        if (doRegexp) {
                            metadataOrs.add(
                                getDatabaseManager().makeRegexpClause(
                                    attrCol, nameTok, doNot));
                        } else if (nameDoLike) {
                            metadataOrs.add(dbm.makeLikeTextClause(attrCol,
                                    nameTok, doNot));
                        } else {
                            metadataOrs.add(Clause.eq(attrCol, nameTok,
                                    doNot));
                        }
                    }
                    ors.add(
                        Clause.and(
                            Clause.or(metadataOrs),
                            Clause.join(
                                Tables.METADATA.COL_ENTRY_ID,
                                Tables.ENTRIES.COL_ID)));
                }
                if (doRegexp) {
                    if (doName) {
                        ors.add(
                            getDatabaseManager().makeRegexpClause(
                                Tables.ENTRIES.COL_NAME, nameTok, doNot));
                    }
                    if (doDesc) {
                        ors.add(
                            getDatabaseManager().makeRegexpClause(
                                Tables.ENTRIES.COL_DESCRIPTION, nameTok,
                                doNot));
                    }
                    if (doFile) {
                        ors.add(
                            getDatabaseManager().makeRegexpClause(
                                Tables.ENTRIES.COL_RESOURCE, nameTok, doNot));
                    }
                } else if (nameDoLike) {
                    if (doName) {
                        ors.add(
                            dbm.makeLikeTextClause(
                                Tables.ENTRIES.COL_NAME, nameTok, doNot));
                    }
                    if (doDesc) {
                        ors.add(
                            dbm.makeLikeTextClause(
                                Tables.ENTRIES.COL_DESCRIPTION, nameTok,
                                doNot));
                    }
                    if (doFile) {
                        ors.add(
                            dbm.makeLikeTextClause(
                                Tables.ENTRIES.COL_RESOURCE, nameTok, doNot));
                    }
                } else {
                    if (doName) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_NAME, nameTok,
                                          doNot));
                    }
                    if (doDesc) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_DESCRIPTION,
                                          nameTok, doNot));
                    }
                    if (doFile) {
                        ors.add(Clause.eq(Tables.ENTRIES.COL_RESOURCE,
                                          nameTok, doNot));
                    }

                }
                if (doNot) {
                    ands.add(Clause.and(ors));
                } else {
                    ands.add(Clause.or(ors));
                }
            }
            //            System.err.println("clauses:" + ands);
            if (ands.size() > 1) {
                //                System.err.println ("ands:" + ands);
                textOrs.add(Clause.and(ands));
            } else if (ands.size() == 1) {
                //                System.err.println ("ors:" + ands.get(0));
                textOrs.add(ands.get(0));
            }
        }
        //        System.err.println ("ors:" + textOrs.get(0));
        if (textOrs.size() > 1) {
            where.add(Clause.or(textOrs));
        } else if (textOrs.size() == 1) {
            where.add(textOrs.get(0));
        }



    }




    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    public boolean isOrSearch(Request request) {
        return request.getString("search.or", "false").equals("true");
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param stmt _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void setStatement(Entry entry, PreparedStatement stmt,
                             boolean isNew)
            throws Exception {}

    /**
     * _more_
     *
     *
     * @param isNew _more_
     * @param typeInserts _more_
     */
    public void getInsertSql(boolean isNew,
                             List<TypeInsertInfo> typeInserts) {
        if (parent != null) {
            parent.getInsertSql(isNew, typeInserts);
        }
    }

    /**
     * _more_
     *
     * @param request The request
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        if (parent != null) {
            parent.deleteEntry(request, statement, entry);
        }
    }


    /**
     * _more_
     *
     * @param request The request
     * @param statement _more_
     * @param id _more_
     * @param parentEntry _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, String id,
                            Entry parentEntry, Object[] values)
            throws Exception {
        if (parent != null) {
            parent.deleteEntry(request, statement, id, parentEntry, values);
        }
    }

    /**
     * _more_
     *
     * @param request The request
     *
     * @return _more_
     */
    protected List getTablesForQuery(Request request) {
        return getTablesForQuery(request, new ArrayList());
    }

    /**
     * _more_
     *
     * @param request The request
     * @param initTables _more_
     *
     * @return _more_
     */
    protected List getTablesForQuery(Request request, List initTables) {
        if (parent != null) {
            parent.getTablesForQuery(request, initTables);
        }
        if ( !initTables.contains(Tables.ENTRIES.NAME)) {
            initTables.add(Tables.ENTRIES.NAME);
        }

        return initTables;
    }



    /**
     * _more_
     *
     * @param columnName _more_
     * @param value _more_
     *
     * @return _more_
     */
    public Object convert(String columnName, String value) {
        if (parent != null) {
            return parent.convert(columnName, value);
        }

        return value;
    }

    /**
     * _more_
     *
     * @param map _more_
     *
     * @return _more_
     */
    public Object[] makeEntryValues(Hashtable map) {
        if (parent != null) {
            return parent.makeEntryValues(map);
        }

        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getValueNames() {
        if (parent != null) {
            return parent.getValueNames();
        }

        return null;
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param list _more_
     * @param quoteThem _more_
     *
     * @return _more_
     */
    protected boolean addOr(String column, String value, List list,
                            boolean quoteThem) {
        if ((value != null) && (value.trim().length() > 0)
                && !value.toLowerCase().equals("all")) {
            list.add("(" + SqlUtil.makeOrSplit(column, value, quoteThem)
                     + ")");

            return true;
        }

        return false;
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     * @param clauses _more_
     *
     * @return _more_
     */
    protected boolean addOrClause(String column, String value,
                                  List<Clause> clauses) {
        if ((value != null) && (value.trim().length() > 0)
                && !value.toLowerCase().equals("all")) {
            clauses.add(Clause.makeOrSplit(column, value));

            return true;
        }

        return false;
    }




    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * _more_
     *
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     */
    public String getFileTypeDescription(Request request, Entry entry) {
        try {
            String desc = msg(entry.getTypeHandler().getDescription());
            if ( !Utils.stringDefined(desc)) {
                desc = entry.getTypeHandler().getType();
            }

            if ( !entry.getTypeHandler().getType().equals(
                    TypeHandler.TYPE_FILE)) {
                String searchUrl =
                    HtmlUtils.href(
                        getSearchManager().URL_SEARCH_TYPE + "/"
                        + entry.getTypeHandler().getType(), desc,
                            HtmlUtils.cssClass("entry-type-search")
                            + HtmlUtils.attr(
                                HtmlUtils.ATTR_ALT,
                                msg(
                                "Search for entries of this type")) + HtmlUtils.attr(
                                    HtmlUtils.ATTR_TITLE,
                                    msg("Search for entries of this type")));

                return searchUrl;
            }
            String path = "";
            try {
                path = getPathForEntry(request, entry);
            } catch (Exception exc) {
                return "Error:" + exc;
            }
            String label = getLabelFromPath(path);
            String url   = getUrlFromPath(path);

            if ((label == null) && (url != null)) {
                desc = HtmlUtils.href(url, desc,
                                      HtmlUtils.attr("target", "_help"));
            } else if (label != null) {
                if (url != null) {
                    //                desc = desc  +" (" + HtmlUtils.href(url, label)+")";
                    desc = HtmlUtils.href(url, msg(label),
                                          HtmlUtils.attr("target", "_help"));
                } else {
                    //                desc = desc  +" (" +label+")";
                    desc = msg(label);
                }
            }

            return desc;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        if ( !Utils.stringDefined(description)) {
            return getType();
        }

        return description;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param property _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getTemplateContent(Request request, Entry entry,
                                     String property, String dflt)
            throws Exception {
        String template = getTypeProperty(property, (String) null);
        if (template != null) {
            return processDisplayTemplate(request, entry, template);
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public int matchValue(String arg, Object value, Entry entry) {
        return MATCH_UNKNOWN;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public TwoFacedObject getCategory(Entry entry) {
        return new TwoFacedObject(description, type);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getMapInfoBubble(Request request, Entry entry)
            throws Exception {
        return getBubbleTemplate(request, entry);
    }

    /**
     * _more_
     *
     * @param request The request
     * @param props _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getSimpleDisplay(Request request, Hashtable props,
                                   Entry entry)
            throws Exception {
        return null;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return type + " " + description;
    }

    /**
     *
     * @param value _more_
     */
    public void setDefaultCategory(String value) {
        defaultCategory = value;
    }

    /**
     *
     * @return _more_
     */
    public String getDefaultCategory() {
        return defaultCategory;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDefaultCategory() {
        return (defaultCategory != null) && (defaultCategory.length() > 0);
    }



    /** _more_ */
    private Hashtable<String, HashSet> columnEnumValues =
        new Hashtable<String, HashSet>();


    /**
     * _more_
     *
     * @param column _more_
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEnumValueKey(Column column, Entry entry) {
        return column.getName();
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryListName(Request request, Entry entry) {
        return getEntryName(entry);
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param entry _more_
     * @param theValue _more_
     *
     * @throws Exception _more_
     */
    protected void addEnumValue(Column column, Entry entry, String theValue)
            throws Exception {
        if ((theValue == null) || (theValue.length() == 0)) {
            return;
        }
        HashSet set = getEnumValuesInner(null, column, entry);
        set.add(theValue);
    }

    /**
     * _more_
     *
     *
     * @param request The request
     * @param column _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<TwoFacedObject> getEnumValues(Request request, Column column,
            Entry entry)
            throws Exception {
        HashSet              set  = getEnumValuesInner(request, column,
                                        entry);
        List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
        List                 tmp  = new ArrayList();
        tmp.addAll(set);

        for (String s : (List<String>) Misc.sort(tmp)) {
            String label = s;
            if (s.length() == 0) {
                label = "&lt;blank&gt;";
            }
            tfos.add(new TwoFacedObject(label,s));
        }

        return tfos;
    }

    /**
     * _more_
     *
     * @param request The request
     * @param entry _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFieldHtml(Request request, Entry entry, String name)
            throws Exception {
        //TODO: support name, desc, etc.
        return null;
    }


    /**
     * _more_
     *
     *
     * @param request The request
     * @param column _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private HashSet getEnumValuesInner(Request request, Column column,
                                       Entry entry)
            throws Exception {


        Clause clause = getEnumValuesClause(column, entry);
        if (request != null) {
            List<Clause> ands = new ArrayList<Clause>();
            for (Column otherCol : getColumns()) {
                if ( !otherCol.getCanSearch() || !otherCol.isEnumeration()) {
                    continue;
                }
                if (otherCol.equals(column)) {
                    continue;
                }
                String urlId = otherCol.getFullName();
                if (request.defined(urlId)) {
                    ands.add(Clause.eq(otherCol.getName(),
                                       request.getString(urlId, "")));
                }
            }
            if (ands.size() > 0) {
                if (clause == null) {
                    clause = Clause.and(ands);
                } else {
                    clause = Clause.and(clause, Clause.and(ands));
                }
                //                System.err.println("col:" + column + " Clause:" + clause);
            }
        }

        //Use the clause string as part of the key
        String  key = getEnumValueKey(column, entry) + ((clause == null)
                ? ""
                : "_" + clause);
        HashSet set = columnEnumValues.get(key);
        if (set != null) {
            return set;
        }


        long t1 = System.currentTimeMillis();
        Statement stmt = getRepository().getDatabaseManager().select(
                             SqlUtil.distinct(column.getName()),
                             column.getTableName(), clause);
        long t2 = System.currentTimeMillis();
        String[] values =
            SqlUtil.readString(
                getRepository().getDatabaseManager().getIterator(stmt), 1);
        long t3 = System.currentTimeMillis();


        //        Utils.printTimes("Key:"+ key +" times:",t1,t2,t3);
        set = new HashSet();
        set.addAll(Misc.toList(values));
        columnEnumValues.put(key, set);

        return set;
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Clause getEnumValuesClause(Column column, Entry entry)
            throws Exception {
        return null;
    }


    /**
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategory(String value) {
        this.category = value;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setSuperCategory(String value) {
        this.superCategory = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        if (Misc.equals(this.category, CATEGORY_DEFAULT)
                && (parent != null)) {
            return parent.getCategory();
        }

        return this.category;
    }


    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getSuperCategory() {
        if ((this.superCategory.length() == 0) && (parent != null)) {
            return parent.getSuperCategory();
        }

        return this.superCategory;
    }


    /**
     * _more_
     *
     * @param v _more_
     */
    public void setForUser(boolean v) {
        this.forUser = v;
    }


    /**
     *  Get the ForUser property.
     *
     *  @return The ForUser
     */
    public boolean getForUser() {
        if ( !forUser) {
            return false;
        } else {
            //Don't inherit the for user
            return true;
        }

        /*
        if (getParent() != null) {
            return getParent().getForUser();
        }
        return true;
        */
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean entryHasDefaultName(Entry entry) {
        return Misc.equals(getStorageManager().getFileTail(entry),
                           entry.getName());
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param index _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public TimeZone getTimeZone(Request request, Entry entry, int index)
            throws Exception {
        TimeZone timeZone = null;
        String   timezone = null;
        if (entry != null) {
            if (index >= 0) {
                timezone = entry.getValue(index, "");
            }
            if ( !Utils.stringDefined(timezone)) {
                timezone = getEntryUtil().getTimezone(entry);
            }
        }
        if (Utils.stringDefined(timezone)) {
            timeZone = TimeZone.getTimeZone(timezone);
        } else {
            timeZone = RepositoryUtil.TIMEZONE_DEFAULT;
        }

        return timeZone;
    }


    /**
     *  Set the SpecialSearch property.
     *
     *  @param value The new value for SpecialSearch
     */
    public void setSpecialSearch(SpecialSearch value) {
        specialSearch = value;
    }

    /**
     *  Get the SpecialSearch property.
     *
     *  @return The SpecialSearch
     */
    public SpecialSearch getSpecialSearch() {
        if (specialSearch == null) {
            specialSearch = new SpecialSearch(this);
        }

        return specialSearch;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryName(Entry entry) {
        return entry.getName();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private List<DateArgument> getDateArgs() {
        if (dateArgs == null) {
            List<DateArgument> tmp = new ArrayList<DateArgument>();
            for (DateArgument arg : DateArgument.SEARCH_ARGS) {
                tmp.add(arg);
            }
            List<String> from = StringUtil.split(
                                    getRepository().getProperty(
                                        "ramadda.arg.date.from", ""), ",",
                                            true, true);
            List<String> to = StringUtil.split(
                                  getRepository().getProperty(
                                      "ramadda.arg.date.to", ""), ",", true,
                                          true);
            List<String> mode = StringUtil.split(
                                    getRepository().getProperty(
                                        "ramadda.arg.date.mode", ""), ",",
                                            true, true);
            List<String> relative = StringUtil.split(
                                        getRepository().getProperty(
                                            "ramadda.arg.date.relative",
                                            ""), ",", true, true);
            for (int i = 0; i < from.size(); i++) {
                tmp.add(new DateArgument(DateArgument.TYPE_DATA, from.get(i),
                                         to.get(i), mode.get(i),
                                         relative.get(i)));
            }
            dateArgs = tmp;
        }

        return dateArgs;
    }




    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request The request
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {
        return null;
    }




    /**
     * _more_
     *
     * @param properties _more_
     * @param inner _more_
     */
    public static void addPropertyTags(Hashtable properties,
                                       Appendable inner) {
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String arg   = (String) keys.nextElement();
            String value = (String) properties.get(arg);
            Utils.append(inner,
                         XmlUtil.tag(TypeHandler.TAG_PROPERTY,
                                     XmlUtil.attrs(ATTR_NAME, arg,
                                         TypeHandler.ATTR_VALUE, value)));
        }
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param mapInfo _more_
     * @param sb _more_
     */
    public void initMapAttrs(Entry entry, MapInfo mapInfo,
                             StringBuilder sb) {}


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String pattern = ".*\\.ggp$";
        System.err.println(args[0].toLowerCase().matches(pattern));
    }




}

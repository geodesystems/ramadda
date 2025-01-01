/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.type;


import org.ramadda.repository.Entry;
import org.ramadda.repository.Repository;
import org.ramadda.repository.Request;
import org.ramadda.repository.Result;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.output.BulkDownloadOutputHandler;
import org.ramadda.repository.output.ZipOutputHandler;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.Element;

import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;


import java.io.File;

import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;


/**
 * Class description
 *
 *
 * @version        $version$, Fri, Aug 23, '13
 * @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class CollectionTypeHandler extends ExtensibleGroupTypeHandler {

    /** collection field id */
    public static final String FIELD_COLLECTION = "collection_id";

    /** shortcut to JQuery */
    public static final JQuery JQ = null;

    /** search argument */
    public static final String ARG_SEARCH = "search";

    /** field argument */
    public static final String ARG_FIELD = "field";

    /** request argument */
    public static final String ARG_REQUEST = "request";

    /** metadata type request */
    public static final String REQUEST_METADATA = "metadata";

    /** search type request */
    public static final String REQUEST_SEARCH = "search";

    /** download type request */
    public static final String REQUEST_DOWNLOAD = "download";

    /** bulk download type request */
    public static final String REQUEST_BULKDOWNLOAD = "bulkdownload";

    /** granule type property */
    public static final String PROP_GRANULE_TYPE = "granule_type";

    /** ZIP outputhandler */
    private ZipOutputHandler zipOutputHandler;

    /** bulk download output handler */
    private BulkDownloadOutputHandler bulkDownloadOutputHandler;

    /** granule columns */
    private List<Column> granuleColumns;

    /** granule type handler */
    private TypeHandler granuleTypeHandler;

    /** select arg */
    private String selectArg = "select";

    /** time to live cache */
    private TTLCache<Object, Object> cache = new TTLCache<Object,
                                                 Object>(60 * 60 * 1000);

    /** label cache */
    private Hashtable<String, Properties> labelCache = new Hashtable<String,
                                                           Properties>();

    /**
     * Create a new CollectionTypeHandler
     *
     * @param repository  the repository
     * @param entryNode  the xml
     *
     * @throws Exception can't create one
     */
    public CollectionTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * Clear the cache
     */
    @Override
    public synchronized void clearCache() {
        super.clearCache();
        cache.clearCache();
        labelCache = new Hashtable<String, Properties>();
    }

    /**
     * Get the BulkDownloadOutputHandler
     *
     * @return the BulkDownloadOutputHandler
     */
    public BulkDownloadOutputHandler getBulkDownloadOutputHandler() {
        if (bulkDownloadOutputHandler == null) {
            bulkDownloadOutputHandler =
                (BulkDownloadOutputHandler) getRepository()
                    .getOutputHandler(org.ramadda.repository.output
                        .BulkDownloadOutputHandler.class);
        }

        return bulkDownloadOutputHandler;
    }


    /**
     * Get the ZipOutputHandler
     *
     * @return the ZipOutputHandler
     */
    public ZipOutputHandler getZipOutputHandler() {
        if (zipOutputHandler == null) {
            zipOutputHandler =
                (ZipOutputHandler) getRepository().getOutputHandler(
                    org.ramadda.repository.output.ZipOutputHandler.class);
        }

        return zipOutputHandler;
    }

    /**
     * Get the granule columns
     *
     * @return  the columns
     *
     * @throws Exception  can't get the columns
     */
    public List<Column> getGranuleColumns() throws Exception {
        getGranuleTypeHandler();

        return granuleColumns;
    }


    /**
     * Get the collection id column
     *
     * @param column  the column
     *
     * @return the column id
     */
    public String getCollectionIdColumn(Column column) {
        return column.getTableName() + "." + FIELD_COLLECTION;
    }


    /**
     * Get the granule type handler
     *
     * @return  the granule type handler
     *
     * @throws Exception can't get it
     */
    public TypeHandler getGranuleTypeHandler() throws Exception {
        if (granuleTypeHandler == null) {
            granuleTypeHandler = getRepository().getTypeHandler(
                getTypeProperty(PROP_GRANULE_TYPE, ""));
            List<Column> tmp =
                new ArrayList<Column>(granuleTypeHandler.getColumns());
            List<Column> tmp2                = new ArrayList<Column>();
            Column       currentCollectionId = null;
            for (Column column : tmp) {
                if ( !column.getName().equals(FIELD_COLLECTION)) {
                    tmp2.add(column);
                } else {
                    currentCollectionId = column;
                }
            }
            granuleColumns = tmp2;
            //            System.err.println("granule columns:" + granuleColumns);
        }

        return granuleTypeHandler;
    }



    /**
     * Get the metadata as json
     *
     * @param request  the request
     * @param entry  the entry
     *
     * @return  the JSON
     *
     * @throws Exception problems
     */
    public Result getMetadataJson(Request request, Entry entry)
            throws Exception {
        int field = 0;
        for (int i = 0; i < 10; i++) {
            if (request.defined(ARG_FIELD + i)) {
                field = i;

                break;
            }
        }

        StringBuffer key = new StringBuffer("json::" + entry.getId()
                                            + ":field:");
        List<Clause> clauses = getClauses(request, entry, key);
        StringBuffer json    = (StringBuffer) cache.get(key);
        if (json == null) {
            Column column = granuleColumns.get(field);
            List<String> uniqueValues = getUniqueValues(entry, column,
                                            clauses);
            String nextColumnName = column.getLabel();
            String selectLabel = ":-- Select "
                                 + Utils.getArticle(nextColumnName) + " "
                                 + nextColumnName + " --";
            uniqueValues.add(0, selectLabel);
            json = new StringBuffer();
            json.append(JsonUtil.map(Utils.makeListFromValues("values",
                    JsonUtil.list(uniqueValues))));
            //System.err.println(json);
            cache.put(key, json);
        }

        return new Result(BLANK, json, JsonUtil.MIMETYPE);
    }


    /**
     * Make a metadata tree
     *
     * @param request  the request
     * @param entry    the entry
     * @param sb       the buffer to add to
     * @param colIdx   the column index
     * @param clauses  the search clauses
     *
     * @throws Exception problems
     */
    public void makeMetadataTree(Request request, Entry entry,
                                 StringBuffer sb, int colIdx,
                                 List<Clause> clauses)
            throws Exception {}


    /**
     * Make a metadata tree
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @throws Exception  problemos
     */
    public void makeMetadataTree(Request request, Entry entry)
            throws Exception {
        StringBuffer tree = new StringBuffer();
        tree.append("<ul>");
        for (Column column : granuleColumns) {
            List<Clause> clauses = new ArrayList<Clause>();
            List<String> uniqueValues = getUniqueValues(entry, column,
                                            clauses);
            for (String v : uniqueValues) {
                tree.append("<li> " + v);
            }
        }
        tree.append("</ul>");
    }




    /**
     * Get unique values for the search
     *
     * @param entry  the entry
     * @param column  the column
     * @param clauses  the search clauses
     *
     * @return  the unique values from the column
     *
     * @throws Exception  problem finding values
     */
    private List<String> getUniqueValues(Entry entry, Column column,
                                         List<Clause> clauses)
            throws Exception {
        clauses = new ArrayList<Clause>(clauses);
        List<String> uniqueValues = new ArrayList<String>();
        if (column != null) {
            clauses.add(Clause.eq(getCollectionIdColumn(column),
                                  entry.getId()));
            Statement stmt =
                getDatabaseManager().select(
                    SqlUtil.distinct(
                        column.getTableName() + "."
                        + column.getName()), column.getTableName(),
                                             Clause.and(clauses));
            List<String> dbValues =
                (List<String>) Misc.toList(
                    SqlUtil.readString(
                        getRepository().getDatabaseManager().getIterator(
                            stmt), 1));
            for (TwoFacedObject tfo : getValueList(entry, dbValues, column)) {
                uniqueValues.add(tfo.getId() + ":" + tfo.getLabel());
            }
        }

        return uniqueValues;
    }


    /**
     * Get the search clauses
     *
     * @param request  the request
     * @param entry    the entry
     * @param key      the entry cache key
     *
     * @return  the clauses
     *
     * @throws Exception  problems
     */
    public List<Clause> getClauses(Request request, Entry entry,
                                   Appendable key)
            throws Exception {
        System.err.println(Utils.getStack(10));
        List<Clause> clauses = new ArrayList<Clause>();
        for (int selectIdx = 0; selectIdx < granuleColumns.size();
                selectIdx++) {
            if ( !request.defined(selectArg + selectIdx)) {
                continue;
            }
            String column = granuleColumns.get(selectIdx).getName();
            String v      = request.getString(selectArg + selectIdx, "");
            clauses.add(Clause.eq(column, v));
            if (key != null) {
                key.append(column + "=" + v + ";");
            }
        }

        return clauses;
    }


    /**
     * Get the enumeration table for the column
     *
     * @param column  the column
     *
     * @return the Hashtable of enums
     *
     * @throws Exception problems
     */
    protected LinkedHashMap getColumnEnumTable(Column column)
            throws Exception {
        LinkedHashMap map       = column.getEnumTable();
        String        key       = column.getName() + ".values";
        String        vocabFile = getTypeProperty(key, (String) null);
        if (vocabFile != null) {
            Properties properties = labelCache.get(vocabFile);
            if (properties == null) {
                properties = new Properties();
                getRepository().loadProperties(properties, vocabFile);
                labelCache.put(vocabFile, properties);
            }
            map = new LinkedHashMap<String, String>();
            map.putAll(properties);
        }

        return map;
    }


    /**
     * Get the values list
     *
     * @param collectionEntry the collection entry
     * @param values the values
     * @param column  the column
     *
     * @return  a list of ids and labels as TwoFacedObjects
     *
     * @throws Exception  can't make list
     */
    public List<TwoFacedObject> getValueList(Entry collectionEntry,
                                             List values, Column column)
            throws Exception {
        LinkedHashMap<String, String> map  = getColumnEnumTable(column);
        List<TwoFacedObject>          tfos = new ArrayList<TwoFacedObject>();
        for (String value : (List<String>) values) {
            String label = map.get(value);
            if (label == null) {
                label = column.getEnumLabel(value);
                if (label == null) {
                    label = value;
                }
            }
            tfos.add(new TwoFacedObject(label.trim(), value));
        }
        TwoFacedObject.sort(tfos);

        return tfos;
    }


    /**
     * Add the selectors to the form
     *
     * @param request  the request
     * @param entry    the entry
     * @param sb       the form
     * @param formId   the formId
     * @param js       the Javascript
     *
     * @throws Exception  problems appending
     */
    public void addSelectorsToForm(Request request, Entry entry,
                                   Appendable sb, String formId,
                                   Appendable js)
            throws Exception {

        List<Column> columns = getGranuleColumns();
        for (int selectIdx = 0; selectIdx < columns.size(); selectIdx++) {
            Column column = columns.get(selectIdx);
            String key = "values::" + entry.getId() + "::" + column.getName();
            List   values = (List) cache.get(key);
            if (values == null) {
                Statement stmt =
                    getRepository().getDatabaseManager().select(
                        SqlUtil.distinct(
                            column.getTableName() + "."
                            + column.getName()), column.getTableName(),
                                Clause.eq(
                                    getCollectionIdColumn(column),
                                    entry.getId()));
                values = getValueList(
                    entry,
                    Misc.toList(
                        SqlUtil.readString(
                            getRepository().getDatabaseManager().getIterator(
                                stmt), 1)), column);
                values.add(0, new TwoFacedObject("-- Select "
                        + Utils.getArticle(column.getLabel()) + " "
                        + column.getLabel() + " --", ""));
                //                values.add(0, new TwoFacedObject("FOOBAR","foobar"));
                cache.put(key, values);
            }
            String selectId = formId + "_" + selectArg + selectIdx;
            String selectedValue = request.getString(selectArg + selectIdx,
                                       "");
            String selectBox =
                HtmlUtils.select(
                    selectArg + selectIdx, values, selectedValue,
                    " style=\"min-width:250px;max-width:250px;\" "
                    + HtmlUtils.attr("id", selectId));
            sb.append(HtmlUtils.formEntry(msgLabel(column.getLabel()),
                                          selectBox));
            js.append(JQ.change(JQ.id(selectId),
                                "return "
                                + HtmlUtils.call(formId + ".select",
                                    HtmlUtils.squote("" + selectIdx))));
        }
    }


    /**
     * Get unique column values
     *
     * @param entry    The entry
     * @param fieldIdx  the field index
     * @param column    the column
     * @param clauses   the search clauses
     * @param useCache  use the cache
     *
     * @return  the values
     *
     * @throws Exception can't get no satisfaction
     */
    public List<String> getUniqueColumnValues(Entry entry, Column column,
            List<Clause> clauses, boolean useCache)
            throws Exception {
        String key = "values::" + entry.getId() + "::col"
                     + column.getOffset();
        List<String> values = null;
        if (useCache) {
            values = (List<String>) cache.get(key);
        }
        if (values == null) {
            List<Column> columns = getGranuleColumns();
            clauses = new ArrayList<Clause>(clauses);
            clauses.add(Clause.eq(getCollectionIdColumn(column),
                                  entry.getId()));
            Clause       clause = Clause.and(clauses);
            List<String> tables = clause.getTableNames();
            for (int i = 0; i < tables.size() - 1; i++) {
                String table1 = tables.get(i);
                String table2 = tables.get(i + 1);
                clauses.add(Clause.join(table1 + ".id", table2 + ".id"));
            }
            clause = Clause.and(clauses);
            //            SqlUtil.debug = tables.size()>1;
            Statement stmt = getRepository().getDatabaseManager().select(
                                 SqlUtil.distinct(
                                     column.getTableName() + "."
                                     + column.getName()), tables, clause, "",
                                         -1);
            SqlUtil.debug = false;
            values = (List<String>) Misc.toList(
                SqlUtil.readString(
                    getRepository().getDatabaseManager().getIterator(stmt),
                    1));
            //            if(tables.size()>1) 
            //                System.err.println("Values:" + values);
            cache.put(key, values);
        }

        return values;
    }


    /**
     * Add the JSON selectors to the form
     *
     * @param request  the request
     * @param entry    the entry
     * @param sb       the form
     * @param formId   the form id
     * @param js       the javascript
     *
     * @throws Exception no can do
     */
    public void addJsonSelectorsToForm(Request request, Entry entry,
                                       Appendable sb, String formId,
                                       Appendable js)
            throws Exception {


        List firstValues = (List) cache.get("firstValues::" + entry.getId());
        if (firstValues == null) {
            Column column = granuleColumns.get(0);
            Statement stmt =
                getRepository().getDatabaseManager().select(
                    SqlUtil.distinct(
                        column.getTableName() + "."
                        + column.getName()), column.getTableName(),
                                             Clause.eq(
                                                 getCollectionIdColumn(
                                                     column), entry.getId()));
            firstValues = getValueList(
                entry,
                Misc.toList(
                    SqlUtil.readString(
                        getRepository().getDatabaseManager().getIterator(
                            stmt), 1)), granuleColumns.get(0));
            cache.put("firstValues::" + entry.getId(), firstValues);
        }


        for (int selectIdx = 0; selectIdx < granuleColumns.size();
                selectIdx++) {
            String column = granuleColumns.get(selectIdx).getName();
            String label  = granuleColumns.get(selectIdx).getLabel();
            List   values = new ArrayList();
            if (selectIdx == 0) {
                values.add(new TwoFacedObject("-- Select a " + label + " --",
                        ""));
                values.addAll(firstValues);
            } else {
                values.add(new TwoFacedObject("--", ""));
            }
            String selectId = formId + "_" + selectArg + selectIdx;
            String selectedValue = request.getString(selectArg + selectIdx,
                                       "");

            String selectBox = HtmlUtils.select(selectArg + selectIdx,
                                   values, selectedValue,
                                   " style=\"min-width:250px;\" "
                                   + HtmlUtils.attr("id", selectId));
            sb.append(HtmlUtils.formEntry(msgLabel(label), selectBox));
            js.append(JQ.change(JQ.id(selectId),
                                "return "
                                + HtmlUtils.call(formId + ".select",
                                    HtmlUtils.squote("" + selectIdx))));
        }
    }


    /**
     * Process the request
     *
     * @param request  the request
     * @param entry    the collection entry
     *
     * @return  the result
     *
     * @throws Exception  no good
     */
    public Result processRequest(Request request, Entry entry)
            throws Exception {
        String what = request.getString(ARG_REQUEST, (String) null);
        if (what == null) {
            return null;
        }
        if (what.equals(REQUEST_METADATA)) {
            return getMetadataJson(request, entry);
        }

        if (what.equals(REQUEST_SEARCH) || request.defined(ARG_SEARCH)) {
            StringBuilder json = new StringBuilder();
            getRepository().getJsonOutputHandler().makeJson(request,
                    processSearch(request, entry), json);

            //            System.err.println(json);
            return new Result("", json, "application/json");
        }

        if (what.equals(REQUEST_DOWNLOAD)) {
            return processDownloadRequest(request, entry);
        }

        if (what.equals(REQUEST_BULKDOWNLOAD)) {
            return processBulkDownloadRequest(request, entry);
        }

        return null;
    }

    /**
     * Process the bulk download script request
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return the script
     *
     * @throws Exception problems
     */
    public Result processBulkDownloadRequest(Request request, Entry entry)
            throws Exception {
        request.setReturnFilename(entry.getName() + "_download.sh");
        StringBuilder             sb   = new StringBuilder();
        BulkDownloadOutputHandler bdoh = getBulkDownloadOutputHandler();
        bdoh.process(request, sb, getEntryManager().getDummyGroup(),
                     processSearch(request, entry, true), false, true,
                     new HashSet<String>(), false);

        return new Result(
            "", sb, bdoh.getMimeType(BulkDownloadOutputHandler.OUTPUT_CURL));
    }

    /**
     * Process the download request
     *
     * @param request  the request
     * @param entry    the entry
     *
     * @return  the download result
     *
     * @throws Exception  a problem
     */
    public Result processDownloadRequest(Request request, Entry entry)
            throws Exception {
        request.setReturnFilename(entry.getName() + ".zip");

        return getZipOutputHandler().toZip(request, entry.getName(),
                                           processSearch(request, entry),
                                           false, false,false);
    }


    /**
     * Zip all the files
     *
     * @param request  the request
     * @param zipFileName  the zip file name
     * @param files  the files to zip
     *
     * @return  the zip result
     *
     * @throws Exception  can't do zip
     */
    public Result zipFiles(Request request, String zipFileName,
                           List<File> files)
            throws Exception {
        return getRepository().zipFiles(request, zipFileName, files);
    }



    /**
     * Get the HTML display for this type
     *
     * @param request  the Request
     * @param entry    the entry
     *
     * @return  the Result
     *
     * @throws Exception  problem getting the HTML
     */
    @Override
    public Result getHtmlDisplay(Request request, Entry entry,  Entries children)
            throws Exception {
        //Always call this to init things
        getGranuleTypeHandler();

        //Check if the user clicked on tree view, etc.
        if ( !isDefaultHtmlOutput(request)) {
            return null;
        }

        Result result = processRequest(request, entry);
        if (result != null) {
            return result;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getDescription());
        String formId = "selectform" + HtmlUtils.blockCnt++;

        sb.append(
            HtmlUtils.form(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                HtmlUtils.attr("id", formId)));
        sb.append(HtmlUtils.formTable());

        StringBuilder js = new StringBuilder();
        js.append("var " + formId + " = new SelectForm("
                  + HtmlUtils.squote(formId) + ","
                  + HtmlUtils.squote(entry.getId()) + ");\n");
        addJsonSelectorsToForm(request, entry, sb, formId, js);
        sb.append(js);

        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.p());
        sb.append(HtmlUtils.submit(ARG_SEARCH, "Submit",
                                   HtmlUtils.id(formId + ".search")));
        js.append(JQ.submit(JQ.id(formId + ".search"),
                            "return "
                            + HtmlUtils.call(formId + ".search", "")));
        sb.append(HtmlUtils.script(js.toString()));
        sb.append(HtmlUtils.formClose());


        return new Result(msg(getLabel()), sb);


    }


    /**
     * Add clauses
     *
     * @param request  the request
     * @param group    the collection
     * @param clauses  pre-existing conditions
     *
     * @throws Exception  can't add clauses
     */
    public void addClauses(Request request, Entry group, List<Clause> clauses)
            throws Exception {
        HashSet<String> seenTable = new HashSet<String>();
        for (int i = 0; i < granuleColumns.size(); i++) {
            Column column      = granuleColumns.get(i);
            String dbTableName = column.getTableName();
            if ( !seenTable.contains(dbTableName)) {
                //xxxxx             clauses.add(Clause.eq(dbTableName +"." + getCollectionIdColumn(column), group.getId()));
                clauses.add(Clause.eq(getCollectionIdColumn(column),
                                      group.getId()));
                clauses.add(Clause.join(Tables.ENTRIES.COL_ID,
                                        dbTableName + ".id"));
                seenTable.add(dbTableName);
            }
            String urlArg = selectArg + i;
            if (request.defined(urlArg)) {
                clauses.add(Clause.makeOrSplit(dbTableName + "."
                        + column.getName(), request.getString(urlArg)));
            }
        }
    }


    /**
     * Process the search request
     *
     * @param request  the request
     * @param group    the collection
     *
     * @return  the found entries
     *
     * @throws Exception  can't make search
     */
    public List<Entry> processSearch(Request request, Entry group)
            throws Exception {
        return processSearch(request, group, false);
    }


    /**
     * Process the search
     *
     * @param request  the request
     * @param group    the collection
     * @param checkForSelectedEntries check for selected entries
     *
     * @return  the list of entries
     *
     * @throws Exception  can't make search
     */
    public List<Entry> processSearch(Request request, Entry group,
                                     boolean checkForSelectedEntries)
            throws Exception {

        if (checkForSelectedEntries) {
            List<String> entryIds = (List<String>) request.get("entryselect",
                                        new ArrayList<String>());
            if (entryIds.size() > 0) {
                List<Entry> entries = new ArrayList<Entry>();
                for (String entryId : entryIds) {
                    Entry entry = getEntryManager().getEntry(request,
                                      entryId);
                    entries.add(entry);
                }

                return entries;
            }
        }
        List<Clause> clauses = new ArrayList<Clause>();
        addClauses(request, group, clauses);

        //Pass in false to say not to do lucene search if its enabled
        return getEntryManager().getEntriesFromDb(request, clauses,
						       getGranuleTypeHandler());
    }

    /**
     * Start the form
     *
     * @param request  the request
     * @param entry    the entry
     * @param sb       the form
     * @param js       javascript to add to
     *
     * @return  the form
     *
     * @throws Exception  problems appending
     */
    public String openForm(Request request, Entry entry, Appendable sb,
                           Appendable js)
            throws Exception {
        sb.append(HtmlUtils.importJS(getHtdocsUrl("/selectform.js")));
        String formId = "selectform" + HtmlUtils.blockCnt++;
        sb.append(
            HtmlUtils.formPost(
                request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                HtmlUtils.id(formId)));
        sb.append(HtmlUtils.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HtmlUtils.hidden(ARG_REQUEST, ""));
        js.append("var " + formId + " =  "
                  + HtmlUtils.call("new  SelectForm",
                                   HtmlUtils.jsMakeArgs(true, formId,
                                       entry.getId(), "select",
                                       formId + "_output_")) + "\n");

        return formId;
    }

    /**
     * Append the search results
     *
     * @param request  the request
     * @param entry    the entry
     * @param sb       the output HTML
     *
     * @throws Exception  problems appending
     */
    public void appendSearchResults(Request request, Entry entry,
                                    Appendable sb)
            throws Exception {
        if (request.exists(ARG_SEARCH)) {
            sb.append(HtmlUtils.p());
            List<Entry> entries = processSearch(request, entry);
            if (entries.size() == 0) {
                sb.append(msg("No entries found"));
            } else {
                sb.append("Found " + entries.size() + " results");
                sb.append(HtmlUtils.p());
                for (Entry child : entries) {
                    sb.append(getPageHandler().getBreadCrumbs(request,
                            child));
                    sb.append(HtmlUtils.br());
                }
            }
        }
    }



}

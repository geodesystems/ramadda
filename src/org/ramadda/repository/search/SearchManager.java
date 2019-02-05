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

package org.ramadda.repository.search;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;

import org.apache.lucene.document.Field;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;


import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import org.ramadda.repository.*;
import org.ramadda.repository.admin.*;

import org.ramadda.repository.auth.*;

import org.ramadda.repository.database.Tables;

import org.ramadda.repository.metadata.*;

import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;

import org.ramadda.util.OpenSearchUtil;

import org.ramadda.util.TTLObject;
import org.ramadda.util.Utils;
import org.ramadda.util.WadlUtil;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.lang.reflect.*;

import java.net.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import java.util.jar.*;



import java.util.regex.*;
import java.util.zip.*;




/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class SearchManager extends AdminHandlerImpl implements EntryChecker {

    /** _more_ */
    public static final String ARG_SEARCH_SUBMIT = "search.submit";

    /** _more_ */
    public static final String ARG_PROVIDER = "provider";

    /** _more_ */
    public static final String ARG_SEARCH_SUBSET = "search.subset";

    /** _more_ */
    public static final String ARG_SEARCH_SERVERS = "search.servers";


    /** _more_ */
    public final RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this,
                                                   "/search/do", "Search");



    /** _more_ */
    public final RequestUrl URL_SEARCH_FORM = new RequestUrl(this,
                                                  "/search/form", "Search");


    /** _more_ */
    public final RequestUrl URL_SEARCH_TYPE = new RequestUrl(this,
                                                  "/search/type",
                                                  "Search by Type");

    /** _more_ */
    public final RequestUrl URL_SEARCH_ASSOCIATIONS =
        new RequestUrl(this, "/search/associations/do",
                       "Search Associations");

    /** _more_ */
    public final RequestUrl URL_SEARCH_ASSOCIATIONS_FORM =
        new RequestUrl(this, "/search/associations/form",
                       "Search Associations");

    /** _more_ */
    public final RequestUrl URL_SEARCH_BROWSE = new RequestUrl(this,
                                                    "/search/browse",
                                                    "Browse Metadata");



    /** _more_ */
    public final RequestUrl URL_SEARCH_REMOTE_DO =
        new RequestUrl(this, "/search/remote/do", "Search Remote Servers");


    /** _more_ */
    public final List<RequestUrl> searchUrls =
        RequestUrl.toList(new RequestUrl[] { URL_SEARCH_FORM,
                                             URL_SEARCH_TYPE,
                                             URL_SEARCH_BROWSE,
                                             URL_SEARCH_ASSOCIATIONS_FORM });

    /** _more_ */
    public final List<RequestUrl> remoteSearchUrls =
        RequestUrl.toList(new RequestUrl[] { URL_SEARCH_FORM,
                                             URL_SEARCH_TYPE,
                                             URL_SEARCH_BROWSE,
                                             URL_SEARCH_ASSOCIATIONS_FORM });


    /** _more_ */
    private static final String FIELD_ENTRYID = "entryid";

    /** _more_ */
    private static final String FIELD_PATH = "path";

    /** _more_ */
    private static final String FIELD_CONTENTS = "contents";

    /** _more_ */
    private static final String FIELD_MODIFIED = "modified";

    /** _more_ */
    private static final String FIELD_DESCRIPTION = "description";

    /** _more_ */
    private static final String FIELD_METADATA = "metadata";

    /** _more_ */
    private IndexSearcher luceneSearcher;

    /** _more_ */
    private IndexReader luceneReader;

    /** _more_ */
    private boolean isLuceneEnabled = true;


    /** _more_ */
    private SearchProvider thisSearchProvider;

    /** _more_ */
    private List<SearchProvider> searchProviders = null;

    /** _more_ */
    private List<SearchProvider> allProviders;

    /** _more_ */
    private Hashtable<String, SearchProvider> searchProviderMap =
        new Hashtable<String, SearchProvider>();


    /** _more_ */
    private List<SearchProvider> pluginSearchProviders =
        new ArrayList<SearchProvider>();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public SearchManager(Repository repository) {
        super(repository);
        repository.addEntryChecker(this);
        isLuceneEnabled =
            getRepository().getProperty(PROP_SEARCH_LUCENE_ENABLED, false);
        getAdmin().addAdminHandler(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean includeMetadata() {
        return getRepository().getProperty(PROP_SEARCH_SHOW_METADATA, true);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isLuceneEnabled() {
        return isLuceneEnabled;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private IndexWriter getLuceneWriter() throws Exception {
        File indexFile = new File(getStorageManager().getIndexDir());
        IndexWriter writer =
            new IndexWriter(FSDirectory.open(indexFile),
                            new StandardAnalyzer(Version.LUCENE_CURRENT),
                            IndexWriter.MaxFieldLength.LIMITED);

        return writer;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<RequestUrl> getAdminUrls() {
        return null;
    }

    /**
     * _more_
     *
     * @param block _more_
     * @param asb _more_
     */
    public void addToAdminSettingsForm(String block, StringBuffer asb) {
        if ( !block.equals(Admin.BLOCK_ACCESS)) {
            return;
        }
        asb.append(HtmlUtils.colspan(msgHeader("Search"), 2));
        asb.append(
            HtmlUtils
                .formEntry(
                    "",
                    HtmlUtils
                        .checkbox(
                            PROP_SEARCH_LUCENE_ENABLED, "true",
                            isLuceneEnabled()) + HtmlUtils.space(2)
                                + msg("Enable Lucene Indexing and Search")));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyAdminSettingsForm(Request request) throws Exception {
        getRepository().writeGlobal(
            PROP_SEARCH_LUCENE_ENABLED,
            isLuceneEnabled = request.get(PROP_SEARCH_LUCENE_ENABLED, false));

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return "searchmanager";
    }

    /**
     * _more_
     *
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public synchronized void indexEntries(List<Entry> entries)
            throws Exception {
        IndexWriter writer = getLuceneWriter();
        for (Entry entry : entries) {
            indexEntry(writer, entry);
        }
        writer.optimize();
        writer.close();
        luceneReader   = null;
        luceneSearcher = null;
    }


    /**
     * _more_
     *
     * @param writer _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void indexEntry(IndexWriter writer, Entry entry)
            throws Exception {

        org.apache.lucene.document.Document doc =
            new org.apache.lucene.document.Document();
        String path = entry.getResource().getPath();
        doc.add(new Field(FIELD_ENTRYID, entry.getId(), Field.Store.YES,
                          Field.Index.NOT_ANALYZED));
        if ((path != null) && (path.length() > 0)) {
            doc.add(new Field(FIELD_PATH, path, Field.Store.YES,
                              Field.Index.NOT_ANALYZED));
        }

        StringBuilder metadataSB = new StringBuilder();
        getRepository().getMetadataManager().getTextCorpus(entry, metadataSB);
        entry.getTypeHandler().getTextCorpus(entry, metadataSB);
        doc.add(new Field(FIELD_DESCRIPTION,
                          entry.getName() + " " + entry.getDescription(),
                          Field.Store.NO, Field.Index.ANALYZED));

        if (metadataSB.length() > 0) {
            doc.add(new Field(FIELD_METADATA, metadataSB.toString(),
                              Field.Store.NO, Field.Index.ANALYZED));
        }

        doc.add(new Field(FIELD_MODIFIED,
                          DateTools.timeToString(entry.getStartDate(),
                              DateTools.Resolution.MINUTE), Field.Store.YES,
                                  Field.Index.NOT_ANALYZED));

        if (entry.isFile()) {
            addContentField(entry, doc, new File(path));
        }
        writer.addDocument(doc);
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param doc _more_
     * @param f _more_
     *
     * @throws Exception _more_
     */
    private void addContentField(Entry entry,
                                 org.apache.lucene.document.Document doc,
                                 File f)
            throws Exception {
        //org.apache.lucene.document.Document doc
        InputStream stream = getStorageManager().getFileInputStream(f);
        try {
            org.apache.tika.metadata.Metadata metadata =
                new org.apache.tika.metadata.Metadata();
            org.apache.tika.parser.AutoDetectParser parser =
                new org.apache.tika.parser.AutoDetectParser();
            org.apache.tika.sax.BodyContentHandler handler =
                new org.apache.tika.sax.BodyContentHandler();
            parser.parse(stream, handler, metadata);
            String contents = handler.toString();
            if ((contents != null) && (contents.length() > 0)) {
                doc.add(new Field(FIELD_CONTENTS, contents, Field.Store.NO,
                                  Field.Index.ANALYZED));
            }

            /*
            String[] names = metadata.names();
            for (String name : names) {
                String value = metadata.get(name);
                System.err.println(name +"=" + value);
                doc.add(new Field(name, value, Field.Store.YES,
                                  Field.Index.ANALYZED));
            }
            */
        } catch (Exception exc) {
            System.err.println("error harvesting corpus from:" + f);
            exc.printStackTrace();
        } finally {
            IOUtil.close(stream);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private IndexReader getLuceneReader() throws Exception {
        if (true) {
            return IndexReader.open(
                FSDirectory.open(
                    new File(getStorageManager().getIndexDir())), false);
        }
        if (luceneReader == null) {
            luceneReader = IndexReader.open(
                FSDirectory.open(
                    new File(getStorageManager().getIndexDir())), false);
        }

        return luceneReader;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private IndexSearcher getLuceneSearcher() throws Exception {
        if (luceneSearcher == null) {
            luceneSearcher = new IndexSearcher(getLuceneReader());
        }

        return luceneSearcher;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntrySuggest(Request request) throws Exception {
        String       string = request.getString("string", "");
        String       type   = request.getString("type", (String) null);
        List<String> names  = new ArrayList<String>();
        if (Utils.stringDefined(string)) {
            if (string.startsWith("name:")) {
                string = string.substring("name:".length());
            } else if (string.startsWith("description:")) {
                string = string.substring("description:".length());
            } else if (string.startsWith("file:")) {
                string = string.substring("file:".length());
            }
            Clause clause = getDatabaseManager().makeLikeTextClause(
                                Tables.ENTRIES.COL_NAME, string + "%", false);
            if (type != null) {
                clause = Clause.and(clause,
                                    Clause.eq(Tables.ENTRIES.COL_TYPE, type));
            }
            Statement statement = getDatabaseManager().select(
                                      "distinct " + Tables.ENTRIES.COL_NAME,
                                      Utils.makeList(Tables.ENTRIES.NAME),
                                      clause, "", 20);
            SqlUtil.Iterator iter =
                getDatabaseManager().getIterator(statement);
            ResultSet results;
            while ((results = iter.getNext()) != null) {
                names.add(results.getString(1));
            }
        }
        String json = Json.map("values", Json.list(names, true));

        return new Result("", new StringBuilder(json), "text/json");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param groups _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void processLuceneSearch(Request request, List<Entry> groups,
                                    List<Entry> entries)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        StandardAnalyzer analyzer =
            new StandardAnalyzer(Version.LUCENE_CURRENT);
        QueryParser qp = new MultiFieldQueryParser(Version.LUCENE_CURRENT,
                             new String[] { FIELD_DESCRIPTION,
                                            FIELD_METADATA,
                                            FIELD_CONTENTS }, analyzer);
        Query         query    = qp.parse(request.getString(ARG_TEXT, ""));
        IndexSearcher searcher = getLuceneSearcher();
        TopDocs       hits     = searcher.search(query, 100);
        ScoreDoc[]    docs     = hits.scoreDocs;
        for (int i = 0; i < docs.length; i++) {
            org.apache.lucene.document.Document doc =
                searcher.doc(docs[i].doc);
            String id = doc.get(FIELD_ENTRYID);
            if (id == null) {
                continue;
            }
            Entry entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
                continue;
            }
            if (entry.isGroup()) {
                groups.add(entry);
            } else {
                entries.add(entry);
            }
        }
    }


    /**
     * _more_
     *
     * @param entries _more_
     */
    public void entriesCreated(List<Entry> entries) {
        if ( !isLuceneEnabled()) {
            return;
        }
        try {
            indexEntries(entries);
        } catch (Exception exc) {
            logError("Error indexing entries", exc);
        }
    }



    /**
     * _more_
     *
     * @param entries _more_
     */
    public void entriesModified(List<Entry> entries) {
        if ( !isLuceneEnabled()) {
            return;
        }
        try {
            List<String> ids = new ArrayList<String>();
            for (Entry entry : entries) {
                ids.add(entry.getId());

            }
            entriesDeleted(ids);
            indexEntries(entries);
        } catch (Exception exc) {
            logError("Error adding entries to Lucene index", exc);
        }
    }


    /**
     * _more_
     *
     * @param ids _more_
     */
    public synchronized void entriesDeleted(List<String> ids) {
        if ( !isLuceneEnabled()) {
            return;
        }
        try {
            IndexWriter writer = getLuceneWriter();
            for (String id : ids) {
                writer.deleteDocuments(new Term(FIELD_ENTRYID, id));
            }
            writer.close();
        } catch (Exception exc) {
            logError("Error deleting entries from Lucene index", exc);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processCapabilities(Request request) throws Exception {
        return new Result("", "text/xml");
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processOpenSearch(Request request) throws Exception {

        Document doc  = XmlUtil.makeDocument();
        Element  root = OpenSearchUtil.getRoot();
        /*
   <ShortName>Web Search</ShortName>
   <Description>Use Example.com to search the Web.</Description>
   <Tags>example web</Tags>
   <Contact>admin@example.com</Contact>
        */
        OpenSearchUtil.addBasicTags(
            root, getRepository().getRepositoryName(),
            getRepository().getRepositoryDescription(),
            getRepository().getRepositoryEmail());
        ((Element) XmlUtil.create(
            OpenSearchUtil.TAG_IMAGE, root)).appendChild(
                XmlUtil.makeCDataNode(
                    root.getOwnerDocument(),
                    getPageHandler().getLogoImage(null), false));




        String url = request.getAbsoluteUrl(URL_ENTRY_SEARCH.toString());
        url = HtmlUtils.url(url, new String[] {
            ARG_OUTPUT, AtomOutputHandler.OUTPUT_ATOM.getId(), ARG_TEXT,
            OpenSearchUtil.MACRO_TEXT, ARG_BBOX, OpenSearchUtil.MACRO_BBOX,
            DateArgument.ARG_DATA.getFromArg(),
            OpenSearchUtil.MACRO_TIME_START, DateArgument.ARG_DATA.getToArg(),
            OpenSearchUtil.MACRO_TIME_END,
        }, false);


        XmlUtil.create(OpenSearchUtil.TAG_URL, root, "",
                       new String[] { OpenSearchUtil.ATTR_TYPE,
                                      "application/atom+xml",
                                      OpenSearchUtil.ATTR_TEMPLATE, url });

        String xml = XmlUtil.getHeader() + XmlUtil.toString(root);

        return new Result(xml, OpenSearchUtil.MIMETYPE);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchForm(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HtmlUtils.sectionOpen(null, false));
        makeSearchForm(request, sb);
        sb.append(HtmlUtils.sectionClose());

        return makeResult(request, msg("Search Form"), sb);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<RequestUrl> getSearchUrls() throws Exception {
        if (getRegistryManager().getEnabledRemoteServers().size() > 0) {
            //            return getRepository().remoteSearchUrls;
            return remoteSearchUrls;
        }

        return searchUrls;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getSearchUrl(Request request) {
        return request.makeUrl(URL_ENTRY_SEARCH, ARG_NAME, WHAT_ENTRIES);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getTextField(Request request) throws Exception {
        String value = (String) request.getString(ARG_TEXT, "");
        value = value.replaceAll("\"","&quot;");
        String textField =
            HtmlUtils.input(
                ARG_TEXT, value,
                HtmlUtils.attr("placeholder", msg(" Search text"))
                + HtmlUtils.id("searchinput") + HtmlUtils.SIZE_50
                + " autocomplete='off' autofocus ") + "\n<div id=searchpopup class=ramadda-popup></div>" + HtmlUtils.script("ramaddaSearchSuggestInit('searchinput');");

        return textField;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getSearchButtons(Request request) throws Exception {
        return HtmlUtils.submit(msg("Search"), ARG_SEARCH_SUBMIT);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void getFormOpen(Request request, Appendable sb)
            throws Exception {
        sb.append(
            HtmlUtils.form(
                getSearchUrl(request),
                makeFormSubmitDialog(sb, msg("Searching..."))
                + " name=\"searchform\" "));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void makeSearchForm(Request request, Appendable sb)
            throws Exception {
        getFormOpen(request, sb);
        sb.append(getTextField(request));
        sb.append(" ");
        sb.append(getSearchButtons(request));
        StringBuilder searchForm = new StringBuilder();
        makeSearchForm(request, searchForm, true, false);
        String        inner  = searchForm.toString();
        StringBuilder formSB = new StringBuilder();
        HtmlUtils.makeAccordian(formSB, msg("Search Options"), inner,
                                "ramadda-accordian", null);

        sb.append(HtmlUtils.insetDiv(formSB.toString(), 0, 0, 0, 0));
        sb.append(HtmlUtils.formClose());
    }


    /**
     *
     * _more_
     *
     * @param request _more_
     * @param typeSpecific _more_
     * @param sb _more_
     * @param addTextField _more_
     *
     * @throws Exception _more_
     */
    private void makeSearchForm(Request request, Appendable sb,
                                boolean typeSpecific, boolean addTextField)
            throws Exception {


        sb.append(HtmlUtils.open(HtmlUtils.TAG_DIV,
                                 HtmlUtils.cssClass("ramadda-search-form")));
        TypeHandler typeHandler = getRepository().getTypeHandler(request);


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtils.submitImage(getIconUrl(ICON_BLANK),
                                        ARG_SEARCH_SUBMIT, "",
                                        " style=\"display: none;\" "));

        String what = (String) request.getWhat(BLANK);
        if (what.length() == 0) {
            what = WHAT_ENTRIES;
        }


        List<String> titles   = new ArrayList<String>();
        List<String> contents = new ArrayList<String>();

        addSearchProviders(request, contents, titles);

        Object       oldValue = request.remove(ARG_RELATIVEDATE);
        List<Clause> where    = typeHandler.assembleWhereClause(request);
        if (oldValue != null) {
            request.put(ARG_RELATIVEDATE, oldValue);
        }

        typeHandler.addToSearchForm(request, titles, contents, where, true,
                                    false);

        long t1 = System.currentTimeMillis();
        if (includeMetadata()) {
            StringBuilder metadataSB = new StringBuilder();
            metadataSB.append(HtmlUtils.formTable());
            getMetadataManager().addToSearchForm(request, metadataSB);
            metadataSB.append(HtmlUtils.formTableClose());
            titles.add(msg("Advanced search options"));
            contents.add(metadataSB.toString());
        }
        long t2 = System.currentTimeMillis();
        //            System.err.println("metadata form:" + (t2-t1));

        /*            StringBuffer outputForm = new StringBuffer(HtmlUtils.formTable());
        String output = makeOutputSettings(request);
        outputForm.append(output);
        outputForm.append(HtmlUtils.formTableClose());
        contents.add(outputForm.toString());
        titles.add(msg("Output"));
        */



        //Pad the contents
        List<String> tmp = new ArrayList<String>();
        for (String c : contents) {
            tmp.add(HtmlUtils.insetDiv(c, 5, 10, 10, 0));
        }
        contents = tmp;

        if (addTextField) {
            sb.append(getTextField(request) + " "
                      + getSearchButtons(request));
        }

        StringBuilder formSB        = new StringBuilder();
        boolean       showProviders = request.get("show_providers", false);
        if (showProviders && (titles.size() == 1)) {
            sb.append(HtmlUtils.h3("Search Providers"));
            sb.append(contents.get(0));
        } else {
            HtmlUtils.makeAccordian(formSB, titles, contents, true,
                                    "ramadda-accordian", null);
        }
        sb.append(formSB.toString());
        sb.append(HtmlUtils.close(HtmlUtils.TAG_DIV));


    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeOutputSettings(Request request) throws Exception {
        Appendable outputForm  = new StringBuilder();
        List       orderByList = new ArrayList();
        orderByList.add(new TwoFacedObject(msg("None"), "none"));
        orderByList.add(new TwoFacedObject(msg("From Date"),
                                           SORTBY_FROMDATE));
        orderByList.add(new TwoFacedObject(msg("To Date"), SORTBY_TODATE));
        orderByList.add(new TwoFacedObject(msg("Create Date"),
                                           SORTBY_CREATEDATE));
        orderByList.add(new TwoFacedObject(msg("Name"), SORTBY_NAME));
        orderByList.add(new TwoFacedObject(msg("Size"), SORTBY_SIZE));

        String orderBy =
            HtmlUtils.select(ARG_ORDERBY, orderByList,
                             request.getString(ARG_ORDERBY,
                                 "none")) + HtmlUtils.checkbox(ARG_ASCENDING,
                                     "true",
                                     request.get(ARG_ASCENDING,
                                         false)) + HtmlUtils.space(1)
                                             + msg("ascending");
        outputForm.append(HtmlUtils.formEntry(msgLabel("Order By"), orderBy));
        outputForm.append(HtmlUtils.formEntry(msgLabel("Output"),
                HtmlUtils.select(ARG_OUTPUT, getOutputHandlerSelectList(),
                                 request.getString(ARG_OUTPUT, ""))));

        return outputForm.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param contents _more_
     * @param titles _more_
     *
     * @throws Exception _more_
     */
    private void addSearchProviders(Request request, List<String> contents,
                                    List<String> titles)
            throws Exception {
        boolean showProviders = request.get("show_providers", false);
        List<SearchProvider> searchProviders = getSearchProviders();
        List<ServerInfo> servers =
            getRegistryManager().getEnabledRemoteServers();
        if ((searchProviders.size() <= 1) && (servers.size() == 0)) {
            return;
        }
        StringBuilder providerSB        = new StringBuilder();

        List<String>  selectedProviders = new ArrayList<String>();
        for (String tok :
                (List<String>) request.get(ARG_PROVIDER,
                                           new ArrayList<String>())) {
            selectedProviders.addAll(StringUtil.split(tok, ",", true, true));
        }

        if (selectedProviders.size() == 0) {
            selectedProviders.add("this");
        }


        CategoryBuffer cats  = new CategoryBuffer();
        StringBuilder  extra = new StringBuilder();
        for (int i = 0; i < searchProviders.size(); i++) {
            SearchProvider searchProvider = searchProviders.get(i);
            boolean        selected       = false;
            if (selectedProviders.size() == 0) {
                selected = (i == 0);
            } else {
                selected = selectedProviders.contains(searchProvider.getId());
                if (selected) {
                    if (extra.length() > 0) {
                        extra.append(", ");
                    }
                    extra.append(searchProvider.getName());
                }
            }

            String cbxId = HtmlUtils.getUniqueId("cbx");
            String cbxCall =
                HtmlUtils.attr(HtmlUtils.ATTR_ONCLICK,
                               HtmlUtils.call("checkboxClicked",
                                   HtmlUtils.comma("event",
                                       HtmlUtils.squote(ARG_PROVIDER),
                                       HtmlUtils.squote(cbxId))));



            String anchor = HtmlUtils.anchorName(searchProvider.getId());
            String cbx = HtmlUtils.labeledCheckbox(ARG_PROVIDER,
                             searchProvider.getId(), selected,
                             cbxCall + HtmlUtils.id(cbxId),
                             searchProvider.getFormLabel(false)
                             + (showProviders
                                ? " -- " + searchProvider.getId()
                                : ""));
            cbx += anchor;
            cats.get(searchProvider.getCategory()).append(cbx);
            cats.get(searchProvider.getCategory()).append(HtmlUtils.br());
            cats.get(searchProvider.getCategory()).append("\n");
        }

        for (String cat : cats.getCategories()) {
            Appendable buff = cats.get(cat);
            if (cat.length() == 0) {
                /*
                buff.append(HtmlUtils.labeledCheckbox(ARG_PROVIDER,
                                                      "all", selectedProviders.contains("all"),"",
                                                      msg("All Search Providers")));
                */
                providerSB.append(buff.toString());
            } else {
                providerSB.append(
                    HtmlUtils.div(
                        cat,
                        HtmlUtils.cssClass(
                            "ramadda-search-provider-header")));
                providerSB.append(HtmlUtils.div(buff.toString(),
                        HtmlUtils.cssClass("ramadda-search-provider-list")));
            }
        }
        String title = msg("Search providers");
        if (extra.length() > 0) {
            title += HtmlUtils.space(4) + extra;
        }
        titles.add(title);
        contents.add(HtmlUtils.insetDiv(providerSB.toString(), 0, 20, 0, 0));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List getOutputHandlerSelectList() {
        List tfos = new ArrayList<TwoFacedObject>();
        for (OutputHandler outputHandler :
                getRepository().getOutputHandlers()) {
            for (OutputType type : outputHandler.getTypes()) {
                if (type.getIsForSearch()) {
                    tfos.add(new HtmlUtils.Selector(type.getLabel(),
                            type.getId(),
                            getRepository().getIconUrl(type.getIcon()), 3,
                            20, false));
                }
            }
        }

        return tfos;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchType(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<String> toks = StringUtil.split(request.getRequestPath(), "/",
                                             true, true);
        String lastTok = toks.get(toks.size() - 1);
        if (lastTok.equals("type")) {
            sb.append(HtmlUtils.sectionOpen(null, false));
            HtmlUtils.open(sb, "div", HtmlUtils.cssClass("ramadda-links"));
            addSearchByTypeList(request, sb);
            HtmlUtils.close(sb, "div");
            sb.append(HtmlUtils.sectionClose());
        } else {
            String      type        = lastTok;
            TypeHandler typeHandler = getRepository().getTypeHandler(type);
            Result result =
                typeHandler.getSpecialSearch().processSearchRequest(request,
                    sb);
            //Is it non-html?
            if (result != null) {
                return result;
            }
        }

        return makeResult(request, msg("Search by Type"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void addSearchByTypeList(Request request, StringBuffer sb)
            throws Exception {
        CategoryBuffer cb = new CategoryBuffer();


        for (String preload : EntryManager.PRELOAD_CATEGORIES) {
            cb.append(preload, "");
        }

        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            if ( !typeHandler.getForUser()) {
                continue;
            }
            if (typeHandler.isAnyHandler()) {
                continue;
            }
            int cnt = getEntryUtil().getEntryCount(typeHandler);
            if (cnt == 0) {
                continue;
            }
            String icon = typeHandler.getIconProperty(null);
            String img;
            if (icon == null) {
                icon = ICON_BLANK;
                img = HtmlUtils.img(typeHandler.getIconUrl(icon), "",
                                    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH,
                                        "16"));
            } else {
                img = HtmlUtils.img(typeHandler.getIconUrl(icon));
            }
            StringBuffer buff = new StringBuffer();

            buff.append("<li> ");
            buff.append(img);
            buff.append(" ");
            String label = typeHandler.getDescription() + " (" + cnt + ")";



            buff.append(HtmlUtils.href(getRepository().getUrlBase()
                                       + "/search/type/"
                                       + typeHandler.getType(), label));
            cb.append(typeHandler.getCategory(), buff);
        }
        getPageHandler().doTableLayout(request, sb, cb);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchInfo(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb, "Search Information",
                                     false);

        sb.append("<a name=entrytypes></a>");
        sb.append(HtmlUtils.b("Entry Types"));
        sb.append(
            HtmlUtils.open(
                "div",
                HtmlUtils.style("max-height: 300px;overflow-y:auto;")));
        sb.append(HtmlUtils.formTable());
        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            String link =
                HtmlUtils.href(URL_SEARCH_TYPE + "/" + typeHandler.getType(),
                               typeHandler.getType());
            sb.append(HtmlUtils.row(HtmlUtils.cols(link,
                    typeHandler.getDescription())));
        }
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.close("div"));


        sb.append(HtmlUtils.close("<p>"));
        sb.append("<a name=outputtypes></a>");
        sb.append(HtmlUtils.b("Output Types"));
        sb.append(
            HtmlUtils.open(
                "div",
                HtmlUtils.style("max-height: 300px;overflow-y:auto;")));
        sb.append(HtmlUtils.formTable());
        for (OutputHandler outputHandler :
                getRepository().getOutputHandlers()) {
            for (OutputType type : outputHandler.getTypes()) {
                sb.append(HtmlUtils.row(HtmlUtils.cols(type.getId(),
                        type.getLabel())));
            }
        }
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.close("div"));


        sb.append(HtmlUtils.close("<p>"));
        sb.append("<a name=metadatatypes></a>");
        sb.append(HtmlUtils.b("Metadata Types"));
        sb.append(
            HtmlUtils.open(
                "div",
                HtmlUtils.style("max-height: 300px;overflow-y:auto;")));
        sb.append(header(msg("Metadata Types")));
        sb.append(HtmlUtils.formTable());
        for (MetadataType type :
                getRepository().getMetadataManager().getMetadataTypes()) {
            if ( !type.getSearchable()) {
                continue;
            }
            sb.append(HtmlUtils.row(HtmlUtils.cols(type.getId(),
                    type.getName())));
        }
        sb.append(HtmlUtils.formTableClose());
        sb.append(HtmlUtils.close("div"));

        getPageHandler().sectionClose(request, sb);

        return makeResult(request, msg("Search Metadata"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchProviders(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        getPageHandler().sectionOpen(request, sb, "Search Providers", false);
        sb.append("<ul>");
        List<SearchProvider> searchProviders = getSearchProviders();
        for (SearchProvider provider : searchProviders) {
            sb.append("<li> ");
            sb.append(provider.getFormLabel(true));
        }
        sb.append("</ul>");
        getPageHandler().sectionClose(request, sb);

        return makeResult(request, msg("Search Providers"), sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchWadl(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        WadlUtil.openTag(sb);

        WadlUtil.closeTag(sb);


        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            String link =
                HtmlUtils.href(URL_SEARCH_TYPE + "/" + typeHandler.getType(),
                               typeHandler.getType());
            sb.append(HtmlUtils.row(HtmlUtils.cols(link,
                    typeHandler.getDescription())));
        }
        sb.append(HtmlUtils.formTableClose());


        sb.append(header(msg("Output Types")));
        sb.append(HtmlUtils.formTable());
        for (OutputHandler outputHandler :
                getRepository().getOutputHandlers()) {
            for (OutputType type : outputHandler.getTypes()) {
                sb.append(HtmlUtils.row(HtmlUtils.cols(type.getId(),
                        type.getLabel())));
            }
        }
        sb.append(HtmlUtils.formTableClose());


        sb.append(header(msg("Metadata Types")));
        sb.append(HtmlUtils.formTable());
        for (MetadataType type :
                getRepository().getMetadataManager().getMetadataTypes()) {
            if ( !type.getSearchable()) {
                continue;
            }
            sb.append(HtmlUtils.row(HtmlUtils.cols(type.getId(),
                    type.getName())));
        }
        sb.append(HtmlUtils.formTableClose());






        return makeResult(request, msg("Search Metadata"), sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param includeThis _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<ServerInfo> findServers(Request request, boolean includeThis)
            throws Exception {
        List<ServerInfo> servers = new ArrayList<ServerInfo>();
        for (String id :
                (List<String>) request.get(ATTR_SERVER, new ArrayList())) {
            if (id.equals(ServerInfo.ID_THIS) && !includeThis) {
                continue;
            }
            ServerInfo server = getRegistryManager().findRemoteServer(id);
            if (server == null) {
                continue;
            }
            servers.add(server);
        }

        return servers;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processRemoteSearch(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<String> servers = (List<String>) request.get(ATTR_SERVER,
                                   new ArrayList());
        sb.append(HtmlUtils.p());
        request.remove(ATTR_SERVER);

        boolean      didone   = false;
        StringBuffer serverSB = new StringBuffer();
        for (String id : servers) {
            ServerInfo server = getRegistryManager().findRemoteServer(id);
            if (server == null) {
                continue;
            }
            if ( !didone) {
                sb.append(header(msg("Selected Servers")));
            }
            serverSB.append(server.getHref(" target=\"server\" "));
            serverSB.append(HtmlUtils.br());
            didone = true;
        }

        if ( !didone) {
            sb.append(
                getPageHandler().showDialogNote(msg("No servers selected")));
        } else {
            sb.append(
                HtmlUtils.div(
                    serverSB.toString(),
                    HtmlUtils.cssClass(CSS_CLASS_SERVER_BLOCK)));
            sb.append(HtmlUtils.p());
        }
        sb.append(HtmlUtils.p());
        sb.append(header(msg("Search Results")));

        return makeResult(request, msg("Remote Form"), sb);

    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryBrowseSearchForm(Request request)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        HtmlUtils.open(sb, "div", HtmlUtils.cssClass("ramadda-links"));
        getMetadataManager().addToBrowseSearchForm(request, sb);
        HtmlUtils.close(sb, "div");

        return makeResult(request, msg("Search Form"), sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result makeResult(Request request, String title, Appendable sb)
            throws Exception {
        StringBuilder headerSB = new StringBuilder();
        getPageHandler().makeLinksHeader(request, headerSB, getSearchUrls(),
                                         "");
        headerSB.append(sb.toString());
        Result result = new Result(title, headerSB);

        return addHeaderToAncillaryPage(request, result);
    }

    /**
     * _more_
     *
     * @param provider _more_
     */
    public void addPluginSearchProvider(SearchProvider provider) {
        pluginSearchProviders.add(provider);
        searchProviderMap.put(provider.getType(), provider);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public SearchProvider getSearchProvider(String id) throws Exception {
        //Force the init
        List<SearchProvider> searchProviders = getSearchProviders();
        if (id.equals(ServerInfo.ID_THIS)) {
            return thisSearchProvider;
        }
        SearchProvider provider = searchProviderMap.get(id);
        if (provider == null) {
            List<ServerInfo> servers =
                getRegistryManager().getEnabledRemoteServers();
            for (ServerInfo server : servers) {
                if (server.getId().equals(id)) {
                    provider = new SearchProvider.RemoteSearchProvider(
                        getRepository(), server);
                    searchProviderMap.put(id, provider);
                }
            }
        }

        return provider;
    }



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<SearchProvider> getSearchProviders() throws Exception {
        if (searchProviders == null) {
            //            System.err.println("SearchManager.doSearch- making searchProviders");
            List<SearchProvider> tmp = new ArrayList<SearchProvider>();
            for (SearchProvider provider : pluginSearchProviders) {
                if (provider.isEnabled()) {
                    tmp.add(provider);
                }
            }
            searchProviders = tmp;
        }


        if (allProviders == null) {
            List<SearchProvider> tmp = new ArrayList<SearchProvider>();

            tmp.add(thisSearchProvider =
                new SearchProvider.RamaddaSearchProvider(getRepository(),
                    ServerInfo.ID_THIS, "This RAMADDA Repository"));
            for (ServerInfo server :
                    getRegistryManager().getEnabledRemoteServers()) {
                tmp.add(
                    new SearchProvider.RemoteSearchProvider(
                        getRepository(), server));
            }

            for (SearchProvider provider : tmp) {
                searchProviderMap.put(provider.getType(), provider);
            }
            tmp.addAll(searchProviders);
            allProviders = tmp;
        }

        return allProviders;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry>[] doSearch(Request request, SearchInfo searchInfo)
            throws Exception {

        HashSet<String> providers = new HashSet<String>();




        for (String arg :
                (List<String>) request.get(ARG_PROVIDER, new ArrayList())) {
            providers.addAll(StringUtil.split(arg, ",", true, true));
        }
        if (providers.size() == 0) {
            providers.add("this");
        }

        boolean     doAll      = providers.contains("all");

        List<Entry> folders    = new ArrayList<Entry>();
        List<Entry> entries    = new ArrayList<Entry>();
        List<Entry> allEntries = new ArrayList<Entry>();

        boolean     doSearch   = true;


        if (request.defined("entries")) {
            for (String id :
                    StringUtil.split(request.getString("entries", ""), ",",
                                     true, true)) {
                Entry e = getEntryManager().getEntry(request, id);
                if (e == null) {
                    continue;
                }
                allEntries.add(e);
            }
            doSearch = false;
        }

        if (doSearch) {
            List<SearchProvider> searchProviders =
                new ArrayList<SearchProvider>();
            for (SearchProvider searchProvider : getSearchProviders()) {
                if ( !doAll && (providers != null)
                        && (providers.size() > 0)) {
                    if ( !providers.contains(searchProvider.getId())) {
                        continue;
                    }
                }
                searchProviders.add(searchProvider);
            }



            final int[]     runnableCnt = { 0 };
            final boolean[] running     = { true };
            List<Runnable>  runnables   = new ArrayList<Runnable>();
            for (SearchProvider searchProvider : searchProviders) {
                Runnable runnable = makeRunnable(request, searchProvider,
                                        allEntries, searchInfo, running,
                                        runnableCnt);
                runnables.add(runnable);
            }

            runEm(runnables, running, runnableCnt);
        }

        if ( !request.exists(ARG_ORDERBY)) {
            for (Entry e : allEntries) {
                if (e.isGroup()) {
                    folders.add(e);
                } else {
                    entries.add(e);
                }
            }
        } else {
            entries = allEntries;
        }

        /**
         * for (SearchProvider searchProvider: searchProviders) {
         *   try {
         *       //                System.err.println("Searching:" +searchProvider.getId());
         *       List<Entry> results = searchProvider.getEntries(request,
         *                                 searchCriteriaSB);
         *       for (Entry e : results) {
         *           if (e.isGroup()) {
         *               folders.add(e);
         *           } else {
         *               entries.add(e);
         *           }
         *       }
         *   } catch (Exception exc) {
         *       getLogManager().logError("Searching provider:"
         *                                + searchProvider, exc);
         *   }
         * }
         */

        if ((folders.size() == 0) && (entries.size() == 0)) {
            if (request.defined(ARG_GROUP)) {
                String groupId = (String) request.getString(ARG_GROUP,
                                     "").trim();
                Entry theGroup = getEntryManager().findGroup(request,
                                     groupId);
                if ((theGroup != null)
                        && theGroup.getTypeHandler().isSynthType()) {
                    List<Entry> children =
                        getEntryManager().getChildrenAll(request, theGroup,
                            null);
                    for (Entry child : children) {
                        if (child.isGroup()) {
                            folders.add(child);
                        } else {
                            entries.add(child);
                        }
                    }
                }
            }
        }


        return (List<Entry>[]) new List[] { folders, entries };

    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntrySearch(Request request) throws Exception {

        if (request.get(ARG_WAIT, false)) {
            return getRepository().getMonitorManager().processEntryListen(
                request);
        }
        if (request.defined("submit_type.x")
                || request.defined(ARG_SEARCH_SUBSET)) {
            request.remove(ARG_OUTPUT);

            return processSearchForm(request);
        }

        boolean textSearch = isLuceneEnabled()
                             && request.getString(ARG_SEARCH_TYPE,
                                 "").equals(SEARCH_TYPE_TEXT);

        SearchInfo       searchInfo = new SearchInfo();
        boolean          searchThis = true;
        List<ServerInfo> servers    = null;

        ServerInfo       thisServer = getRepository().getServerInfo();
        boolean          doFrames   = request.get(ARG_DOFRAMES, false);

        List<Entry>      groups     = new ArrayList<Entry>();
        List<Entry>      entries    = new ArrayList<Entry>();

        long             t1         = System.currentTimeMillis();
        if (textSearch) {
            processLuceneSearch(request, groups, entries);
        } else if (searchThis) {
            List[] pair = doSearch(request, searchInfo);
            groups.addAll((List<Entry>) pair[0]);
            entries.addAll((List<Entry>) pair[1]);
        }
        int   total    = groups.size() + entries.size();
        long  t2       = System.currentTimeMillis();
        Entry theGroup = null;

        if (request.defined(ARG_GROUP)) {
            String groupId = (String) request.getString(ARG_GROUP, "").trim();
            theGroup = getEntryManager().findGroup(request, groupId);
        }

        request.remove(ARG_SEARCH_SUBMIT);
        boolean       foundAny = (groups.size() > 0) || (entries.size() > 0);

        StringBuilder header   = new StringBuilder();
        getPageHandler().makeLinksHeader(request, header, getSearchUrls(),
                                         "");
        getPageHandler().sectionOpen(request, header, "Search Results",
                                     false);
        makeSearchForm(request, header);
        if ( !foundAny) {
            header.append(
                getPageHandler().showDialogNote(msg("Sorry, nothing found")));
        }
        request.appendPrefixHtml(header.toString());

        if (theGroup == null) {
            theGroup = getEntryManager().getDummyGroup();
        }

        long t3 = System.currentTimeMillis();

        Result result =
            getRepository().getOutputHandler(request).outputGroup(request,
                                             request.getOutput(), theGroup,
                                             groups, entries);
        long   t4 = System.currentTimeMillis();

        Result r;
        if (theGroup.isDummy()) {
            r = addHeaderToAncillaryPage(request, result);
        } else {
            header.append(HtmlUtils.sectionOpen());
            r = getEntryManager().addEntryHeader(request, theGroup, result);
        }

        //                System.err.println("search:  #: " + total + " doSearch: " + (t2 - t1)
        //                                   + " makeForm:" + (t3 - t2) + " outputGroup:"
        //                                   + (t4 - t3));
        //        System.err.println(total + "," + (t4 - t3));
        return r;
    }


    /*
            if (doFrames) {
                String linkUrl = request.getUrlArgs();
                request.put(ARG_DECORATE, "false");
                request.put(ATTR_TARGET, "_server");
                String       embeddedUrl = request.getUrlArgs();
                StringBuffer sb          = new StringBuffer();
                sb.append(msgHeader("Remote Server Search Results"));
                for (ServerInfo server : servers) {
                    String remoteSearchUrl = server.getUrl()
                                             + URL_ENTRY_SEARCH.getPath()
                                             + "?" + linkUrl;
                    sb.append("\n");
                    sb.append(HtmlUtils.p());
                    String link = HtmlUtils.href(remoteSearchUrl,
                                      server.getUrl());
                    String fullUrl = server.getUrl()
                                     + URL_ENTRY_SEARCH.getPath() + "?"
                                     + embeddedUrl;
                    String content =
                        HtmlUtils.tag(
                            HtmlUtils.TAG_IFRAME,
                            HtmlUtils.attrs(
                                HtmlUtils.ATTR_WIDTH, "100%",
                                HtmlUtils.ATTR_HEIGHT, "200",
                                HtmlUtils.ATTR_SRC,
                                fullUrl), "need to have iframe support");
                    sb.append(HtmlUtils.makeShowHideBlock(server.getLabel()
                            + HtmlUtils.space(2) + link, content, true));

                    sb.append("\n");
                }
                request.remove(ARG_DECORATE);
                request.remove(ARG_TARGET);

                return new Result("Remote Search Results", sb);
            }


    */



    /**
     * _more_
     *
     * @param request _more_
     * @param servers _more_
     * @param tmpEntry _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void doDistributedSearch(Request request,
                                    List<ServerInfo> servers,
                                    final Entry tmpEntry,
                                    final List<Entry> entries)
            throws Exception {
        request = request.cloneMe();
        ServerInfo      thisServer  = getRepository().getServerInfo();
        final int[]     runnableCnt = { 0 };
        final boolean[] running     = { true };
        //TODO: We need to cap the number of servers we're searching on
        List<Runnable> runnables = new ArrayList<Runnable>();
        for (ServerInfo server : servers) {
            if (server.equals(thisServer)) {
                continue;
            }

            Runnable runnable = makeRunnable(request, server, tmpEntry,
                                             entries, running, runnableCnt);

            runnables.add(runnable);
        }


        runnableCnt[0] = runnables.size();
        for (Runnable runnable : runnables) {
            Misc.runInABit(0, runnable);
        }


        //Wait at most 10 seconds for all of the thread to finish
        long t1 = System.currentTimeMillis();
        while (true) {
            synchronized (runnableCnt) {
                if (runnableCnt[0] <= 0) {
                    break;
                }
            }
            //Busy loop
            Misc.sleep(100);
            if (runnables.size() > 1) {
                long t2 = System.currentTimeMillis();
                //Wait at most 10 seconds
                if ((t2 - t1) > 20000) {
                    logInfo("Remote search waited too long");

                    break;
                }
            }
        }
        running[0] = false;
    }


    /**
     * _more_
     *
     * @param runnables _more_
     * @param running _more_
     * @param runnableCnt _more_
     *
     * @throws Exception _more_
     */
    private void runEm(List<Runnable> runnables, boolean[] running,
                       int[] runnableCnt)
            throws Exception {
        runnableCnt[0] = runnables.size();
        for (Runnable runnable : runnables) {
            Misc.runInABit(0, runnable);
        }

        //Wait at most 10 seconds for all of the thread to finish
        long t1 = System.currentTimeMillis();
        while (true) {
            synchronized (runnableCnt) {
                if (runnableCnt[0] <= 0) {
                    break;
                }
            }
            //Busy loop
            Misc.sleep(100);
            if (runnables.size() > 1) {
                long t2 = System.currentTimeMillis();
                //Wait at most 20 seconds
                if ((t2 - t1) > 20000) {
                    logInfo("Remote search waited too long");

                    break;
                }
            }
        }
        running[0] = false;
    }



    /**
     * _more_
     *
     *
     * @param theRequest _more_
     * @param serverInfo _more_
     * @param tmpEntry _more_
     * @param entries _more_
     * @param running _more_
     * @param runnableCnt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Runnable makeRunnable(final Request theRequest,
                                 final ServerInfo serverInfo,
                                 final Entry tmpEntry,
                                 final List<Entry> entries,
                                 final boolean[] running,
                                 final int[] runnableCnt)
            throws Exception {


        final Request request = theRequest.cloneMe();
        request.put(ARG_OUTPUT, XmlOutputHandler.OUTPUT_XML);
        final Entry parentEntry =
            new Entry(getRepository().getGroupTypeHandler(), true);
        final String serverUrl = serverInfo.getUrl();
        parentEntry.setId(getEntryManager().getRemoteEntryId(serverUrl, ""));
        getEntryManager().cacheEntry(parentEntry);
        parentEntry.setRemoteServer(serverInfo);
        parentEntry.setUser(getUserManager().getAnonymousUser());
        parentEntry.setParentEntry(tmpEntry);
        parentEntry.setName(serverUrl);
        final String linkUrl  = request.getUrlArgs();
        Runnable     runnable = new Runnable() {
            public void run() {
                String remoteSearchUrl = serverUrl
                                         + URL_ENTRY_SEARCH.getPath() + "?"
                                         + linkUrl;

                System.err.println("Remote URL:" + remoteSearchUrl);
                try {
                    String entriesXml =
                        getStorageManager().readSystemResource(
                            new URL(remoteSearchUrl));
                    //                        System.err.println(entriesXml);
                    if ((running != null) && !running[0]) {
                        return;
                    }
                    Element  root     = XmlUtil.getRoot(entriesXml);
                    NodeList children = XmlUtil.getElements(root);
                    //Synchronize on the list so only one thread at  a time adds its entries to it
                    for (int i = 0; i < children.getLength(); i++) {
                        Element node = (Element) children.item(i);
                        //                    if (!node.getTagName().equals(TAG_ENTRY)) {continue;}
                        Entry entry =
                            getEntryManager().createEntryFromXml(request,
                                node, parentEntry, new Hashtable(), false,
                                false);

                        //                            entry.setName("remote:" + entry.getName());
                        entry.setResource(new Resource("remote:"
                                + XmlUtil.getAttribute(node, ATTR_RESOURCE,
                                    ""), Resource.TYPE_REMOTE_FILE));
                        String id = XmlUtil.getAttribute(node, ATTR_ID);
                        entry.setId(
                            getEntryManager().getRemoteEntryId(
                                serverUrl, id));
                        entry.setRemoteServer(serverInfo);
                        entry.setRemoteUrl(serverUrl + "/entry/show?entryid="
                                           + id);
                        getEntryManager().cacheEntry(entry);
                        synchronized (entries) {
                            entries.add((Entry) entry);
                        }
                    }
                } catch (Exception exc) {
                    logException("Error doing search:" + remoteSearchUrl,
                                 exc);
                } finally {
                    if (runnableCnt != null) {
                        synchronized (runnableCnt) {
                            runnableCnt[0]--;
                        }
                    }
                }
            }

            public String toString() {
                return "Runnable:" + serverUrl;
            }
        };

        return runnable;

    }


    /**
     * _more_
     *
     * @param theRequest _more_
     * @param provider _more_
     * @param entries _more_
     * @param searchInfo _more_
     * @param running _more_
     * @param runnableCnt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Runnable makeRunnable(final Request theRequest,
                                 final SearchProvider provider,
                                 final List<Entry> entries,
                                 final SearchInfo searchInfo,
                                 final boolean[] running,
                                 final int[] runnableCnt)
            throws Exception {
        final Request request  = theRequest.cloneMe();
        Runnable      runnable = new Runnable() {
            public void run() {
                try {
                    //                        System.err.println("start search:"+ provider.getName());
                    List<Entry> results = provider.getEntries(request,
                                              searchInfo);
                    //                        System.err.println("end search:"+ provider.getName());
                    synchronized (entries) {
                        entries.addAll(results);
                    }
                } catch (Exception exc) {
                    logException("Error doing search:" + provider, exc);
                } finally {
                    if (runnableCnt != null) {
                        synchronized (runnableCnt) {
                            runnableCnt[0]--;
                        }
                    }
                }
            }
        };

        return runnable;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getSearchFormLinks(Request request, String what)
            throws Exception {
        TypeHandler typeHandler = getRepository().getTypeHandler(request);
        List        links       = new ArrayList();
        String      extra1      = " class=subnavnolink ";
        String      extra2      = " class=subnavlink ";
        String[]    whats       = { WHAT_ENTRIES, WHAT_TAG,
                                    WHAT_ASSOCIATION };
        String[]    names       = { LABEL_ENTRIES, "Tags", "Associations" };

        String      formType    = request.getString(ARG_FORM_TYPE, "basic");

        for (int i = 0; i < whats.length; i++) {
            String item;
            if (what.equals(whats[i])) {
                item = HtmlUtils.span(names[i], extra1);
            } else {
                item = HtmlUtils.href(request.makeUrl(URL_SEARCH_FORM,
                        ARG_WHAT, whats[i], ARG_FORM_TYPE,
                        formType), names[i], extra2);
            }
            if (i == 0) {
                item = "<span " + extra1
                       + ">Search For:&nbsp;&nbsp;&nbsp; </span>" + item;
            }
            links.add(item);
        }

        List<TwoFacedObject> whatList = typeHandler.getListTypes(false);
        for (TwoFacedObject tfo : whatList) {
            if (tfo.getId().equals(what)) {
                links.add(HtmlUtils.span(tfo.toString(), extra1));
            } else {
                links.add(HtmlUtils.href(request.makeUrl(URL_SEARCH_FORM,
                        ARG_WHAT, BLANK + tfo.getId(), ARG_TYPE,
                        typeHandler.getType()), tfo.toString(), extra2));
            }
        }

        return links;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String f : args) {
            InputStream stream = new FileInputStream(f);
            org.apache.tika.metadata.Metadata metadata =
                new org.apache.tika.metadata.Metadata();
            org.apache.tika.parser.AutoDetectParser parser =
                new org.apache.tika.parser.AutoDetectParser();
            org.apache.tika.sax.BodyContentHandler handler =
                new org.apache.tika.sax.BodyContentHandler(100000000);
            parser.parse(stream, handler, metadata);
            String contents = handler.toString();
            //            System.out.println("contents: " + contents);
        }
    }



}

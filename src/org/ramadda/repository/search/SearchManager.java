/*
 * Copyright (c) 2008-2021 Geode Systems LLC
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
import org.ramadda.util.CategoryList;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.JQuery;
import org.ramadda.util.Json;

import org.ramadda.util.OpenSearchUtil;

import org.ramadda.util.TTLObject;
import org.ramadda.util.Utils;
import org.ramadda.util.WadlUtil;

import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;
import org.ramadda.util.SelectionRectangle;
import java.util.function.Function;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.*;
import java.nio.file.*;

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


import java.util.concurrent.*;
import org.ramadda.repository.job.JobManager;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;

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
							     "/search/form", "Form");


    /** _more_ */
    public final RequestUrl URL_SEARCH_TYPE = new RequestUrl(this,
							     "/search/type", "By Type");

    /** _more_ */
    public final RequestUrl URL_SEARCH_ASSOCIATIONS =
        new RequestUrl(this, "/search/associations/do", "Associations");

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


    private static final String  FIELD_ENTRYORDER ="entryorder";
    private static final String  FIELD_SIZE ="size";
    private static final String  FIELD_SUPERTYPE ="supertype";    

    /** _more_ */
    private static final String FIELD_ENTRYID = "entryid";
    private static final String FIELD_PARENT = "parent";
    private static final String FIELD_ANCESTOR = "ancestor";    

    /** _more_ */
    private static final String FIELD_PATH = "path";

    private static final String FIELD_TYPE = "type";

    private static final String FIELD_NORTH = "north";
    private static final String FIELD_WEST = "west";    
    private static final String FIELD_SOUTH = "south";
    private static final String FIELD_EAST = "east";

    /** _more_ */
    private static final String FIELD_CONTENTS = "contents";

    /** _more_ */
    private static final String FIELD_DATE_CREATED = "date_created";
    private static final String FIELD_DATE_CHANGED = "date_changed";
    private static final String FIELD_DATE_START = "date_start";
    private static final String FIELD_DATE_END = "date_end";    

    /** _more_ */
    private static final String FIELD_METADATA = "metadata";

    private static final String FIELD_PROPERTY = "property";


    /** _more_ */
    private static final String FIELD_DESCRIPTION = "description";

    /** _more_ */
    private static final String FIELD_NAME = "name";

    private static final String FIELD_NAME_SORT = "namesort";    


    private static final String[] SEARCH_FIELDS ={FIELD_NAME, FIELD_DESCRIPTION, FIELD_CONTENTS,FIELD_PATH};

    public static final int LUCENE_MAX_LENGTH = 10000000;

    private IndexWriter luceneWriter;

    /** _more_ */
    private boolean isLuceneEnabled = true;


    /** _more_ */
    private SearchProvider thisSearchProvider;

    /** _more_ */
    private List<SearchProvider> searchProviders = null;

    /** _more_ */
    private List<SearchProvider> allProviders;

    /** _more_ */
    private Hashtable<String, SearchProvider> searchProviderMap =     new Hashtable<String, SearchProvider>();


    /** _more_ */
    private List<SearchProvider> pluginSearchProviders =
        new ArrayList<SearchProvider>();


    private TikaConfig tikaConfig;
    
    private Object luceneMutex = new Object();

    /**
     * _more_
     *
     * @param repository _more_
     */
    public SearchManager(Repository repository) {
        super(repository);
        repository.addEntryChecker(this);
        getAdmin().addAdminHandler(this);
    }


    @Override
    public void initAttributes() {
        super.initAttributes();
        isLuceneEnabled =
            getRepository().getProperty(PROP_SEARCH_LUCENE_ENABLED, false);
	try {
	    tikaConfig = new TikaConfig(getClass().getResourceAsStream("/org/ramadda/repository/resources/tika-config.xml"));
	} catch(Exception exc) {
	    System.err.println("Error calling TikaConfig:" + exc);
	}
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


    public TikaConfig getTikaConfig() {
	return tikaConfig;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private IndexWriter getLuceneWriter() throws Exception {
	if(luceneWriter==null) {
	    Directory index = new NIOFSDirectory(Paths.get(getStorageManager().getIndexDir()));
	    IndexWriterConfig config = new IndexWriterConfig();
	    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
	    luceneWriter = new IndexWriter(index, config);
	}
	return luceneWriter;
    }


    public void reindexLucene(Object actionId, boolean all)  {
	try {
	    IndexWriter writer = getLuceneWriter();
	    try {
		reindexLuceneInner(writer, actionId, all);
	    } finally {
		//		writer.close();
	    }
	} catch(Throwable thr) {
	    throw new RuntimeException(thr);
	}
    }

    private void reindexLuceneInner(final IndexWriter writer, Object actionId, boolean all)  throws Throwable {	
        Statement statement =
            getDatabaseManager().select(Tables.ENTRIES.COL_ID,
					Misc.newList(Tables.ENTRIES.NAME),
					null);

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
	List<String> ids = new ArrayList<String>();
	IndexSearcher searcher = null;
	while ((results = iter.getNext()) != null) {
            String id = results.getString(1);
	    if(!all) {
		if(searcher==null) {
		    IndexReader reader =  DirectoryReader.open(writer);
		    searcher = new IndexSearcher(reader);
		}
		Query query = new TermQuery(new Term(FIELD_ENTRYID, id));
		TopDocs       hits     = searcher.search(query, 1);
		ScoreDoc[]    docs     = hits.scoreDocs;
		if(docs.length>0) {
		    //		    System.err.println("skipping:" + id);
		    continue;
		} else {
		    //		    System.err.println("not skipping:"  + id);
		}
	    }
	    ids.add(id);
	    //	    if(ids.size()>10000) break;
	}

	//	System.err.println("ids:" + ids.size());

	if(all) {
	    writer.deleteAll();
	    writer.commit();
	}

	Object mutex = new Object();
	//Really 4
	int numThreads = 1;
	List<List> idLists;
	if(numThreads==1) {
	    idLists = new ArrayList<List>();
	    idLists.add(ids);
	} else {
	    idLists = Utils.splitList(ids,ids.size()/numThreads);
	}
	int []cnt =new int[]{0};
	boolean[]ok = new boolean[]{true};
	List<Callable<Boolean>> callables = new ArrayList<Callable<Boolean>>();
	for(List idList:idLists) {
	    callables.add(makeReindexer((List<String>)idList,writer,ids.size(),cnt,actionId,mutex,ok));
	}
	long t1 = System.currentTimeMillis();
	getRepository().getJobManager().invokeAllAndWait(callables);
	long t2 = System.currentTimeMillis();
	System.err.println("time:" + (t2-t1));
	if(ok[0]) {
	    System.err.println("committing");
	    writer.commit();
	}
	//	System.err.println("closing");
	//        writer.close();
	getActionManager().actionComplete(actionId);
    }


    private Callable<Boolean> makeReindexer(final List<String> ids, final IndexWriter writer,final int total, final int[] cnt, final Object actionId, final Object mutex, final boolean[]ok) throws Exception {
        return  new Callable<Boolean>() {
            public Boolean call() {
                try {
		    for(String id: ids) {
			if(!getRepository().getActive()) return true;
			Entry entry = getEntryManager().getEntry(null, id,false);
			if(entry==null) continue;
			synchronized(mutex) {
			    cnt[0]++;
			    System.err.println("#" + cnt[0] +"/"+ total +" entry:" + entry.getName());
			}
			/*
			if(entry.getParentEntry()==null) {
			    if(entry.getId().equals("2e485e95-eb29-44fc-8987-76e6ac74365a")) {
				System.err.println("*************** top:" + entry +"  "+ entry.getId());
			    } else {
				cnt[0]++;
				System.err.println(cnt[0]+" missing:" + entry +"  "+ entry.getId());
			    }
			    }*/
			indexEntry(writer, entry);
			getEntryManager().removeFromCache(entry);
			//			if(true) continue;
			if(!ok[0]) break;
			if(actionId!=null) {
			    if(!getActionManager().getActionOk(actionId)) {
				ok[0] =false;
				break;
			    }
			    synchronized(mutex) {
				getActionManager().setActionMessage(actionId,
								    "Reindexed " + cnt[0] +" entries");
			    }
			}
		    }
                    return Boolean.TRUE;
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
        };
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
    @Override
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
    @Override
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

    public String getMetadataField(String  type) {
	return FIELD_METADATA+"_"+ type;
    }

    public String getPropertyField(TypeHandler  handler,String  type) {
	return FIELD_PROPERTY+"_"+ handler.getType()+"_"+type;
    }    



    /**
     * _more_
     *
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private  void indexEntries(List<Entry> entries)
	throws Exception {
	synchronized(luceneMutex) {
	    IndexWriter writer = getLuceneWriter();
	    try {
		for (Entry entry : entries) {
		    indexEntry(writer, entry);
		}
		//        writer.optimize();
		writer.commit();
	    } finally {
		//	    writer.close();
	    }
	}
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

        doc.add(new StringField(FIELD_ENTRYID, entry.getId(), Field.Store.YES));
	//        doc.add(new StringField(FIELD_TYPE, entry.getTypeHandler().getType(), Field.Store.YES));	
	TypeHandler parentType = entry.getTypeHandler();//.getParent();
	while(parentType!=null) {
	    doc.add(new StringField(FIELD_SUPERTYPE, parentType.getType(), Field.Store.YES));	
	    parentType = parentType.getParent();
	}


	if(entry.getParentEntryId()!=null) {
	    doc.add(new StringField(FIELD_PARENT, entry.getParentEntryId(), Field.Store.YES));	
	    Entry parent = entry;
	    while(parent!=null) {
		doc.add(new StringField(FIELD_ANCESTOR, parent.getId(), Field.Store.YES));	
		parent = parent.getParentEntry();
	    }
	}
        doc.add(new SortedNumericDocValuesField(FIELD_SIZE, entry.getResource().getFileSize()));
        doc.add(new SortedNumericDocValuesField(FIELD_ENTRYORDER, entry.getEntryOrder()));
	if(entry.hasAreaDefined()) {
	    doc.add(new DoublePoint(FIELD_NORTH, entry.getNorth()));
	    doc.add(new DoublePoint(FIELD_WEST, entry.getWest()));
	    doc.add(new DoublePoint(FIELD_SOUTH, entry.getSouth()));
	    doc.add(new DoublePoint(FIELD_EAST, entry.getEast()));
	} else if(entry.hasLocationDefined()) {
	    doc.add(new DoublePoint(FIELD_NORTH, entry.getLatitude()));
	    doc.add(new DoublePoint(FIELD_WEST, entry.getLongitude()));
	    doc.add(new DoublePoint(FIELD_SOUTH, entry.getLatitude()));
	    doc.add(new DoublePoint(FIELD_EAST, entry.getLongitude()));
	}



        String path = entry.getResource().getPath();
        if ((path != null) && (path.length() > 0)) {
	    if(entry.getResource().isFile()) {
		path = getStorageManager().getFileTail(entry);
	    }
            doc.add(new TextField(FIELD_PATH, path.toLowerCase(), Field.Store.NO));
        }

        doc.add(new TextField(FIELD_NAME,  entry.getName().toLowerCase(),Field.Store.YES));
	doc.add(new SortedDocValuesField(FIELD_NAME_SORT, new BytesRef(entry.getName())));

	StringBuilder desc = new StringBuilder();
        entry.getTypeHandler().getTextCorpus(entry, desc);
        doc.add(new TextField(FIELD_DESCRIPTION, desc.toString().toLowerCase(),Field.Store.NO));


	List<Column> columns = entry.getTypeHandler().getColumns();
	if (columns != null) {
	    Object[] values = entry.getTypeHandler().getEntryValues(entry);
	    if(values!=null) {
		for (Column column : columns) {
		    if (!column.getCanSearch()) continue;
		    Object v= column.getObject(values);
		    if(v==null) continue;
		    String field  = getPropertyField(entry.getTypeHandler(),column.getName());
		    if(column.isEnumeration()) 
			doc.add(new StringField(field, v.toString(),Field.Store.YES));
		    else if(column.isDouble()) 
			doc.add(new DoublePoint(field, (Double)v));
		    else if(column.isInteger()) 
			doc.add(new IntPoint(field, (Integer)v));		    
		    else
			doc.add(new TextField(field, v.toString(),Field.Store.NO));
		}
	    }
	}


	//	try {
        for (Metadata metadata : getMetadataManager().getMetadata(entry)) {
	    MetadataType type = getMetadataManager().getType(metadata);
	    if(type==null) {
		//		System.err.println("Null type:" + entry.getName() +"  "+ metadata);
		continue;
	    }
	    if(type.getSearchable()) {
		doc.add(new StringField(getMetadataField(type.getId()), metadata.getAttr1(),Field.Store.NO));
	    }
	}
	//	} catch(Exception exc) {
	    //	    exc.printStackTrace();
	    //	    System.exit(0);
	//	}


        StringBuilder metadataSB = new StringBuilder();
        getRepository().getMetadataManager().getTextCorpus(entry, metadataSB);
        if (metadataSB.length() > 0) {

        }

	doc.add(new SortedNumericDocValuesField(FIELD_DATE_CREATED, entry.getCreateDate()));	
	doc.add(new SortedNumericDocValuesField(FIELD_DATE_CHANGED, entry.getChangeDate()));
	doc.add(new SortedNumericDocValuesField(FIELD_DATE_START, entry.getStartDate()));
	doc.add(new SortedNumericDocValuesField(FIELD_DATE_END, entry.getEndDate()));	


        if (entry.isFile()) {
            addContentField(entry, doc, entry.getResource().getTheFile());
        }
        writer.addDocument(doc);
    }


    private String readContents(File f) throws Exception {
	//Don't do really big files or images
	if(f.length()>LUCENE_MAX_LENGTH) return null;
	if(Utils.isImage(f.toString())) return null;
	try(InputStream stream = getStorageManager().getFileInputStream(f)) {
	    //	    System.err.println("SearchManager.readContents:" + f.getName());
            org.apache.tika.metadata.Metadata metadata =
                new org.apache.tika.metadata.Metadata();
	    Parser parser = new org.apache.tika.parser.AutoDetectParser(tikaConfig);
            org.apache.tika.sax.BodyContentHandler handler =
                new org.apache.tika.sax.BodyContentHandler(LUCENE_MAX_LENGTH);
            parser.parse(stream, handler, metadata,new org.apache.tika.parser.ParseContext());
	    //	    System.err.println("done");
            return  handler.toString();
	}  catch(Throwable exc) {
	    System.err.println("Error reading contents:" + f.getName() +" error:" + exc);
	    exc.printStackTrace();
	    return null;
	}
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
        try {
            String contents = readContents(f);
	    //	    System.out.println(f+"\n" + contents);
            if ((contents != null) && (contents.length() > 0)) {
                doc.add(new TextField(FIELD_CONTENTS, contents, Field.Store.NO));
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
            System.err.println("SearchManager: error harvesting corpus from:" + f);
            exc.printStackTrace();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private IndexSearcher getLuceneSearcher() throws Exception {
	return new IndexSearcher(DirectoryReader.open(getLuceneWriter()));
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
        List<String> names  = new ArrayList<String>();
	request.put(ARG_MAX,20);
	for(Entry entry:  getEntryManager().getEntries(request, new StringBuilder())) {
	    String obj = Json.map("name", Json.quote(entry.getName()), "id",
				  Json.quote(entry.getId()),
				  "type",Json.quote(entry.getTypeHandler().getType()),
				  "icon",
				  Json.quote(entry.getTypeHandler().getTypeIconUrl()));
	    names.add(obj);
	}
	return new Result("", new StringBuilder(Json.map("values", Json.list(names))), "text/json");
    }

    private Query makeAnd(Query...queries) {
	BooleanQuery.Builder builder = new BooleanQuery.Builder();
	for(Query query: queries) {
	    builder.add(query, BooleanClause.Occur.MUST);
	}
	return builder.build();
    }

    private Query makeOr(Query...queries) {
	BooleanQuery.Builder builder = new BooleanQuery.Builder();
	for(Query query: queries) {
	    builder.add(query, BooleanClause.Occur.SHOULD);
	}
	return builder.build();
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
    public void processLuceneSearch(Request request, List<Entry> entries)
	throws Exception {
        StringBuffer sb = new StringBuffer();
	//        StandardAnalyzer analyzer =       new StandardAnalyzer();
	//	QueryBuilder builder = new QueryBuilder(analyzer);
        IndexSearcher searcher = getLuceneSearcher();
	String text = request.getString(ARG_TEXT,"");
	String searchField = null;
	for(String field: SEARCH_FIELDS) {
	    if(text.indexOf(field+":")>=0) {
		searchField = field;
		text = text.substring((field.length()+1));
		break;
	    }
	}

	List<Query> queries = new ArrayList<Query>();
	text = text.trim();
	if(text.length()>0) {
	    text = text.toLowerCase();
	    BooleanQuery.Builder builder = new BooleanQuery.Builder();
	    List<String> words = Utils.split(text," ",true,true);
	    for(String field: SEARCH_FIELDS) {
		if(searchField!=null && !field.equals(searchField)) continue;
		if(words.size()>1) {
		    BooleanQuery.Builder multiBuilder = new BooleanQuery.Builder();
		    for (String word : words) {
			Query term = new WildcardQuery(new Term(field, word));		
			multiBuilder.add(term, BooleanClause.Occur.MUST);
		    }
		    builder.add(multiBuilder.build(),BooleanClause.Occur.SHOULD);
		} else {
		    Query term = new WildcardQuery(new Term(field, text));		
		    builder.add(term, BooleanClause.Occur.SHOULD);
		}
	    }
	    queries.add(builder.build());
	}

	for (DateArgument arg : DateArgument.SEARCH_ARGS) {
	    long min = Long.MIN_VALUE;
	    long max = Long.MAX_VALUE;
            Date[] dateRange = request.getDateRange(arg.getFrom(),
                                   arg.getTo(), arg.getRelative(),
                                   new Date());
	    Date date1 = dateRange[0];
	    Date date2 = dateRange[1];
	    if(date1==null && date2==null) continue;
	    if (arg.forCreateDate() || arg.forChangeDate()) {
		String field = arg.forCreateDate()
		    ? FIELD_DATE_CREATED
		    : FIELD_DATE_CHANGED;
		if(date1!=null || date2!=null) {
		    queries.add(SortedNumericDocValuesField.newSlowRangeQuery(field, date1!=null?date1.getTime():min,
									      date2!=null?date2.getTime():max));
		}
		continue;
	    }
	    long t1 = date1==null?min:date1.getTime();
	    long t2 = date2==null?max:date2.getTime();	    
	    if (date1 == null) {
		date1 = date2;
	    }
	    if (date2 == null) {
		date2 = date1;
	    }


	    queries.add(LongPoint.newRangeQuery(FIELD_DATE_START, t1,t2));

	    String dateSearchMode = request.getString(arg.getMode(), DATE_SEARCHMODE_DEFAULT);
	    queries.add(LongPoint.newRangeQuery(FIELD_DATE_START, t1,t2));
	    queries.add(LongPoint.newRangeQuery(FIELD_DATE_END, t1,t2));	    
	    if (dateSearchMode.equals(DATE_SEARCHMODE_OVERLAPS)) {
	    } else if (dateSearchMode.equals(DATE_SEARCHMODE_CONTAINEDBY)) {
		//TODO
		//		queries.add(Clause.ge(FIELD_DATE_START, date1));
		//		queries.add(Clause.le(FIELD_DATE_END,  date2));
	    } else {
		//DATE_SEARCHMODE_CONTAINS
		//		queries.add(Clause.le(FIELD_DATE_START,  date1));
		//		queries.add(Clause.ge(FIELD_DATE_END, date2));
	    }
	}

	boolean contains = !(request.getString(
					       ARG_AREA_MODE, VALUE_AREA_OVERLAPS).equals(
											  VALUE_AREA_OVERLAPS));


	contains=true;
	List<SelectionRectangle> rectangles = getEntryUtil().getSelectionRectangles(request.getSelectionBounds());
	List<Query> areaQueries = new ArrayList<Query>();
	for (SelectionRectangle rectangle : rectangles) {
	    if(!rectangle.anyDefined()) continue;
	    System.err.println("BBOX:" + rectangle);
	    double minLat = rectangle.hasSouth()?rectangle.getSouth():-90;
	    double maxLat = rectangle.hasNorth()?rectangle.getNorth():90;	    
	    double minLon = rectangle.hasWest()?rectangle.getWest():-180;
	    double maxLon = rectangle.hasEast()?rectangle.getEast():180;	    
	    if (contains) {
		if (rectangle.hasNorth()) {
		    areaQueries.add(DoublePoint.newRangeQuery(FIELD_NORTH,minLat,rectangle.getNorth()));
		}
		if (rectangle.hasSouth()) {
		    areaQueries.add(DoublePoint.newRangeQuery(FIELD_SOUTH,rectangle.getSouth(),maxLat));
		}
		if (rectangle.hasWest()) {
		    areaQueries.add(DoublePoint.newRangeQuery(FIELD_WEST,rectangle.getWest(),maxLon));
		}
		if (rectangle.hasEast()) {
		    areaQueries.add(DoublePoint.newRangeQuery(FIELD_EAST,minLon,rectangle.getEast()));
		}
	    } else {

	    }
	}
	if (areaQueries.size() > 0) {
	    BooleanQuery.Builder areaBuilder = new BooleanQuery.Builder();
	    for(Query query: areaQueries) {
		areaBuilder.add(query, BooleanClause.Occur.MUST);
	    }
	    queries.add(areaBuilder.build());
	}


	String ancestor = request.getString(ARG_ANCESTOR+"_hidden", request.getString(ARG_ANCESTOR,null));
	if(Utils.stringDefined(ancestor)) {
	    queries.add(new TermQuery(new Term(FIELD_ANCESTOR, ancestor)));
	}
        if (request.defined(ARG_GROUP)) {
	    List<String> toks = Utils.split(request.getString(ARG_GROUP), "|", true,
					    true);
	    BooleanQuery.Builder parentBuilder = new BooleanQuery.Builder();
	    for(String tok: toks) {
		Query query = new TermQuery(new Term(FIELD_PARENT, tok));
		parentBuilder.add(query, BooleanClause.Occur.SHOULD);
	    }
	    queries.add(parentBuilder.build());
	}


        Hashtable args        = request.getArgs();
        String metadataPrefix = ARG_METADATA_ATTR1 + "_";
	CategoryList<String> cats = new CategoryList<String>();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(metadataPrefix)) {
                continue;
            }
            if ( !request.defined(arg)) {
                continue;
            }
	    String value = request.getString(arg);
            String type = arg.substring(ARG_METADATA_ATTR1.length() + 1);
	    cats.add(type,value);
	}
	List<Query> metadataQueries = new ArrayList<Query>();
	for(String type: cats.getCategories()) {
	    BooleanQuery.Builder builder = new BooleanQuery.Builder();
	    String field = getMetadataField(type);
	    for(String value: cats.get(type)) {
		Query query = new TermQuery(new Term(field, value));
		builder.add(query,BooleanClause.Occur.SHOULD);		
	    }
	    queries.add(builder.build());
	}

	if(request.defined(ARG_TYPE)) {
	    TypeHandler typeHandler = getRepository().getTypeHandler(request.getString(ARG_TYPE));
	    if(typeHandler!=null) {
		//		queries.add(new TermQuery(new Term(FIELD_TYPE, typeHandler.getType())));
		queries.add(new TermQuery(new Term(FIELD_SUPERTYPE, request.getString(ARG_TYPE))));
		List<Column> columns = typeHandler.getColumns();
		if (columns != null) {
		    BooleanQuery.Builder builder = new BooleanQuery.Builder();
		    int cnt = 0;
		    for (Column column : columns) {

			if (!column.getCanSearch()) {
			    continue;
			}
			String       searchArg = column.getSearchArg();
			Query term=null;
			String field = getPropertyField(typeHandler,column.getName());
			if(column.isEnumeration()) {
			    String v = request.getString(searchArg,null);
			    if(!Utils.stringDefined(v)||v.equals(TypeHandler.ALL)) continue;
			    term = new TermQuery(new Term(field, v));
			} else if(column.isDouble()) {
			    String expr = request.getEnum(searchArg + "_expr", "", "",
							  Column.EXPR_EQUALS, Column.EXPR_LE, Column.EXPR_GE,
							  Column.EXPR_BETWEEN, "&lt;=", "&gt;=");
			    expr = expr.replace("&lt;", "<").replace("&gt;", ">");
			    double from  = request.get(searchArg + "_from", Double.NaN);
			    double to    = request.get(searchArg + "_to", Double.NaN);
			    double value = request.get(searchArg, Double.NaN);
			    if (column.isType(Column.DATATYPE_PERCENTAGE)) {
				from  = from / 100.0;
				to    = to / 100.0;
				value = value / 100.0;
			    }
			    if (!Double.isNaN(from) && Double.isNaN(to)) {
				to = from;
			    } else if (Double.isNaN(from) && !Double.isNaN(to)) {
				from = to;
			    } else if (Double.isNaN(from) && Double.isNaN(to)) {
				from = value;
				to   = value;
			    }
			    if (Double.isNaN(from)) continue;
			    if (expr.equals("")) {
				expr = Column.EXPR_EQUALS;
			    }
			    if (expr.equals(Column.EXPR_EQUALS)) {
				term = DoublePoint.newExactQuery(field,from);
			    } else if (expr.equals(Column.EXPR_LE)) {
				term = DoublePoint.newRangeQuery(field,Double.MIN_VALUE,to);
			    } else if (expr.equals(Column.EXPR_GE)) {
				term = DoublePoint.newRangeQuery(field,from,Double.MAX_VALUE);
			    } else if (expr.equals(Column.EXPR_BETWEEN)) {
				term = DoublePoint.newRangeQuery(field,from,to);
			    } else if (expr.length() > 0) {
				throw new IllegalArgumentException("Unknown expression:"
								   + expr);
			    }
			} else if(column.isInteger()) {
			    String expr = request.getEnum(searchArg + "_expr", "", "",
							  Column.EXPR_EQUALS, Column.EXPR_LE, Column.EXPR_GE,
							  Column.EXPR_BETWEEN, "&lt;=", "&gt;=");
			    expr = expr.replace("&lt;", "<").replace("&gt;", ">");
			    int undef = -99999999;
			    int from  = request.get(searchArg + "_from", undef);
			    int to    = request.get(searchArg + "_to", undef);
			    int value = request.get(searchArg, undef);
			    if ((from != undef) && (to == undef)) {
				to = from;
			    } else if ((from == undef) && (to != undef)) {
				from = to;
			    } else if ((from == undef) && (to == undef)) {
				from = value;
				to   = value;
			    }
			    if (from == undef) continue;
			    if (expr.equals("")) {
				expr = Column.EXPR_EQUALS;
			    }
			    if (expr.equals(Column.EXPR_EQUALS)) {
				term = IntPoint.newExactQuery(field,from);
			    } else if (expr.equals(Column.EXPR_LE)) {
				term = IntPoint.newRangeQuery(field,Integer.MIN_VALUE,to);
			    } else if (expr.equals(Column.EXPR_GE)) {
				term = IntPoint.newRangeQuery(field,from,Integer.MAX_VALUE);
			    } else if (expr.equals(Column.EXPR_BETWEEN)) {
				term = IntPoint.newRangeQuery(field,from,to);
			    } else if (expr.length() > 0) {
				throw new IllegalArgumentException("Unknown expression:"
								   + expr);
			    }
			} else {
			    String v = request.getString(searchArg,null);
			    if(!Utils.stringDefined(v)||v.equals(TypeHandler.ALL)) continue;
			    term = new WildcardQuery(new Term(field, v));
			}
			cnt++;
			if(term!=null)
			    builder.add(term, BooleanClause.Occur.SHOULD);
		    }
		    if(cnt>0) queries.add(builder.build());
		}
	    }
	}

	Query query = null;
	if(queries.size()==0) {
	    query = new MatchAllDocsQuery();
	} else if(queries.size()==1) {
	    query = queries.get(0);
	} else  {
	    BooleanQuery.Builder builder = new BooleanQuery.Builder();
	    for(Query q: queries)
		builder.add(q,BooleanClause.Occur.MUST);
	    query = builder.build();
	}

	//	System.err.println("QUERY:" + query);
	int max = request.get(ARG_MAX,100);
	int skip = request.get(ARG_SKIP,0);

	//	searcher.setDefaultFieldSortScoring(true, false);
	Sort sort;
        if(request.exists(ARG_ORDERBY)) {
	    boolean desc = true;
            String by = request.getString(ARG_ORDERBY, (String) null);
            if (request.get(ARG_ASCENDING, false)) {
                desc = false;
            }

	    String field=null;
	    SortField.Type sortType = SortField.Type.STRING;
            if (by.equals(ORDERBY_FROMDATE)) {
                field = FIELD_DATE_START;
		sortType = SortField.Type.LONG;
            } else if (by.equals(ORDERBY_TODATE)) {
                field = FIELD_DATE_END;                
		sortType = SortField.Type.LONG;
            } else if (by.equals(ORDERBY_RELEVANT)) {
		sort = Sort.RELEVANCE;
            } else if (by.equals(ORDERBY_CREATEDATE)) {
		sortType = SortField.Type.LONG;
                field = FIELD_DATE_CREATED;
	    } else if (by.equals(ORDERBY_CHANGEDATE)) {
		sortType = SortField.Type.LONG;
                field = FIELD_DATE_CHANGED;
            } else if (by.equals(ORDERBY_ENTRYORDER)) {
		sortType = SortField.Type.INT;
                field = FIELD_ENTRYORDER;
            } else if (by.equals(ORDERBY_TYPE)) {
                field = FIELD_TYPE;
            } else if (by.equals(ORDERBY_SIZE)) {
		sortType = SortField.Type.LONG;
                field = FIELD_SIZE;
            } else {
		field=FIELD_NAME_SORT;
	    }

	    if(field==null)
		sort = Sort.RELEVANCE;
	    else {
		if(sortType == SortField.Type.LONG || sortType == SortField.Type.INT)
		    sort = new Sort(new SortField[] {
			    new SortedNumericSortField(field, sortType,desc),
			    new SortField(FIELD_NAME_SORT, SortField.Type.STRING,true)});
		else
		    sort = new Sort(new SortField[] {new SortField(field, sortType,desc),
						     new SortField(FIELD_NAME_SORT, SortField.Type.STRING,desc)});
	    }
	} else {
	    sort = Sort.RELEVANCE;
	}

	//	System.err.println("m:" + (max+skip));
	TopDocs       hits     = searcher.search(query, max+skip,sort);
	//        TopDocs       hits     = searcher.search(query, 100);		
        ScoreDoc[]    docs     = hits.scoreDocs;
	HashSet seen = new HashSet();
        for (int i = skip; i < docs.length; i++) {
            org.apache.lucene.document.Document doc =
                searcher.doc(docs[i].doc);
            String id = doc.get(FIELD_ENTRYID);
            if (id == null) {
		System.err.println("No ID");
                continue;
            }
	    if(seen.contains(id)) {
		//		System.err.println("seen:"+ id);
		continue;
	    }
	    seen.add(id);
            Entry entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
		System.err.println("No ENTRY:" + id);
                continue;
            }
	    //	    System.err.println("entry:"+ entry +" id:" + entry.getId());
	    entries.add(entry);
        }
	System.err.println("lucene results:" + docs.length +" #entries:" + entries.size() +" query:" + query);
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


    public void entriesMoved(final List<Entry> entries) {
        if ( !isLuceneEnabled()) {
            return;
        }
	Misc.run(new Runnable() {
		public void run() {
		    try {
			Request tmp = getRepository().getTmpRequest();
			for(Entry entry:entries) {
			    entriesMovedInner(tmp, entry);
			}
		    } catch(Exception exc) {
			logException("Error handling entriesMoved", exc);
		    }
		}
	    });

    }

    private void entriesMovedInner(Request request,  Entry entry) throws Exception {
	List<Entry> children = getEntryManager().getChildren(request, entry);
	//	System.err.println("moved:" + entry +" children:" + children);
	if(children!=null) {
	    for(Entry child: children) {
		entriesMovedInner(request, child);
	    }
	}
	List<String> ids = new ArrayList<String>();
	List<Entry> entries = new ArrayList<Entry>();
	ids.add(entry.getId());
	entries.add(entry);
	entriesDeleted(ids);
	indexEntries(entries);
    }


    /**
     * _more_
     *
     * @param ids _more_
     */
    public void entriesDeleted(List<String> ids) {
        if ( !isLuceneEnabled()) {
            return;
        }
        try {
	    synchronized(luceneMutex) {
		IndexWriter writer = getLuceneWriter();
		for (String id : ids) {
		    writer.deleteDocuments(new Term(FIELD_ENTRYID, id));
		}
		writer.commit();
	    }
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
        value = value.replaceAll("\"", "&quot;");
        String textField =
            HtmlUtils.input(
			    ARG_TEXT, value,
			    HtmlUtils.attr("placeholder", msg(" Search text"))
			    + HtmlUtils.id("searchinput") + HtmlUtils.SIZE_50
			    + " autocomplete='off' autofocus ") + "\n<div id=searchpopup class=ramadda-popup></div>";
	//	textField+= HtmlUtils.script("Utils.searchSuggestInit('searchinput');");

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
        String        inner         = searchForm.toString();
        StringBuilder formSB        = new StringBuilder();
        boolean       showProviders = request.get("show_providers", false);
        HtmlUtils.makeAccordion(formSB, msg("Search Options"), inner,
                                !showProviders, "ramadda-accordion", null);

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
	Function<String,String> inset = c->{
		return HtmlUtils.insetDiv(c, 5, 10, 10, 0);
	    };


	if(isLuceneEnabled()) {
	    String ancestor = request.getString(ARG_ANCESTOR+"_hidden", request.getString(ARG_ANCESTOR,null));
	    Entry ancestorEntry = ancestor==null?null:getEntryManager().getEntry(request, ancestor);
	    String select =
		getRepository().getHtmlOutputHandler().getSelect(request, ARG_ANCESTOR,
								 null,
								 true, "", ancestorEntry, true);

	    String event = OutputHandler.getSelectEvent(request, ARG_ANCESTOR, true, "", ancestorEntry);
	    sb.append(HU.hidden(ARG_ANCESTOR + "_hidden",
				ancestor!=null?ancestor:"",
				HU.id(ARG_ANCESTOR + "_hidden")));
	    sb.append(inset.apply(select + HU.space(1) +
				  HU.disabledInput(ARG_ANCESTOR, ancestorEntry!=null?ancestorEntry.getName():"",
						   HU.clazz("disabledinput ramadda-entry-popup-select") + HU.attr("placeholder","Search under") + HU.attr("onClick", event) + HU.SIZE_40 + HU.id(ARG_ANCESTOR))));
	}


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


        Object       oldValue = request.remove(ARG_RELATIVEDATE);
        List<Clause> where    = typeHandler.assembleWhereClause(request);
        if (oldValue != null) {
            request.put(ARG_RELATIVEDATE, oldValue);
        }

        typeHandler.addToSearchForm(request, titles, contents, where, true,
                                    false);

        addSearchProviders(request, contents, titles);
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
            tmp.add(inset.apply(c));
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
            HtmlUtils.makeAccordion(formSB, titles, contents, !showProviders,
                                    "ramadda-accordion", null);
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
        orderByList.add(new TwoFacedObject(msg("Relevant"), ORDERBY_RELEVANT));
        orderByList.add(new TwoFacedObject(msg("From Date"),
                                           ORDERBY_FROMDATE));
        orderByList.add(new TwoFacedObject(msg("To Date"), ORDERBY_TODATE));
        orderByList.add(new TwoFacedObject(msg("Create Date"),
                                           ORDERBY_CREATEDATE));
        orderByList.add(new TwoFacedObject(msg("Name"), ORDERBY_NAME));
        orderByList.add(new TwoFacedObject(msg("Size"), ORDERBY_SIZE));

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
            selectedProviders.addAll(Utils.split(tok, ",", true, true));
        }

        if (selectedProviders.size() == 0) {
            selectedProviders.add("this");
        }


        CategoryBuffer cats  = new CategoryBuffer();
        StringBuilder  extra = new StringBuilder();
	HashSet seen = new HashSet();
        for (int i = 0; i < searchProviders.size(); i++) {
            SearchProvider searchProvider = searchProviders.get(i);
	    if(seen.contains(searchProvider.getId())) continue;
	    seen.add(searchProvider.getId());

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
                    String icon = type.getIcon();
                    if (icon == null) {
                        icon = "";
                    }
                    if ( !HU.isFontAwesome("fa") && !icon.equals("")) {
                        icon = getRepository().getIconUrl(icon);
                    }
                    tfos.add(new HtmlUtils.Selector(HtmlUtils.space(2)
						    + type.getLabel(), type.getId(), icon));
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
        List<String> toks = Utils.split(request.getRequestPath(), "/", true,
                                        true);
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
            if (typeHandler != null) {
                Result result =
                    typeHandler.getSpecialSearch().processSearchRequest(
									request, sb);
                //Is it non-html?
                if (result != null) {
                    return result;
                }
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
        for (EntryManager.SuperType superType :
		 getEntryManager().getCats(false)) {
            boolean didSuper = false;
            for (EntryManager.Types types : superType.getList()) {
                boolean didSub = false;
                for (TypeHandler typeHandler : types.getList()) {
                    int cnt = getEntryUtil().getEntryCount(typeHandler);
                    if (cnt == 0) {
                        continue;
                    }
                    if ( !didSuper) {
                        didSuper = true;
                        sb.append(
				  "<div class=type-group-container><div class='type-group-header'>"
				  + superType.getName()
				  + "</div><div class=type-group>");
                    }
                    if ( !didSub) {
                        didSub = true;
                        sb.append(
				  "<div class=type-list-container><div class='type-list-header'>"
				  + types.getName()
				  + "</div><div class=type-list>");
                    }
                    String icon = typeHandler.getIconProperty(null);
                    String img;
                    if (icon == null) {
                        icon = ICON_BLANK;
                        img = HtmlUtils.img(
					    typeHandler.getIconUrl(icon), "",
					    HtmlUtils.attr(HtmlUtils.ATTR_WIDTH, "16"));
                    } else {
                        img = HtmlUtils.img(typeHandler.getIconUrl(icon));
                    }
                    String label = img + HU.SPACE
			+ typeHandler.getDescription() + HU.SPACE
			+ "(" + cnt + ")";
                    String href = HtmlUtils.href(getRepository().getUrlBase()
						 + "/search/type/"
						 + typeHandler.getType(), label);
                    String help = "Search for "
			+ typeHandler.getDescription() + " - "
			+ cnt + " entries";
                    HU.div(sb, href,
                           HU.attrs("class", "type-list-item", "title",
                                    help));
                }
                if (didSub) {
                    sb.append("</div></div>");
                }
            }
            if (didSuper) {
                sb.append("</div></div>");
            }
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
        //        sb.append(header(msg("Search Results")));

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
        getPageHandler().sectionOpen(request, headerSB, "Search", false);
        getPageHandler().makeLinksHeader(request, headerSB, getSearchUrls(),
                                         "");
        headerSB.append(sb.toString());
        getPageHandler().sectionClose(request, headerSB);
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
    public synchronized  List<SearchProvider> getSearchProviders() throws Exception {
        if (searchProviders == null) {
            //            System.err.println("SearchManager.doSearch- making searchProviders");
	    searchProviderMap =     new Hashtable<String, SearchProvider>();
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

	    HashSet seen = new HashSet();
            tmp.add(thisSearchProvider =
		    new SearchProvider.RamaddaSearchProvider(getRepository(),
							     ServerInfo.ID_THIS, "This RAMADDA Repository"));

	    seen.add(thisSearchProvider.getId());
            for (ServerInfo server :
		     getRegistryManager().getEnabledRemoteServers()) {
		SearchProvider provider = new SearchProvider.RemoteSearchProvider(getRepository(), server);
		if(seen.contains(provider.getId())) continue;
		seen.add(provider.getId());
		tmp.add(provider);
            }

            for (SearchProvider provider : tmp) {
                searchProviderMap.put(provider.getType(), provider);
            }
            tmp.addAll(searchProviders);
            allProviders = tmp;
        }

        return allProviders;
    }



    public synchronized void clearSearchProviders() {
	searchProviderMap = null;
	allProviders = null;
	searchProviders = null;
	
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
            providers.addAll(Utils.split(arg, ",", true, true));
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
		     Utils.split(request.getString("entries", ""), ",", true,
				 true)) {
                Entry e = getEntryManager().getEntry(request, id);
                if (e == null) {
                    continue;
                }
                allEntries.add(e);
            }
            doSearch = false;
        }
        String root = null;

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
            runSearch(runnables, running, runnableCnt);
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

        SearchInfo       searchInfo = new SearchInfo();
        List<ServerInfo> servers    = null;

        ServerInfo       thisServer = getRepository().getServerInfo();

        List<Entry>      groups     = new ArrayList<Entry>();
        List<Entry>      entries    = new ArrayList<Entry>();

        long             t1         = System.currentTimeMillis();
	List[] pair = doSearch(request, searchInfo);
	groups.addAll((List<Entry>) pair[0]);
	entries.addAll((List<Entry>) pair[1]);
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
        getPageHandler().sectionOpen(request, header, "Search", false);
        getPageHandler().makeLinksHeader(request, header, getSearchUrls(),
                                         "");
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

        return r;
    }




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
    private void runSearch(List<Runnable> runnables, boolean[] running,
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
			    List<Entry> entryList =
				getEntryManager().createEntryFromXml(request,
								     node, parentEntry, new Hashtable(), false,
								     false);

			    Entry entry = entryList.get(0);

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
	    System.out.println("contents: " + contents);
        }
    }



}

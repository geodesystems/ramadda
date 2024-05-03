/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.search;

import org.ramadda.repository.*;
import org.ramadda.repository.admin.*;
import static org.ramadda.repository.type.TypeHandler.CorpusType;

import org.ramadda.repository.auth.*;
import org.ramadda.repository.database.DatabaseManager;
import org.ramadda.repository.database.Tables;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.DateArgument;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.repository.util.ServerInfo;

import org.ramadda.util.CategoryBuffer;
import org.ramadda.util.CategoryList;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.FileWrapper;
import org.ramadda.util.IO;
import org.ramadda.util.JQuery;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.TikaUtil;

import org.ramadda.util.OpenSearchUtil;

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

import org.ramadda.util.ProcessRunner;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.net.*;

import java.sql.ResultSet;
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
import org.json.*;
import java.util.Comparator;
import java.util.Collections;



import java.util.regex.*;
import java.util.zip.*;


import java.util.concurrent.*;
import org.ramadda.repository.job.JobManager;


import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;

/**
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("unchecked")
public class SearchManager extends AdminHandlerImpl implements EntryChecker {

    private static boolean debugCorpus = false;

    public static final String SUFFIX_LATITUDE ="_latitude";
    public static final String SUFFIX_LONGITUDE ="_longitude";


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

    private static final String FIELD_CORPUS = "corpus";


    /** _more_ */
    private static final String FIELD_CONTENTS = "contents";

    private static final String FIELD_ATTACHMENT = "attachment";

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

    private static final String FIELD_CREATOR = "creator";    

    private static final String FIELD_NAME_SORT = "namesort";    

    private static final String[] SEARCH_FIELDS ={FIELD_CORPUS, FIELD_NAME, FIELD_CREATOR,FIELD_DESCRIPTION, FIELD_CONTENTS,FIELD_ATTACHMENT, FIELD_PATH};


    private boolean isLuceneEnabled = true;

    private Object LUCENE_MUTEX = new Object();

    public static final int LUCENE_MAX_LENGTH = 25_000_000;

    private IndexWriter luceneWriter;

    private IndexSearcher luceneSearcher;

    private String tesseractPath;

    private boolean indexImages = true;

    private SearchProvider thisSearchProvider;

    private List<SearchProvider> searchProviders;

    private List<SearchProvider> allProviders;

    private Hashtable<String, SearchProvider> searchProviderMap;

    private List<SearchProvider> pluginSearchProviders =
        new ArrayList<SearchProvider>();

    private Hashtable<String,List<String>> synonyms;

    private boolean showMetadata= true;

    public SearchManager(Repository repository) {
        super(repository);
        repository.addEntryChecker(this);
        getAdmin().addAdminHandler(this);
    }


    @Override
    public void initAttributes() {
        super.initAttributes();
        showMetadata = getRepository().getProperty(PROP_SEARCH_SHOW_METADATA, true);
	tesseractPath = getRepository().getScriptPath("ramadda.tesseract");
	indexImages = getRepository().getProperty("ramadda.indeximages",false);
        isLuceneEnabled = getRepository().getProperty(PROP_SEARCH_LUCENE_ENABLED, true);
    }

    public List<String> getSynonyms(String word) throws Exception {
	word = word.toLowerCase().trim();
	if(synonyms==null)synonyms = getSynonyms();
	return synonyms.get(word);
    }

    public Hashtable<String,List<String>>getSynonyms() throws Exception {
	if(synonyms==null) {
	    Hashtable<String,List<String>>tmp = new Hashtable<String,List<String>>();
	    //https://www.kaggle.com/duketemon/wordnet-synonyms
	    String resource = getStorageManager().readSystemResource("/org/ramadda/repository/resources/synonyms.csv");
	    //big,adjective,large
	    for(String line: Utils.split(resource,"\n",true,true)) {
		List<String> toks = Utils.splitUpTo(line,",",3);
		String word  = toks.get(0);
		List<String> row = new ArrayList<String>();
		for(String tuple: Utils.split(toks.get(2),";",true,true)) {
		    row.addAll(Utils.split(tuple,"|",true,true));
		}
		tmp.put(word,row);
	    }

	    synonyms = tmp;
	}
	return synonyms;
    }





    public boolean isImageIndexingEnabled() {
	return stringDefined(tesseractPath);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean includeMetadata() {
	return showMetadata;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isLuceneEnabled() {
	//For now it is always true
	return true;
	//        return isLuceneEnabled;
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
	    synchronized(LUCENE_MUTEX) {
		if(luceneWriter==null) {
		    Directory index = new NIOFSDirectory(Paths.get(getStorageManager().getIndexDir()));
		    IndexWriterConfig config = new IndexWriterConfig();
		    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		    luceneWriter = new IndexWriter(index, config);
		}
	    }
	}
	return luceneWriter;
    }


    public void reindexLucene(Request request,Object actionId, String type)  {
	try {
	    IndexWriter indexWriter = getLuceneWriter();
	    try {
		reindexLuceneInner(request, indexWriter, actionId, type);
	    } finally {
		//		writer.close();
	    }
	} catch(Throwable thr) {
	    thr.printStackTrace();
	    throw new RuntimeException(thr);
	}
    }

    private void reindexLuceneInner(final Request request, final IndexWriter indexWriter, Object actionId, String type)  throws Throwable {	
	Clause clause = null;
	if(stringDefined(type)) {
	    clause = Clause.or(getDatabaseManager().addTypeClause(getRepository(),request, Utils.split(type,",",true,true),null));
	}
        Statement statement =
            getDatabaseManager().select(Tables.ENTRIES.COL_ID,
					Misc.newList(Tables.ENTRIES.NAME),
					clause,null,DatabaseManager.NOMAX);

        SqlUtil.Iterator iter = getDatabaseManager().getIterator(statement);
        ResultSet        results;
	List<String> ids = new ArrayList<String>();
	IndexSearcher searcher = null;
	while ((results = iter.getNext()) != null) {
            String id = results.getString(1);
	    ids.add(id);
	}

	getDatabaseManager().closeAndReleaseConnection(statement);

	if(false) {
	    indexWriter.deleteAll();
	    commit(indexWriter);
	} else {
	    for(String id: ids)  {
		indexWriter.deleteDocuments(new Term(FIELD_ENTRYID, id));
	    }
	    commit(indexWriter);
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
	    callables.add(makeReindexer((List<String>)idList,indexWriter,ids.size(),cnt,actionId,mutex,ok));
	}
	long t1 = System.currentTimeMillis();
	getRepository().getJobManager().invokeAllAndWait(callables);
	long t2 = System.currentTimeMillis();
	if(ok[0]) {
	    System.err.println("committing");
	    commit(indexWriter);
	}
	getActionManager().actionComplete(actionId);
    }

    private Callable<Boolean> makeReindexer(final List<String> ids, final IndexWriter indexWriter,final int total, final int[] cnt, final Object actionId, final Object mutex, final boolean[]ok) throws Exception {
        return  new Callable<Boolean>() {
            public Boolean call() {
                try {
		    final Request request =getRepository().getAdminRequest();
		    for(String id: ids) {
			if(!getRepository().getActive()) return true;
			Entry entry = getEntryManager().getEntry(null, id,false);
			if(entry==null) continue;
			synchronized(mutex) {
			    cnt[0]++;
			    System.err.println("#" + cnt[0] +"/"+ total +" entry:" + entry.getName());
			}
			indexEntry(indexWriter, entry, request, false);
			getEntryManager().removeFromCache(entry);
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
		    exc.printStackTrace();
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

    /**** Don't do this
    @Override
    public void addToAdminSettingsForm(String block, StringBuffer asb) {
        if ( !block.equals(Admin.BLOCK_ACCESS)) {
            return;
        }
        asb.append(HU.colspan(msgHeader("Search"), 2));
	HU.formEntry(asb,  "",
			    HU.labeledCheckbox(
						      PROP_SEARCH_LUCENE_ENABLED, "true",
						      isLuceneEnabled(),
						      msg("Enable Lucene Indexing and Search")));
    }
    **/


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
				    isLuceneEnabled = request.get(PROP_SEARCH_LUCENE_ENABLED, true));
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
    private  void indexEntries(List<Entry> entries, Request request, boolean isNew)
	throws Exception {
	//	synchronized(LUCENE_MUTEX) {
	    IndexWriter indexWriter = getLuceneWriter();
	    try {
		for (Entry entry : entries) {
		    long t1= System.currentTimeMillis();
		    indexEntry(indexWriter, entry, request,isNew);
		    long t2= System.currentTimeMillis();
		    //		    System.err.println("indexEntry:" + entry +" time:" + (t2-t1));
		}
		//        indexWriter.optimize();
		commit(indexWriter);
	    } finally {
		//	    indexWriter.close();
	    }
	    //	}
    }


    /**
     * _more_
     *
     * @param indexWriter _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    int rcnt=0;
    private void indexEntry(IndexWriter indexWriter, Entry entry, Request request, boolean isNew)
	throws Exception {
	//	System.err.println("reindex: " +(rcnt++));
        org.apache.lucene.document.Document doc =
            new org.apache.lucene.document.Document();
	StringBuilder corpus = new StringBuilder();

        doc.add(new StringField(FIELD_ENTRYID, entry.getId(), Field.Store.YES));
	//        doc.add(new StringField(FIELD_TYPE, entry.getTypeHandler().getType(), Field.Store.YES));	
	TypeHandler parentType = entry.getTypeHandler();
	while(parentType!=null) {
	    doc.add(new StringField(FIELD_SUPERTYPE, parentType.getType(), Field.Store.YES));	
	    parentType = parentType.getParent();
	}

	if(entry.getParentEntryId()!=null) {
	    doc.add(new StringField(FIELD_PARENT, entry.getParentEntryId(), Field.Store.YES));	
	    Entry parent = entry;
	    while(parent!=null) {
		//		System.err.println("\tancestor:" + parent.getId());
		doc.add(new StringField(FIELD_ANCESTOR, parent.getId(), Field.Store.YES));	
		parent = parent.getParentEntry();
	    }
	}
	//	System.err.println("add size:" + entry +" " +entry.getResource().getFileSize());

        doc.add(new SortedNumericDocValuesField(FIELD_SIZE, entry.getResource().getFileSize()));
	doc.add(new LongPoint(FIELD_SIZE, entry.getResource().getFileSize()));
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
	    path = path.toLowerCase();
	    corpus.append(path);
	    corpus.append(" ");
            doc.add(new TextField(FIELD_PATH, path, Field.Store.NO));
        }

	String name = entry.getName().toLowerCase();
	corpus.append(name);
	corpus.append(" ");
        doc.add(new TextField(FIELD_NAME,  name,Field.Store.YES));
	String nameSort = entry.getTypeHandler().getNameSort(entry);

	doc.add(new SortedDocValuesField(FIELD_NAME_SORT, new BytesRef(nameSort)));
	doc.add(new StringField(FIELD_CREATOR,  entry.getUserId(),Field.Store.YES));	
	StringBuilder desc = new StringBuilder();
	//false=>don't add columns
	//true=> add metadata
        entry.getTypeHandler().getTextCorpus(entry, desc,false,true);
	String _desc = desc.toString().toLowerCase();
	corpus.append(_desc);
	corpus.append(" ");
        doc.add(new TextField(FIELD_DESCRIPTION, _desc,Field.Store.NO));

	List<Column> columns = entry.getTypeHandler().getColumns();
	if (columns != null) {
	    Object[] values = entry.getTypeHandler().getEntryValues(entry);
	    if(values!=null) {
		for (Column column : columns) {
		    if (!column.getCanSearch()) continue;
		    String field  = getPropertyField(entry.getTypeHandler(),column.getName());
		    Object v= column.getObject(values);
		    if(v==null) continue;
		    //TODO handle latlonbox
		    if(column.isLatLon()) {
			double[] latlon = column.getLatLon(values);
			doc.add(new DoublePoint(field+SUFFIX_LATITUDE, latlon[0]));
			doc.add(new DoublePoint(field+SUFFIX_LONGITUDE, latlon[1]));
		    } else   if(column.isDate()) {
			Date d = DateHandler.checkDate((Date)v);
			if(d!=null) {
			    doc.add(new SortedNumericDocValuesField(field, d.getTime()));
			    if(column.getCanSort())
				doc.add(new SortedNumericDocValuesField(field+"_sort", d.getTime()));
			}
		    } else if(column.isEnumeration())  {
			corpus.append(v.toString());
			corpus.append(" ");
			doc.add(new StringField(field, v.toString(),Field.Store.YES));
			if(column.getCanSort())
			    doc.add(new SortedDocValuesField(field+"_sort", new BytesRef(v.toString())));
		    }
		    else if(column.isDouble())  {
			doc.add(new DoublePoint(field, (Double)v));
			if(column.getCanSort())
			    doc.add(new SortedNumericDocValuesField(field+"_sort",
								    Double.doubleToRawLongBits(Utils.getDouble(v))));

		    }   else if(column.isInteger())  {
			doc.add(new IntPoint(field, (Integer)v));		    
			if(column.getCanSort()) {
			    doc.add(new SortedNumericDocValuesField(field+"_sort", (Integer)v));
			}
		    } else {
			corpus.append(v.toString());
			corpus.append(" ");
			doc.add(new TextField(field, v.toString(),Field.Store.NO));
			if(column.getCanSort())
			    doc.add(new SortedDocValuesField(field+"_sort", new BytesRef(v.toString())));
		    }
		}
	    }
	}

	doc.add(new SortedNumericDocValuesField(FIELD_DATE_CREATED, entry.getCreateDate()));	
	doc.add(new SortedNumericDocValuesField(FIELD_DATE_CHANGED, entry.getChangeDate()));
	doc.add(new SortedNumericDocValuesField(FIELD_DATE_START, entry.getStartDate()));
	doc.add(new SortedNumericDocValuesField(FIELD_DATE_END, entry.getEndDate()));	

	StringBuilder fileCorpus = null;
        if (entry.isFile()) {
	    fileCorpus = new StringBuilder();
            addContentField(request, entry, doc, FIELD_CONTENTS, entry.getResource().getTheFile(), true, fileCorpus);
	} else if(request.get("harvesthtml",false)) {
	    if(entry.getResource().isUrl()) {
		String url = entry.getResource().getPath();
		IO.Result result = IO.getHttpResult(IO.HTTP_METHOD_GET,new URL(url),"");
		if(!result.getError()) {
		    fileCorpus=new StringBuilder(result.getResult());
		} else {
		    getLogManager().logSpecial("Error reading URL:" + url+ " code:" + result.getCode());
		    //+     " error:" + result.getResult());
		}
	    }
	}
        if (fileCorpus!=null) {
	    corpus.append(fileCorpus);
	    if(/*isNew && */request!=null) {
		String llmCorpus = fileCorpus.toString();
		fileCorpus = null;
		boolean entryChanged = false;
		if(isNew) {
		    try {
			//			Misc.sleepSeconds(5);
			//			if(true) throw new RuntimeException("TEST ERROR");
			entryChanged |= getLLMManager().applyEntryExtract(request, entry, llmCorpus);
		    } catch(Throwable thr) {
			getSessionManager().addSessionErrorMessage(request,
								   "An error occurred doing the LLM extraction for the entry: " + entry.getName()+
								   "<br>Error: " + thr.getMessage());
			throw new RuntimeException(thr);
		    }
		}


		if(entryChanged) {
		    List<Entry> tmp = new ArrayList<Entry>();
		    tmp.add(entry);
		    getEntryManager().updateEntries(request, tmp,false);
		    //IMPORTANT: We end up calling back into this method. To keep this section of code from
		    //being called, thus leading to an infinite loop, pass in isNew=false
		    indexEntries(tmp, request, false);
		}
	    }
        } else {
	    StringBuilder contents = new StringBuilder();
	    entry.getTypeHandler().getTextContents(entry, contents);
	    if(contents.length()>0) {
		doc.add(new TextField(FIELD_CONTENTS, contents.toString(), Field.Store.NO));
		corpus.append(contents);
		corpus.append(" ");
	    }
	}




        for (Metadata metadata : getMetadataManager().getMetadata(request,entry)) {
	    MetadataType type = getMetadataManager().getType(metadata);
	    if(type==null) {
		continue;
	    }
	    for (MetadataElement element : type.getChildren()) {
		if ( !element.getDataType().equals(element.DATATYPE_FILE)) {
		    continue;
		}
		File f = element.getFile(entry, metadata, element);
		if(f!=null && f.exists()) {
		    if(!Utils.isImage(f.toString())) {
			addContentField(request,entry, doc, FIELD_ATTACHMENT, f, false, corpus);
		    }
		}
	    }
	    if(type.getSearchable()) {
		//Don't add to corpus as the TypeHandler.getTextCorpus above does that for us
		//		System.err.println("MTD:" + metadata.getAttr1().toLowerCase());
		//		corpus.append(metadata.getAttr1().toLowerCase());
		//		corpus.append(" ");
		doc.add(new StringField(getMetadataField(type.getId()), metadata.getAttr1(),Field.Store.NO));
	    }
	}

        doc.add(new TextField(FIELD_CORPUS, corpus.toString(),Field.Store.NO));
        indexWriter.addDocument(doc);
    }


    /**
       is the file a pdf, doc, ppt, etc
     */
    public boolean isDocument(String path) {
	path = path.toLowerCase();	
	//Only do documents
	if(!(path.endsWith("pdf") ||
	     path.endsWith("ipynb") ||
	     path.endsWith("py") ||
	     path.endsWith("java") ||
	     path.endsWith("js") ||	     	     	     
	     path.endsWith("doc") ||
	     path.endsWith("xls") ||
	     path.endsWith("xlsx") ||	     
	     path.endsWith("ppt") ||
	     path.endsWith("html") ||
	     path.endsWith("pptx") ||	   	   
	     path.endsWith("docx"))) {
	    //	    System.err.println("not doc:" + path);
	    return false;
	}
	return true;
    }


    public TikaConfig getTikaConfig() throws Exception {
	return indexImages?TikaUtil.getConfig():TikaUtil.getConfigNoImage();
    }


    private String readContents(Request request, Entry entry,
				File f,List<org.apache.tika.metadata.Metadata> metadataList) throws Exception {
	boolean isImage = Utils.isImage(f.getName());
	if(isImage) {
	    if(!request.get(ARG_INDEX_IMAGE,false) || tesseractPath==null) {
		if(debugCorpus)
		    System.err.println("SearchManager.readContents: Not indexing images:" + f.getName());
		return null;
	    }
	    try {
		long t1= System.currentTimeMillis();
		File tmp  =getStorageManager().getUniqueScratchFile("output");
		List<String> commands = new ArrayList<String>();
		Utils.add(commands, tesseractPath,f.toString(), tmp.toString());
		ProcessBuilder pb = getRepository().makeProcessBuilder(commands);
		pb.redirectErrorStream(true);
		Process     process = pb.start();
		InputStream is      = process.getInputStream();
		String      result  = new String(IOUtil.readBytes(is));
		String imageText = IO.readContents(tmp.toString()+".txt", getClass());
		long t2= System.currentTimeMillis();
		if(debugCorpus)
		    System.err.println("SearchManager.readContents: from image:" + f.getName());
		System.err.println("tesseract:" + f.getName() +" time:" + (t2-t1));
		return imageText;
	    } catch(Exception exc) {
		getLogManager().logError("Error running tesseract for:" + f.getName(), exc);
		return null;
	    }
	}


	if(!isDocument(f.getName())) {
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: Not a document:" + f.getName());
	    return null;
	}

	//Don't do really big files 
	if(f.length()>LUCENE_MAX_LENGTH) {
	    //Don't do this since the max size should be capped by tika below
	    if(debugCorpus)System.err.println("SearchManager.readContents file too big: " + f.getName() +" " +f.length());
	    //	    return null;
	}

	if(f.length()==0) {
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: empty file: " + f.getName());
	    return null;
	}

	String corpus = entry.getTypeHandler().getCorpus(request, entry,CorpusType.SEARCH);
	if(corpus!=null) return corpus;
	return extractCorpus(request, f.toString(),metadataList);

    }	


    public String extractCorpus(Request request,
				String path,
				List<org.apache.tika.metadata.Metadata> metadataList) throws Exception {

	File f = new File(path);
	File corpusFile = TikaUtil.getTextCorpusCacheFile(f);
	if(corpusFile.exists()) {
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: corpus file exists:" + f.getName());
	    return  IO.readContents(corpusFile.toString(), SearchManager.class);
	} 
	
	if(!f.exists() && path.startsWith("http")) {
	    String url = path;
	    IO.Result result = IO.getHttpResult(IO.HTTP_METHOD_GET,new URL(url),"");
	    if(result.getError()) return null;
	    return result.getResult();
	}


	try(InputStream stream = getStorageManager().getFileInputStream(f)) {
	    BufferedInputStream bis = new BufferedInputStream(stream);
            org.apache.tika.metadata.Metadata metadata =
                new org.apache.tika.metadata.Metadata();
	    if(metadataList!=null)
		metadataList.add(metadata);
	    Parser parser = new AutoDetectParser(getTikaConfig());
            BodyContentHandler handler =  new BodyContentHandler(LUCENE_MAX_LENGTH);	
	    long t1 = System.currentTimeMillis();
            parser.parse(bis, handler, metadata,new org.apache.tika.parser.ParseContext());
	    long t2= System.currentTimeMillis();
	    String corpus = handler.toString();
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: corpus:" + f.getName() +" time:" + (t2-t1)+" length:" + corpus.length());
	    return  corpus;
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
    private void addContentField(Request request, Entry entry,
                                 org.apache.lucene.document.Document doc,
				 String field,
                                 File f, boolean mainEntryFile,
				 StringBuilder corpus)
	throws Exception {
        try {
	    //If it is a metadata attachment and an image then don't try to process it
	    if(!mainEntryFile && Utils.isImage(f.toString())) return;

	    List<org.apache.tika.metadata.Metadata> metadata = new ArrayList<org.apache.tika.metadata.Metadata>();
	    long t1 = System.currentTimeMillis();
	    String contents = readContents(request, entry, f,metadata);
	    long t2= System.currentTimeMillis();
            if ((contents != null) && (contents.length() > 0)) {
                doc.add(new TextField(field, contents, Field.Store.NO));
		if(debugCorpus)
		    System.err.println("SearchManager.addContentField corpus:" + contents.length());
		corpus.append(contents);
		corpus.append(" ");
            }
	    for(org.apache.tika.metadata.Metadata mtd: metadata) {
		String[] names = mtd.names();
		for (String name : names) {
		    String value = mtd.get(name);
		    doc.add(new StringField("document_metadata_"+ name, value, Field.Store.NO));
		}
	    }
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
	if(luceneSearcher==null) {
	    luceneSearcher = new IndexSearcher(DirectoryReader.open(getLuceneWriter()));
	    //	    return new IndexSearcher(DirectoryReader.open(getLuceneWriter()));
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
        List<String> names  = new ArrayList<String>();
	request.put(ARG_MAX,20);
	for(Entry entry:  getEntryManager().searchEntries(request)) {
	    String obj = JsonUtil.map(Utils.makeList("name", JsonUtil.quote(entry.getName()), "id",
						     JsonUtil.quote(entry.getId()),
						     "type",JsonUtil.quote(entry.getTypeHandler().getType()),
						     "typeName",JsonUtil.quote(entry.getTypeHandler().getLabel()),
						     "icon",
						     JsonUtil.quote(entry.getTypeHandler().getTypeIconUrl())));
	    names.add(obj);
	}
	String json = JsonUtil.map(Utils.makeList("values", JsonUtil.list(names)));
	return new Result("", new StringBuilder(json), "text/json");
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

    private Query makeOr(List<Query>queries) {
	BooleanQuery.Builder builder = new BooleanQuery.Builder();
	for(Query query: queries) {
	    builder.add(query, BooleanClause.Occur.SHOULD);
	}
	return builder.build();
    }    
    

    public Result processEntryList(Request request) throws Exception {
	StringBuilder sb = new StringBuilder();
	Entry entry = getEntryManager().getEntry(request, request.getString(ARG_ENTRYID));
	if(entry==null) {
	    sb.append("No entry for id:" + request.getString(ARG_ENTRYID));
	    return new Result("Entry List", sb);
	}
	getPageHandler().entrySectionOpen(request,  entry, sb, "Lucene Document Listing");
	Query idQuery = new TermQuery(new Term(FIELD_ENTRYID,entry.getId()));
	IndexSearcher searcher = getLuceneSearcher();
	TopDocs       hits     = searcher.search(idQuery, 1);
        ScoreDoc[]    docs     = hits.scoreDocs;
	if(docs.length==0) {
	    sb.append("No entry");
	    getPageHandler().entrySectionClose(request,  entry, sb);
	    return new Result("Entry List", sb);
	}
	org.apache.lucene.document.Document doc =
	    searcher.doc(docs[0].doc);

	sb.append(HU.formTable());
	for(IndexableField field: doc.getFields()) {
	    String[]values  = doc.getValues(field.name());
	    for(String v: values) {
		HU.formEntry(sb, Utils.makeLabel(field.name())+":",v);
	    }
	}
	sb.append(HU.formTableClose());
					       
	getPageHandler().entrySectionClose(request,  entry, sb);
	return new Result("Entry List", sb);
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
    int scnt=0;
    public void processLuceneSearch(Request request, List<Entry> entries)
	throws Exception {
        StringBuffer sb = new StringBuffer();
	//        StandardAnalyzer analyzer =       new StandardAnalyzer();
	//	QueryBuilder builder = new QueryBuilder(analyzer);

	//	System.err.println("search:" + (scnt++));
	String text = request.getUnsafeString(ARG_TEXT,"");
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
	    Hashtable<String,List<String>>synonyms=null; 
	    text = text.toLowerCase();
	    BooleanQuery.Builder builder = new BooleanQuery.Builder();
	    List<String> toks = Utils.parseCommandLine(text,false);
	    List<List<String>> words = new ArrayList<List<String>>();
	    if(toks!=null) {
		for (String word : toks) {
		    boolean isSyn = false;
		    if(word.startsWith("~")) {
			word = word.substring(1);
			isSyn = true;
		    }
		    List<String> ors = new ArrayList<String>();
		    words.add(ors);
		    ors.add(word);
		    if(isSyn) {
			List<String> syns = getSynonyms(word);
			if(syns!=null)  {
			    ors.addAll(syns);
			}
		    }
		}
	    }

	    for(String field: SEARCH_FIELDS) {
		boolean isName = field.equals(FIELD_NAME);
		if(searchField!=null && !field.equals(searchField)) continue;
		// for now always do this
		if(true ||words.size()>1) {
		    BooleanQuery.Builder multiBuilder = new BooleanQuery.Builder();
		    for (List<String> ors: words) {
			BooleanQuery.Builder orBuilder = new BooleanQuery.Builder();
			for (String word : ors) {
			    Query term;
			    if(word.indexOf(" ")>0) {
				PhraseQuery.Builder phraseBuilder = new PhraseQuery.Builder();
				for (String pword : Utils.split(word," ",true,true)) {
				    phraseBuilder.add(new Term(field, pword));
				}
				term = phraseBuilder.build();
			    } else {
				term = new WildcardQuery(new Term(field, word));		
			    }
			    if(isName) term = new BoostQuery(term,6);
			    orBuilder.add(term, BooleanClause.Occur.SHOULD);
			}
			multiBuilder.add(orBuilder.build(),BooleanClause.Occur.MUST);
		    }
		    builder.add(multiBuilder.build(),BooleanClause.Occur.SHOULD);
		} else {
		    Query term = new WildcardQuery(new Term(field, text));		
		    if(isName) {
			term = new BoostQuery(term,6);
		    }
		    builder.add(term, BooleanClause.Occur.SHOULD);
		}
	    }
	    queries.add(builder.build());
	}


	String name = request.getUnsafeString(ARG_NAME,null);
	if(stringDefined(name)) {
	    Query query = new BoostQuery(new WildcardQuery(new Term(FIELD_NAME, name.toLowerCase())),6);
	    queries.add(query);
	}
	String description = request.getUnsafeString(ARG_DESCRIPTION,null);
	if(stringDefined(description)) {
	    Query query = new BoostQuery(new WildcardQuery(new Term(FIELD_DESCRIPTION, description.toLowerCase())),6);
	    queries.add(query);
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
	    //	    System.err.println(date1);	    System.err.println(date2);
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
		//		date1 = date2;
	    }
	    if (date2 == null) {
		//		date2 = date1;
	    }


	    //	    queries.add(LongPoint.newRangeQuery(FIELD_DATE_START, min,max));
	    //	    queries.add(LongPoint.newRangeQuery(FIELD_DATE_START, t1,t2));
	    //	    queries.add(LongPoint.newRangeQuery(FIELD_DATE_END, t1,t2));	    
	    queries.add(SortedNumericDocValuesField.newSlowRangeQuery(FIELD_DATE_START,t1,t2));
	    queries.add(SortedNumericDocValuesField.newSlowRangeQuery(FIELD_DATE_END,t1,t2));	    

	    /*
	      we dont do this now
	    String dateSearchMode = request.getUnsafeString(arg.getMode(), DATE_SEARCHMODE_DEFAULT);
	    if (dateSearchMode.equals(DATE_SEARCHMODE_OVERLAPS)) {
	    } else if (dateSearchMode.equals(DATE_SEARCHMODE_CONTAINEDBY)) {
	    } else {}
	    */
	}


	List<SelectionRectangle> rectangles = getEntryUtil().getSelectionRectangles(request.getSelectionBounds());

	boolean contains = !(request.getString(ARG_AREA_MODE, VALUE_AREA_OVERLAPS).equals(VALUE_AREA_OVERLAPS));
	makeAreaQueries(rectangles, queries, contains,FIELD_NORTH,FIELD_WEST,FIELD_SOUTH,FIELD_EAST);


	int sizeMin =  request.get(ARG_SIZE_MIN,-1);
	int sizeMax =  request.get(ARG_SIZE_MAX,-1);	
	if(sizeMin>=0|| sizeMax>=0) {
	    queries.add(LongPoint.newRangeQuery(FIELD_SIZE,sizeMin>=0?sizeMin:Integer.MIN_VALUE,sizeMax>=0?sizeMax:Integer.MAX_VALUE));
	}


	String user = request.getString(ARG_USER_ID,null);
	if(Utils.stringDefined(user)) {
	    queries.add(new TermQuery(new Term(FIELD_CREATOR, user)));
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
	    List<String> values = (List<String>) request.get(arg,new ArrayList());
            String type = arg.substring(ARG_METADATA_ATTR1.length() + 1);
	    for(String value: values) {
		cats.add(type,value);
	    }
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
	    List<Query> typeQueries = new ArrayList<Query>();
	    for(String type: Utils.split(request.getUnsafeString(ARG_TYPE),",",true,true)) {
		TypeHandler typeHandler = getRepository().getTypeHandler(type);
		if(typeHandler==null) continue;
		//		queries.add(new TermQuery(new Term(FIELD_TYPE, typeHandler.getType())));
		typeQueries.add(new TermQuery(new Term(FIELD_SUPERTYPE, type)));
		List<Column> columns = typeHandler.getColumns();
		if (columns == null) continue;
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
			List<String> values = (List<String>) request.get(searchArg,new ArrayList<String>());
			List<Query> ors = new ArrayList<Query>();
			for(String v: values) {
			    if(!Utils.stringDefined(v)||v.equals(TypeHandler.ALL)) continue;
			    if(v.equals("--blank--")) v = "";
			    ors.add(new TermQuery(new Term(field, v)));
			}			    
			if(ors.size()==1) 
			    term = ors.get(0);
			else if(ors.size()>1)
			    term =  makeOr(ors);
			else continue;
		    } else if(column.isDouble()) {
			String expr = request.getEnum(searchArg + "_expr", "", "",
						      Column.EXPR_EQUALS, Column.EXPR_LE, Column.EXPR_GE,Column.EXPR_LT,Column.EXPR_GT,
						      Column.EXPR_BETWEEN, "&lt;=", "&gt;=").trim();
			expr = expr.replace("&lt;", "<").replace("&gt;", ">");
			double from  = request.get(searchArg + "_from", Double.NaN);
			double to    = request.get(searchArg + "_to", Double.NaN);
			double value = request.get(searchArg, Double.NaN);
			if (column.isType(Column.DATATYPE_PERCENTAGE)) {
			    from  = from / 100.0;
			    to    = to / 100.0;
			    value = value / 100.0;
			}
			if (expr.equals("") && (!Double.isNaN(from) || !Double.isNaN(to))) {
			    term = DoublePoint.newRangeQuery(field,Double.isNaN(from)?Double.MIN_VALUE:from,Double.isNaN(to)?Double.MAX_VALUE:to);
			}  else {
			    if (!Double.isNaN(from) && Double.isNaN(to)) {
				to = from;
				expr= Column.EXPR_GE;
			    } else if (Double.isNaN(from) && !Double.isNaN(to)) {
				from = to;
				expr= Column.EXPR_LE;
			    } else if (!Double.isNaN(from) && !Double.isNaN(to)) {
				expr= Column.EXPR_BETWEEN;
			    } else if (Double.isNaN(from) && Double.isNaN(to)) {
				from = value;
				to   = value;
			    }
			    if (Double.isNaN(from)) continue;
			    if (expr.equals("")) {
				term = DoublePoint.newRangeQuery(field,from,to);
				expr = Column.EXPR_EQUALS;
			    }
			    double delta = 0.00000001;
			    if (expr.equals(Column.EXPR_EQUALS)) {
				term = DoublePoint.newExactQuery(field,from);
			    } else if (expr.equals(Column.EXPR_LE)) {
				term = DoublePoint.newRangeQuery(field,Double.MIN_VALUE,to);
			    } else if (expr.equals(Column.EXPR_LT)) {
				term = DoublePoint.newRangeQuery(field,Double.MIN_VALUE,to-delta);
			    } else if (expr.equals(Column.EXPR_GE)) {
				term = DoublePoint.newRangeQuery(field,from,Double.MAX_VALUE);
			    } else if (expr.equals(Column.EXPR_GT)) {
				term = DoublePoint.newRangeQuery(field,from+delta,Double.MAX_VALUE);				
			    } else if (expr.equals(Column.EXPR_BETWEEN)) {
				term = DoublePoint.newRangeQuery(field,from,to);
			    } else if (expr.length() > 0) {
				throw new IllegalArgumentException("Unknown expression:"
								   + expr);
			    }
			}
		    } else if(column.isInteger()) {
			String expr = request.getEnum(searchArg + "_expr", "", "",
						      Column.EXPR_EQUALS, Column.EXPR_LE, Column.EXPR_GE,Column.EXPR_LT,Column.EXPR_GT,
						      Column.EXPR_BETWEEN, "&lt;=", "&gt;=");
			expr = expr.replace("&lt;", "<").replace("&gt;", ">");
			int undef = -99999999;
			int from  = request.get(searchArg + "_from", undef);
			int to    = request.get(searchArg + "_to", undef);
			int value = request.get(searchArg, undef);
			if ((from != undef) && (to == undef)) {
			    to = from;
			    expr=Column.EXPR_GE;
			} else if ((from == undef) && (to != undef)) {
			    from = to;
			    expr=Column.EXPR_LE;
			} else if ((from != undef) && (to != undef)) {
			    expr=Column.EXPR_BETWEEN;
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
			} else if (expr.equals(Column.EXPR_LT)) {
			    term = IntPoint.newRangeQuery(field,Integer.MIN_VALUE,to-1);				
			} else if (expr.equals(Column.EXPR_GE)) {
			    term = IntPoint.newRangeQuery(field,from,Integer.MAX_VALUE);
			} else if (expr.equals(Column.EXPR_GT)) {
			    term = IntPoint.newRangeQuery(field,from+1,Integer.MAX_VALUE);				
			} else if (expr.equals(Column.EXPR_BETWEEN)) {
			    term = IntPoint.newRangeQuery(field,from,to);
			} else if (expr.length() > 0) {
			    throw new IllegalArgumentException("Unknown expression:"
							       + expr);
			}
		    } else if(column.isLatLon()) {
			double[] nwse  = column.getAreaSearchArgs(request);
			SelectionRectangle rectangle = new SelectionRectangle(nwse);
			if(!rectangle.anyDefined()) continue;
			List<SelectionRectangle> rects = getEntryUtil().getSelectionRectangles(rectangle);
			makeAreaQueries(rects, queries, column.getAreaSearchContains(request),
					field+SUFFIX_LATITUDE,
					field+SUFFIX_LONGITUDE,
					null,null);
			continue;
		    } else {
			String v = request.getUnsafeString(searchArg,null);
			if(!Utils.stringDefined(v)||v.equals(TypeHandler.ALL)) continue;
			v = v.toLowerCase();
			term = new WildcardQuery(new Term(field, v));
		    }
		    cnt++;
		    if(term!=null) {
			builder.add(term, BooleanClause.Occur.MUST);
		    }
		}
		if(cnt>0) queries.add(builder.build());
	    }
	    if(typeQueries.size()==1) {
		queries.add(typeQueries.get(0));
	    } else if(typeQueries.size()>1) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for(Query q: typeQueries)
		    builder.add(q,BooleanClause.Occur.SHOULD);
		queries.add(builder.build());
	    }
	}
	//	System.err.println("queries:"+queries);


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

	


	int max = Math.max(0,request.get(ARG_MAX,100));
	int skip = Math.max(0,request.get(ARG_SKIP,0));
	Sort sort;
        if(request.exists(ARG_ORDERBY)) {
	    boolean desc = true;
            String by = request.getString(ARG_ORDERBY, "");
	    if(by.endsWith("_descending")) {
		desc = true;
		by = by.replace("_descending","");
	    } else if(by.endsWith("_ascending")) {
		desc = false;
		by = by.replace("_ascending","");
	    }

            if (request.get(ARG_ASCENDING, false)) {
                desc = false;
            }

	    String field=null;
	    SortField.Type sortType = SortField.Type.STRING;
            if (by.equals(ORDERBY_FROMDATE) || by.equals("date")) {
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
	    } else if(by.startsWith("field:")) {
		TypeHandler typeHandler = getRepository().getTypeHandler(request);
		by = by.substring("field:".length());
		if(typeHandler!=null) {
		    Column column = typeHandler.findColumn(by);
		    if(column!=null) {
			//property_type_missing_person_biological_sex
			//property_type_missing_person_biological_sex
			field = getPropertyField(typeHandler,by)+"_sort";
			if(column.isString() || column.isEnumeration())
			    sortType = SortField.Type.STRING;
			else if(column.isDouble()) 
			    sortType = SortField.Type.DOUBLE;
			else if(column.isDate()) 
			    sortType = SortField.Type.LONG;						
			else if(column.isInteger()) 
			    sortType = SortField.Type.INT;			
		    }
		}
            } else {
		field=FIELD_NAME_SORT;
	    }

	    if(field==null) {
		sort = Sort.RELEVANCE;
	    }  else {
		if(sortType == SortField.Type.LONG || sortType == SortField.Type.INT || sortType==SortField.Type.DOUBLE) {
		    sort = new Sort(new SortField[] {
			    new SortedNumericSortField(field, sortType,desc),
			    new SortField(FIELD_NAME_SORT, SortField.Type.STRING,true)});
		} else {
		    sort = new Sort(new SortField[] {new SortField(field, sortType,desc),
						     new SortField(FIELD_NAME_SORT, SortField.Type.STRING,desc)});
		}
	    }
	} else {
	    sort = Sort.RELEVANCE;
	}
        IndexSearcher searcher = getLuceneSearcher();
	//	searcher.setDefaultFieldSortScoring(true, false);
	TopDocs       hits     = searcher.search(query, max+skip,sort);
	//        TopDocs       hits     = searcher.search(query, 100);		
        ScoreDoc[]    docs     = hits.scoreDocs;
	HashSet seen = new HashSet();
        for (int i = skip; i < docs.length; i++) {
	    //sanity check
	    if(i<0 || i>=docs.length) continue;
	    int  scoreDoc=docs[i].doc;
            org.apache.lucene.document.Document doc = searcher.doc(scoreDoc);
            String id = doc.get(FIELD_ENTRYID);
            if (id == null) {
		getLogManager().logSpecial("luceneSearch: No ID in document");
                continue;
            }
	    if(seen.contains(id)) {
		continue;
	    }
	    seen.add(id);
            Entry entry = getEntryManager().getEntry(request, id);
            if (entry == null) {
		getLogManager().logSpecial("SearchManager.processLuceneSearch - unable to find entry from id:" + id);
                continue;
            }
	    entries.add(entry);
        }

    }

    private void makeAreaQueries(List<SelectionRectangle> rectangles,
				 List<Query> queries,
				 boolean contains,
				 String north,String west,String south, String east) throws Exception {
	List<Query> areaQueries = new ArrayList<Query>();
	for (SelectionRectangle rectangle : rectangles) {
	    if(!rectangle.anyDefined()) continue;
	    double minLat = rectangle.hasSouth()?rectangle.getSouth():-90;
	    double maxLat = rectangle.hasNorth()?rectangle.getNorth():90;	    
	    double minLon = rectangle.hasWest()?rectangle.getWest():-180;
	    double maxLon = rectangle.hasEast()?rectangle.getEast():180;	    
	    if (contains) {
		if(north!=null)
		    areaQueries.add(DoublePoint.newRangeQuery(north,minLat,maxLat));
		if(west!=null)
		areaQueries.add(DoublePoint.newRangeQuery(west,minLon,maxLon));
		if(south!=null)
		    areaQueries.add(DoublePoint.newRangeQuery(south,minLat,maxLat));
		if(east!=null)
		    areaQueries.add(DoublePoint.newRangeQuery(east,minLon,maxLon));
	    } else {
		if(north!=null)
		    areaQueries.add(DoublePoint.newRangeQuery(north,minLat,90));
		if(west!=null)
		   areaQueries.add(DoublePoint.newRangeQuery(west,-180,maxLon));
		if(south!=null)
		    areaQueries.add(DoublePoint.newRangeQuery(south,-90,maxLat));
		if(east!=null)
		    areaQueries.add(DoublePoint.newRangeQuery(east,minLon,180));
	    }
	}

	if (areaQueries.size() > 0) {
	    BooleanQuery.Builder areaBuilder = new BooleanQuery.Builder();
	    for(Query query: areaQueries) {
		areaBuilder.add(query, BooleanClause.Occur.MUST);
	    }
	    queries.add(areaBuilder.build());
	}
	

    }



    /**
     * _more_
     *
     * @param entries _more_
     */
    public void entriesCreated(Request request, List<Entry> entries) {
        if ( !isLuceneEnabled()) {
            return;
        }
        try {
            indexEntries(entries, request, true);
        } catch (Throwable exc) {
            logError("Error indexing entries", exc);
        }
    }



    /**
     * _more_
     *
     * @param entries _more_
     */
    public void entriesModified(Request request, List<Entry> entries) {
        if ( !isLuceneEnabled()) {
            return;
        }
        try {
            List<String> ids = new ArrayList<String>();
            for (Entry entry : entries) {
                ids.add(entry.getId());
            }
            entriesDeleted(ids);
            indexEntries(entries, request,false);
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
	if(children!=null) {
	    for(Entry child: children) {
		if(getEntryManager().isSynthEntry(child.getId())) {
		    continue;
		}
		entriesMovedInner(request, child);
	    }
	}
	//TODO: instead of completely reindexing the entries instead delete the ancestor/parent fields
	//and add the new ones back into the index
	List<String> ids = new ArrayList<String>();
	List<Entry> entries = new ArrayList<Entry>();
	ids.add(entry.getId());
	entries.add(entry);
	entriesDeleted(ids);
	indexEntries(entries, request, false);
    }

    private void commit(IndexWriter indexWriter) throws Exception {
	luceneSearcher = null;
	indexWriter.commit();
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
	    //	    synchronized(LUCENE_MUTEX) {
		IndexWriter indexWriter = getLuceneWriter();
		for (String id : ids) {
		    indexWriter.deleteDocuments(new Term(FIELD_ENTRYID, id));
		}
		commit(indexWriter);
		//	    }
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
        url = HU.url(url, new String[] {
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
        sb.append(HU.sectionOpen(null, false));
        makeSearchForm(request, sb);
        sb.append(HU.sectionClose());

        return makeResult(request, "Search Form", sb);
    }


    public Result processSearchSynonyms(Request request) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(HU.sectionOpen(null, false));
	Hashtable<String,List<String>> syns =getSynonyms();
        sb.append("<table>");
	for(Object o:Utils.getKeys(syns)) {
	    List s= syns.get(o);
	    sb.append("<tr valign=top>");
	    sb.append(HU.td(o.toString()));
	    sb.append(HU.td(Utils.join(s,", ")));
	    sb.append("</tr>");
	}
	sb.append("</table>");
        sb.append(HU.sectionClose());
        return makeResult(request, "Search Synonyms", sb);
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
	//        return request.makeUrl(URL_ENTRY_SEARCH, ARG_NAME, WHAT_ENTRIES);
        return request.makeUrl(URL_ENTRY_SEARCH);
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
            HU.input(
			    ARG_TEXT, value,
			    HU.attr("placeholder", "Search text")
			    + HU.id("searchinput") + HU.SIZE_50
			    + " autocomplete='off' autofocus ") + "\n<div id=searchpopup class=ramadda-popup></div>";
	//	textField+= HU.script("Utils.searchSuggestInit('searchinput');");

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
        return HU.submit("Search", ARG_SEARCH_SUBMIT);
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
	sb.append("\n");
	String id = HU.getUniqueId("searchform_");
        sb.append(HU.formPost(getSearchUrl(request),
			      ""
			      + HU.attr("id",id)
			      + HU.attr("name","searchform")));


	sb.append("\n");
        sb.append(getTextField(request));
        sb.append(" ");
        sb.append(getSearchButtons(request));
	sb.append("\n");
        StringBuilder searchForm = new StringBuilder();
        makeSearchForm(request, searchForm, true, false);
	OutputHandler.addUrlShowingForm(searchForm,null,id,null,null,"showInputField","false");
	searchForm.append("<p>");
        String        inner         = searchForm.toString();
        StringBuilder formSB        = new StringBuilder();
        boolean       showProviders = request.get("show_providers", false);
        HU.makeAccordion(formSB, msg("Search Options"), inner,
                                !showProviders, "ramadda-accordion", null);

        sb.append(HU.insetDiv(formSB.toString(), 0, 0, 0, 0));
        sb.append(HU.formClose());
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

        sb.append(HU.open(HU.TAG_DIV,
                                 HU.cssClass("ramadda-search-form")));
	Function<String,String> inset = c->{
	    return HU.insetDiv(c, 5, 10, 10, 0);
	};


	if(isLuceneEnabled()) {
	    String ancestor = request.getString(ARG_ANCESTOR+"_hidden", request.getString(ARG_ANCESTOR,null));
	    Entry ancestorEntry = ancestor==null?null:getEntryManager().getEntry(request, ancestor);
	    String select =
		getRepository().getHtmlOutputHandler().getSelect(request, ARG_ANCESTOR,
								 null,
								 true, "", ancestorEntry, true,true);

	    String event = OutputHandler.getSelectEvent(request, ARG_ANCESTOR, true, "", ancestorEntry);
	    sb.append(HU.hidden(ARG_ANCESTOR + "_hidden",
				ancestor!=null?ancestor:"",
				HU.id(ARG_ANCESTOR + "_hidden")));
	    String input = HU.disabledInput(ARG_ANCESTOR, ancestorEntry!=null?ancestorEntry.getName():"",
					    HU.clazz("disabledinput ramadda-entry-popup-select") + HU.attr("placeholder","Search under") + HU.attr("onClick", event) + HU.SIZE_40 + HU.id(ARG_ANCESTOR));

	    sb.append(inset.apply(Utils.join(HU.space(1),HU.b("Under")+":",input,select)));
	}


        TypeHandler typeHandler = getRepository().getTypeHandler(request);


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HU.submitImage(getIconUrl(ICON_BLANK),
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

        sb.append(makeOutputSettings(request,true));


        addSearchProviders(request, contents, titles,false,false);
        typeHandler.addToSearchForm(request, titles, contents, where, true,
                                    false);

        long t1 = System.currentTimeMillis();
        if (includeMetadata()) {
            StringBuilder metadataSB = new StringBuilder();
            metadataSB.append(HU.formTable());
            getMetadataManager().addToSearchForm(request, metadataSB);
            metadataSB.append(HU.formTableClose());
            titles.add(msg("Properties"));
            contents.add(metadataSB.toString());
        }
        long t2 = System.currentTimeMillis();
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
            sb.append(HU.h3("Search Providers"));
            sb.append(contents.get(0));
        } else {
            HU.makeAccordion(formSB, titles, contents, !showProviders,
                                    "ramadda-accordion", null);
        }
        sb.append(formSB.toString());
        sb.append(HU.close(HU.TAG_DIV));


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
    public String makeOutputSettings(Request request, boolean addMax) throws Exception {
        String orderBy =makeOrderBy(request,false);
        String s = HU.b("Output") +": " +
	    HU.select(ARG_OUTPUT, getOutputHandlerSelectList(),
		      request.getString(ARG_OUTPUT, "")) +
	    HU.space(2) + HU.b("Order By")+ ": " +orderBy;
	if(addMax) s+=HU.space(2) +HU.b("Max")+": " +
		       HU.input(ARG_MAX,request.getString(ARG_MAX,DEFAULT_SEARCH_SIZE),
				HtmlUtils.SIZE_5);
	return s;
    }

    public String makeOrderBy(Request request, boolean vertical) throws Exception {
        List       orderByList = new ArrayList();
        orderByList.add(new TwoFacedObject(msg("None"), "none"));
        orderByList.add(new TwoFacedObject(msg("Name"), ORDERBY_NAME));
        orderByList.add(new TwoFacedObject(msg("Size"), ORDERBY_SIZE));
        orderByList.add(new TwoFacedObject(msg("Create Date"), ORDERBY_CREATEDATE));
        orderByList.add(new TwoFacedObject(msg("From Date"),  ORDERBY_FROMDATE));
        orderByList.add(new TwoFacedObject(msg("To Date"), ORDERBY_TODATE));
        orderByList.add(new TwoFacedObject(msg("Relevant"), ORDERBY_RELEVANT));

        return 
            HU.select(ARG_ORDERBY, orderByList,
		      request.getString(ARG_ORDERBY,
					"none")) + (vertical?"<br>":"")+
	    HU.labeledCheckbox(ARG_ASCENDING,     "true",
			       request.get(ARG_ASCENDING,false),
			       msg("ascending"));
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
    public void addSearchProviders(Request request, List<String> contents,
				   List<String> titles,boolean justRamadda,
				   boolean skipIfNone)
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
	int cnt = 0;
        for (int i = 0; i < searchProviders.size(); i++) {
            SearchProvider searchProvider = searchProviders.get(i);
	    if(justRamadda && !searchProvider.getType().equals("ramadda")) continue;
	    if(seen.contains(searchProvider.getId())) continue;
	    cnt++;
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

            String cbxId = HU.getUniqueId("cbx");
            String cbxCall =
                HU.attr(HU.ATTR_ONCLICK,
                               HU.call("HU.checkboxClicked",
					      HU.comma("event",
							      HU.squote(ARG_PROVIDER),
							      HU.squote(cbxId))));



            String anchor = HU.anchorName(searchProvider.getId());
            String cbx = HU.labeledCheckbox(ARG_PROVIDER,
					    searchProvider.getId(),
					    selected,
					    cbxCall + HU.id(cbxId),
					    searchProvider.getFormLabel(false)
					    + (showProviders
					       ? " -- " + searchProvider.getId()
					       : "")) +" " + searchProvider.getFormSuffix();

            cbx += anchor;
            cats.get(searchProvider.getCategory()).append(
							  HU.div(cbx,
								 HU.attrs("title",searchProvider.getTooltip(),
									  "class","ramadda-search-provider")));
        }

        for (String cat : cats.getCategories()) {
            Appendable buff = cats.get(cat);
            if (cat.length() == 0) {
                providerSB.append(buff.toString());
            } else {
                providerSB.append(
				  HU.div(
						cat,
						HU.cssClass(
								   "ramadda-search-provider-header")));
                providerSB.append(HU.div(buff.toString(),
						HU.cssClass("ramadda-search-provider-list")));
            }
        }
        String title =msg("Search providers");
        if (extra.length() > 0) {
            title += HU.space(4) + HU.span(extra.toString(),HU.cssClass("ramadda-highlighted"));
        }
	if(cnt<=1 && skipIfNone) return;
        titles.add(title);
        contents.add(HU.insetDiv(providerSB.toString(), 0, 20, 0, 0));
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private List getOutputHandlerSelectList() {
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
                    tfos.add(new TwoFacedObject(type.getLabel(), type.getId()));
                    //tfos.add(new HU.Selector(HU.space(2)
		    //+ type.getLabel(), type.getId(), icon));
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
            sb.append(HU.sectionOpen(null, false));
            HU.open(sb, "div", HU.cssClass("ramadda-links"));
            addSearchByTypeList(request, sb,true,true,"",null,null,null);
            HU.close(sb, "div");
            sb.append(HU.sectionClose());
        } else {
            String      type        = lastTok;
            TypeHandler typeHandler = getRepository().getTypeHandler(type);
            if (typeHandler != null) {
                Result result =
                    typeHandler.getSpecialSearch().processSearchRequest(request, sb,new Hashtable());
                //Is it non-html?
                if (result != null) {
                    return result;
                }
            }
        }

        return makeResult(request, "Search by Type", sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addSearchByTypeList(Request request, Appendable sb,
				    boolean showHeader,
				    boolean showSearchField,
				    String listStyle,
				    HashSet<String> supers,
				    HashSet<String> cats, HashSet<String> types)
	throws Exception {

	
	String uid =  HU.getUniqueId("types");
	if(showSearchField) {
	    sb.append("<center>");
	    HU.script(sb,"HtmlUtils.initPageSearch('.type-list-item','#" + uid +" .type-list-container','Find Type')");
	    sb.append("</center>");
	}
	sb.append(HU.open("div","id",uid));
	sb.append("\n");
        for (EntryManager.SuperType superType :
		 getEntryManager().getCats(false)) {
	    if(supers!=null && !supers.contains(superType.getName())) continue;
            boolean didSuper = false;
            for (EntryManager.Types typeList : superType.getList()) {
		if(cats!=null && !cats.contains(typeList.getName())) continue;
                boolean didSub = false;
                for (TypeHandler typeHandler : typeList.getList()) {
		    if(types!=null && !types.contains(typeHandler.getType())) continue;
                    int cnt = getEntryUtil().getEntryCount(typeHandler);
                    if (cnt == 0) {
                        continue;
                    }
                    if (!didSuper && showHeader) {
                        didSuper = true;
			sb.append("<div class=type-group-container><div class='type-group-header'>"
				  + superType.getName()
				  + "</div>\n<div class=type-group>\n");
                    }
                    if ( !didSub) {
                        didSub = true;
			sb.append(HU.open("div","class","type-list-container"));
			sb.append("\n");
			HU.div(sb,typeList.getName(),HU.attrs("class","type-list-header"));
			sb.append("\n");
			sb.append(HU.open("div","class","type-list","style",listStyle));
			sb.append("\n");
                    }
                    String icon = typeHandler.getIconProperty(null);
                    String img;
                    if (icon == null) {
                        icon = ICON_BLANK;
                    }
		    img = HU.img(typeHandler.getIconUrl(icon), "", HU.attr(HU.ATTR_WIDTH, ICON_WIDTH));
                    String label = img + HU.SPACE
			+ typeHandler.getDescription() + HU.SPACE
			+ "(" + cnt + ")";
                    String href = HU.href(getRepository().getUrlBase()
						 + "/search/type/"
						 + typeHandler.getType(), label);
                    String help = "Search for "
			+ typeHandler.getDescription() + " - "
			+ cnt + " entries";
                    HU.div(sb, href,
                           HU.attrs("class", "type-list-item",
				    "data-category",
				    typeList.getName(),
				    "title",
                                    help));
                }
                if (didSub) {
                    sb.append("\n</div></div>\n");
                }
            }
            if (didSuper) {
                sb.append("\n</div></div>\n");
            }
        }
	sb.append(HU.close("div"));
	
    }

    public  String getTypeSearchUrl(TypeHandler typeHandler) {
	return URL_SEARCH_TYPE + "/" + typeHandler.getType();
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
        sb.append(HU.b("Entry Types"));
        sb.append(
		  HU.open(
				 "div",
				 HU.style("max-height: 300px;overflow-y:auto;")));
        sb.append(HU.formTable());
        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            String link =
                HU.href(getTypeSearchUrl(typeHandler),
                               typeHandler.getType());
            sb.append(HU.row(HU.cols(link,
						   typeHandler.getDescription())));
        }
        sb.append(HU.formTableClose());
        sb.append(HU.close("div"));
        sb.append(HU.close("p"));
        sb.append("<a name=outputtypes></a>");
        sb.append(HU.b("Output Types"));
        sb.append(
		  HU.open(
				 "div",
				 HU.style("max-height: 300px;overflow-y:auto;")));
        sb.append(HU.formTable());
        for (OutputHandler outputHandler :
		 getRepository().getOutputHandlers()) {
            for (OutputType type : outputHandler.getTypes()) {
                sb.append(HU.row(HU.cols(type.getId(),
						       type.getLabel())));
            }
        }
        sb.append(HU.formTableClose());
        sb.append(HU.close("div"));


        sb.append(HU.close("p"));
        sb.append("<a name=metadatatypes></a>");
        sb.append(HU.b("Metadata Types"));
        sb.append(
		  HU.open(
				 "div",
				 HU.style("max-height: 300px;overflow-y:auto;")));
        sb.append(header(msg("Metadata Types")));
        sb.append(HU.formTable());
        for (MetadataType type :
		 getRepository().getMetadataManager().getMetadataTypes()) {
            if ( !type.getSearchable()) {
                continue;
            }
            sb.append(HU.row(HU.cols(type.getId(),
						   type.getName())));
        }
        sb.append(HU.formTableClose());
        sb.append(HU.close("div"));

        getPageHandler().sectionClose(request, sb);

        return makeResult(request, "Search Metadata", sb);
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

        return makeResult(request, "Search Providers", sb);
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
                HU.href(getTypeSearchUrl(typeHandler),
                               typeHandler.getType());
            sb.append(HU.row(HU.cols(link,
						   typeHandler.getDescription())));
        }
        sb.append(HU.formTableClose());


        sb.append(header(msg("Output Types")));
        sb.append(HU.formTable());
        for (OutputHandler outputHandler :
		 getRepository().getOutputHandlers()) {
            for (OutputType type : outputHandler.getTypes()) {
                sb.append(HU.row(HU.cols(type.getId(),
						       type.getLabel())));
            }
        }
        sb.append(HU.formTableClose());


        sb.append(header(msg("Metadata Types")));
        sb.append(HU.formTable());
        for (MetadataType type :
		 getRepository().getMetadataManager().getMetadataTypes()) {
            if ( !type.getSearchable()) {
                continue;
            }
            sb.append(HU.row(HU.cols(type.getId(),
						   type.getName())));
        }
        sb.append(HU.formTableClose());
        return makeResult(request, "Search Metadata", sb);
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
        sb.append(HU.p());
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
            serverSB.append(HU.br());
            didone = true;
        }

        if ( !didone) {
            sb.append(
		      getPageHandler().showDialogNote(msg("No servers selected")));
        } else {
            sb.append(
		      HU.div(
				    serverSB.toString(),
				    HU.cssClass(CSS_CLASS_SERVER_BLOCK)));
            sb.append(HU.p());
        }
        sb.append(HU.p());
        return makeResult(request, "Remote Form", sb);

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
        HU.open(sb, "div", HU.cssClass("ramadda-links"));
        getMetadataManager().addToBrowseSearchForm(request, sb);
        HU.close(sb, "div");

        return makeResult(request, "Search Form", sb);
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
	if(searchProviderMap==null) searchProviderMap =     new Hashtable<String, SearchProvider>();    
        searchProviderMap.put(provider.getType(), provider);
        searchProviderMap.put(provider.getId(), provider);	
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
		boolean match= server.getId().equals(id); 
                if (!match) {
		    if(server.getId().startsWith("http")) {
			if(server.getId().indexOf("/" + id+"/")>=0) match=true;
		    }
		}
                if (match) {
                    provider = new SearchProvider.RemoteSearchProvider(getRepository(), server);
                    searchProviderMap.put(id, provider);
		    break;
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
	    if(this.getRepository().getProperty("ramadda.search.providers.show",true)) {
		for (SearchProvider provider : pluginSearchProviders) {
		    searchProviderMap.put(provider.getType(), provider);
		    searchProviderMap.put(provider.getId(), provider);	
		    if (provider.isEnabled()) {
			tmp.add(provider);
		    }
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
		searchProviderMap.put(provider.getId(), provider);		
	    }
            tmp.addAll(searchProviders);
            allProviders = tmp;
        }

        return allProviders;
    }



    public synchronized void clearSearchProviders() {
	searchProviderMap =     null;//new Hashtable<String, SearchProvider>();
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
    public List<Entry> doSearch(Request request, SelectInfo searchInfo)
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

	boolean didMulti = false;
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

	    didMulti = searchProviders.size()>1;
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



        if (allEntries.size() == 0) {
            if (request.defined(ARG_GROUP)) {
                String groupId = (String) request.getString(ARG_GROUP,
							    "").trim();
                Entry theGroup = getEntryManager().findGroup(request,
							     groupId);
                if ((theGroup != null)
		    && theGroup.getTypeHandler().isSynthType()) {
                    allEntries = getEntryManager().getChildrenAll(request, theGroup,
								  null);
                }
            }
        }
	//Sort them if needed
	String orderBy = request.getString(ARG_ORDERBY,null);
	if(didMulti && stringDefined(orderBy) && !orderBy.equals("none")) {
	    allEntries = getEntryUtil().sortEntriesOn(allEntries,request.getString(ARG_ORDERBY,""),
						      !request.get(ARG_ASCENDING,false));
	}

	return allEntries;
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
	//	System.err.println(request);

        if (request.get(ARG_WAIT, false)) {
            return getRepository().getMonitorManager().processEntryListen(
									  request);
        }
        if (request.defined("submit_type.x")
	    || request.defined(ARG_SEARCH_SUBSET)) {
            request.remove(ARG_OUTPUT);

            return processSearchForm(request);
        }

        SelectInfo       searchInfo = new SelectInfo(request);
        List<ServerInfo> servers    = null;

        ServerInfo       thisServer = getRepository().getServerInfo();
	long t1 = System.currentTimeMillis();
	List<Entry> children = doSearch(request, searchInfo);
	long t2 = System.currentTimeMillis();

	if(request.get("docount",false)) {
	    return new Result("count:" + children.size(), MIME_TEXT);
	}
	

	OutputHandler outputHandler = getRepository().getOutputHandler(request);

        StringBuilder header   = new StringBuilder();
	if(outputHandler.isHtml()) {
	    request.remove(ARG_SEARCH_SUBMIT);
	    boolean       foundAny = (children.size() > 0);
	    getPageHandler().sectionOpen(request, header, "Search", false);
	    getPageHandler().makeLinksHeader(request, header, getSearchUrls(), "");
	    makeSearchForm(request, header);
	    if ( !foundAny) {
		header.append(
			      getPageHandler().showDialogNote(msg("Sorry, nothing found")));
	    }
	    request.appendPrefixHtml(header.toString());
	}

        Entry theGroup = null;
        if (request.defined(ARG_GROUP)) {
            String groupId = (String) request.getString(ARG_GROUP, "").trim();
            theGroup = getEntryManager().findGroup(request, groupId);
        }
        if (theGroup == null) {
            theGroup = getEntryManager().getDummyGroup();
        }
	//	Utils.printTimes("Search.doSearch: #:" + children.size() +" times (search): ",t1,t2); 
        Result result =
            outputHandler.outputGroup(request,
				      request.getOutput(), theGroup,
				      children);
	if(!outputHandler.isHtml()) {
	    return result;
	}


        Result r;
        if (theGroup.isDummy()) {
            r = addHeaderToAncillaryPage(request, result);
        } else {
            header.append(HU.sectionOpen());
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

		    if(Utils.stringDefined(serverInfo.getSearchRoot())) {
			remoteSearchUrl+="&ancestor=" +serverInfo.getSearchRoot();
		    }

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
			Hashtable<String,Entry> entryMap  = new Hashtable<String,Entry>();
			Hashtable<String,File> filesMap =  new Hashtable<String,File>();
			for (int i = 0; i < children.getLength(); i++) {
			    Element node = (Element) children.item(i);
			    //                    if (!node.getTagName().equals(TAG_ENTRY)) {continue;}
			    List<Entry> entryList =
				getEntryManager().createEntryFromXml(request, node, parentEntry,
								     filesMap,
								     entryMap,
								     false,
								     EntryManager.TEMPLATE.YES,
								     EntryManager.INTERNAL.NO,
								     new StringBuilder());

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

    public Runnable makeRunnable(final Request theRequest,
                                 final SearchProvider provider,
                                 final List<Entry> entries,
                                 final SelectInfo searchInfo,
                                 final boolean[] running,
                                 final int[] runnableCnt)
	throws Exception {
        final Request request  = theRequest.cloneMe();
        Runnable      runnable = new Runnable() {
		public void run() {
		    try {
			List<Entry> results = provider.getEntries(request, searchInfo);
			if(provider!=thisSearchProvider)  {
			    getEntryManager().sanitizeEntries(results);
			}			    

			synchronized (entries) {
			    entries.addAll(results);
			}
		    } catch (Exception exc) {
			logException("Error doing search:" + provider, exc);
			getSessionManager().addSessionErrorMessage(request,"Error doing search at:" + provider+"<br>Error:" + exc.getMessage());

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
                item = HU.span(names[i], extra1);
            } else {
                item = HU.href(request.makeUrl(URL_SEARCH_FORM,
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
                links.add(HU.span(tfo.toString(), extra1));
            } else {
                links.add(HU.href(request.makeUrl(URL_SEARCH_FORM,
							 ARG_WHAT, BLANK + tfo.getId(), ARG_TYPE,
							 typeHandler.getType()), tfo.toString(), extra2));
            }
        }

        return links;
    }





}

/**
   Copyright (c) 2008-2025 Geode Systems LLC
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.sortedset.*;
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
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.AutoDetectParser;

import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.pdfbox.multipdf.Splitter;

@SuppressWarnings("unchecked")
public class SearchManager extends AdminHandlerImpl implements EntryChecker {

    public static final String PROP_INDEX_ACTION = "indexaction";

    public static boolean debugCorpus = false;
    public static boolean debugIndex = false;
    public static boolean debugSearch = false;

    public static final String SUFFIX_LATITUDE ="_latitude";
    public static final String SUFFIX_LONGITUDE ="_longitude";

    public static final String ARG_SEARCH_SUBMIT = "search.submit";
    public static final String ARG_PROVIDER = "provider";
    public static final String ARG_SEARCH_SUBSET = "search.subset";
    public static final String ARG_SEARCH_SERVERS = "search.servers";
    public final RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this,
							      "/search/do", "Search");

    public final RequestUrl URL_SEARCH_FORM = new RequestUrl(this,
							     "/search/form", "Form");

    public final RequestUrl URL_SEARCH_TYPE = new RequestUrl(this,
							     "/search/type", "By Type");

    public final RequestUrl URL_SEARCH_ASSOCIATIONS =
        new RequestUrl(this, "/search/associations/do", "Associations");

    public final RequestUrl URL_SEARCH_ASSOCIATIONS_FORM =
        new RequestUrl(this, "/search/associations/form",
                       "Search Associations");

    public final RequestUrl URL_SEARCH_BROWSE = new RequestUrl(this,
							       "/search/browse",
							       "Browse Metadata");
    public final RequestUrl URL_SEARCH_REMOTE_DO =
        new RequestUrl(this, "/search/remote/do", "Search Remote Servers");

    public final List<RequestUrl> searchUrls =
        RequestUrl.toList(new RequestUrl[] { URL_SEARCH_FORM,
					    URL_SEARCH_TYPE,
					    URL_SEARCH_BROWSE,
					    URL_SEARCH_ASSOCIATIONS_FORM });
    public final List<RequestUrl> remoteSearchUrls =
        RequestUrl.toList(new RequestUrl[] { URL_SEARCH_FORM,
					    URL_SEARCH_TYPE,
					    URL_SEARCH_BROWSE,
					    URL_SEARCH_ASSOCIATIONS_FORM });

    private static final String FIELD_ENTRYORDER ="entryorder";
    private static final String FIELD_SIZE ="size";
    private static final String FIELD_SUPERTYPE ="supertype";    
    private static final String FIELD_ENTRYID = "entryid";
    private static final String FIELD_PARENT = "parent";
    private static final String FIELD_ANCESTOR = "ancestor";    
    private static final String FIELD_PATH = "path";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_NORTH = "north";
    private static final String FIELD_WEST = "west";    
    private static final String FIELD_SOUTH = "south";
    private static final String FIELD_EAST = "east";
    private static final String FIELD_CORPUS = "corpus";
    private static final String FIELD_CONTENTS = "contents";
    private static final String FIELD_ATTACHMENT = "attachment";
    private static final String FIELD_DATE_CREATED = "date_created";
    private static final String FIELD_DATE_CHANGED = "date_changed";
    private static final String FIELD_DATE_START = "date_start";
    private static final String FIELD_DATE_END = "date_end";    
    private static final String FIELD_METADATA = "metadata";
    private static final String FIELD_PROPERTY = "property";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CREATOR = "entry_creator";    
    private static final String FIELD_NAME_SORT = "namesort";    
    private static final String[] SEARCH_FIELDS ={
	FIELD_CORPUS, FIELD_NAME, FIELD_CREATOR,FIELD_DESCRIPTION, FIELD_CONTENTS,FIELD_ATTACHMENT, FIELD_PATH};

    private Object LUCENE_MUTEX = new Object();
    public static final int FIELD_MAX_LENGTH = 32000;
    public static final int LUCENE_MAX_LENGTH = 25_000_000;
    private Directory luceneDirectory;
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

    public SearchManager(Repository repository)  {
        super(repository);
        repository.addEntryChecker(this);
        getAdmin().addAdminHandler(this);
	try {
	    luceneDirectory = new NIOFSDirectory(Paths.get(getStorageManager().getLuceneDir().toString()));
	} catch(Exception exc) {
	    throw new RuntimeException(exc);

	}
    }

    public  void debug(String msg) {
	if(debugSearch) getLogManager().logSpecial(msg);
    }

    @Override
    public void initAttributes() {
        super.initAttributes();
        debugSearch = getRepository().getProperty("ramadda.search.debug",debugSearch);
        showMetadata = getRepository().getProperty(PROP_SEARCH_SHOW_METADATA, true);
	tesseractPath = getRepository().getScriptPath("ramadda.tesseract");
	indexImages = getRepository().getProperty("ramadda.indeximages",false);
	String tikaConfig =  getRepository().getProperty("ramadda.tika.config",null);
	if(tikaConfig!=null ) {
	    System.err.println("SearchManager: set config file:" + tikaConfig);
	    TikaUtil.setConfigFile(new File(tikaConfig));
	}

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

    public boolean includeMetadata() {
	return showMetadata;
    }

    private IndexWriter getLuceneWriter() throws Exception {
	if(luceneWriter==null) {
	    synchronized(LUCENE_MUTEX) {
		if(luceneWriter==null) {
		    Directory index = new NIOFSDirectory(Paths.get(getStorageManager().getLuceneDir().toString()));
		    IndexWriterConfig config = new IndexWriterConfig();
		    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		    luceneWriter = new IndexWriter(index, config);
		}
	    }
	}
	return luceneWriter;
    }

    public void reindexLucene(Request request,Object actionId, String type, boolean deleteAll)  {
	try {
	    IndexWriter indexWriter = getLuceneWriter();
	    try {
		reindexLuceneInner(request, indexWriter, actionId, type,deleteAll);
	    } finally {
		//		writer.close();
	    }
	} catch(Throwable thr) {
	    thr.printStackTrace();
	    throw new RuntimeException(thr);
	}
    }

    private void reindexLuceneInner(final Request request, final IndexWriter indexWriter, Object actionId, String type,
				    boolean deleteAll)  throws Throwable {	
	Clause clause = null;
	if(stringDefined(type)) {
	    clause = Clause.or(getDatabaseManager().addTypeClause(getRepository(),request, Utils.split(type,",",true,true),null));
	}
	if(deleteAll) {
	    indexWriter.deleteAll();
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

	for(String id: ids)  {
	    indexWriter.deleteDocuments(new Term(FIELD_ENTRYID, id));
	}
	commit(indexWriter);

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
			    getLogManager().logSpecial("#" + cnt[0] +"/"+ total +" entry:" + entry.getName());
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
								    "Reindexed " + cnt[0] +
								    "/"+ total +
								    " entries");
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

    public List<RequestUrl> getAdminUrls() {
        return null;
    }

    public String getId() {
        return "searchmanager";
    }

    public String getMetadataField(String  type) {
	return FIELD_METADATA+"_"+ type;
    }

    public String getPropertyField(TypeHandler  handler,Column column) {
	//We used to use the main type handler to get the field name
	//return   = getPropertyField(handler,column.getName());
	//but now we use the type handler of the column
	return  getPropertyField(column.getTypeHandler(),column.getName());		    
    }

    public String getPropertyField(TypeHandler  handler,String  type) {
	return FIELD_PROPERTY+"_"+ handler.getType()+"_"+type;
    }    

    private  void indexEntries(List<Entry> entries, final Request request, boolean isNewCall)
	throws Exception {
	if(entries.size()==0) return;
	IndexWriter indexWriter = getLuceneWriter();
	final SessionMessage message[]={null};
	Object actionId = null;
	String cancel = null;
	if(isNewCall) {
	    ActionManager.Action action = new ActionManager.Action(entries.get(0)) {
		    @Override
		    public String getRedirectUrl() {
			return getEntryManager().getEntryUrl(request, entry);
		    }
		    @Override
		    public void setRunning(boolean state) {
			//call super because there might be a future that needs cancelling
			super.setRunning(state);
			if(!state && message[0]!=null) {
			    getSessionManager().clearSessionMessage(request,message[0]);
			}
		    }
		};
	    actionId = getActionManager().addAction("","",null,action);
	    request.putExtraProperty(PROP_INDEX_ACTION, action);
	    cancel = HU.href(getActionManager().getCancelUrl(request, actionId),
			     HU.span(HU.image(ICON_CANCEL),HU.attrs("class","ramadda-clickable","style","margin-right:5px;")),
			     HU.attrs("title","Cancel processing"));
	    message[0] =  getSessionManager().addStickySessionMessage(request,
								      (entries.size()==1?
								       cancel+"Processing " + getLink(request,entries.get(0)):
								       cancel +"Processing " + entries.size() +" entries"));
	}
	try {
	    int cnt = 0;
	    for (Entry entry : entries) {
		if(actionId!=null && !getActionManager().getActionOk(actionId)) break;
		cnt++;
		long t1= System.currentTimeMillis();
		if(message[0]!=null) {
		    if(entries.size()>1)
			message[0].setMessage(cancel+"Processing " + cnt +" of " + entries.size() +" entries: " + getLink(request,entry));
		}
		indexEntry(indexWriter, entry, request,isNewCall);
		long t2= System.currentTimeMillis();
		//		    System.err.println("indexEntry:" + entry +" time:" + (t2-t1));
	    }
	    //        indexWriter.optimize();
	    commit(indexWriter);
	} finally {
	    if(actionId!=null)
		getActionManager().removeAction(actionId);		    
		   
	    if(message[0]!=null) {
		getSessionManager().clearSessionMessage(request,message[0]);
	    }
	    //	    indexWriter.close();
	}
    }

    int rcnt=0;
    private void indexEntry(IndexWriter indexWriter, Entry entry, Request request, boolean isNew)
	throws Exception {
        org.apache.lucene.document.Document doc =
            new org.apache.lucene.document.Document();
	StringBuilder corpus = new StringBuilder();

        doc.add(new StringField(FIELD_ENTRYID, entry.getId(), Field.Store.YES));
	//        doc.add(new StringField(FIELD_TYPE, entry.getTypeHandler().getType(), Field.Store.YES));	

	TypeHandler parentType = entry.getTypeHandler();
	doc.add(new SortedSetDocValuesFacetField(FIELD_TYPE,parentType.getType()));
	while(parentType!=null) {
	    doc.add(new StringField(FIELD_SUPERTYPE, parentType.getType(), Field.Store.YES));	
	    parentType = parentType.getParent();
	}

	//	System.err.println("index:" + entry);
	if(entry.getParentEntryId()!=null) {
	    doc.add(new StringField(FIELD_PARENT, entry.getParentEntryId(), Field.Store.YES));	
	    Entry parent = entry;
	    while(parent!=null) {
		//		System.err.println("\tancestor:" + parent.getId() +  " "+ parent.getName());
		doc.add(new StringField(FIELD_ANCESTOR, parent.getId(), Field.Store.YES));	
		parent = parent.getParentEntry();
	    }
	}
	//	System.err.println("add size:" + entry +" " +entry.getResource().getFileSize());

        doc.add(new SortedNumericDocValuesField(FIELD_SIZE, entry.getResource().getFileSize()));
	doc.add(new LongPoint(FIELD_SIZE, entry.getResource().getFileSize()));
	doc.add(new SortedNumericDocValuesField(FIELD_ENTRYORDER, entry.getEntryOrder()));
	if(entry.hasAreaDefined(request)) {
	    doc.add(new DoublePoint(FIELD_NORTH, entry.getNorth(request)));
	    doc.add(new DoublePoint(FIELD_WEST, entry.getWest(request)));
	    doc.add(new DoublePoint(FIELD_SOUTH, entry.getSouth(request)));
	    doc.add(new DoublePoint(FIELD_EAST, entry.getEast(request)));
	} else if(entry.hasLocationDefined(request)) {
	    doc.add(new DoublePoint(FIELD_NORTH, entry.getLatitude(request)));
	    doc.add(new DoublePoint(FIELD_WEST, entry.getLongitude(request)));
	    doc.add(new DoublePoint(FIELD_SOUTH, entry.getLatitude(request)));
	    doc.add(new DoublePoint(FIELD_EAST, entry.getLongitude(request)));
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
	corpus.append(entry.getTypeHandler().getDescription());
	corpus.append(" ");

	Entry parent = entry.getParentEntry();
	if(parent!=null) {
	    corpus.append(parent.getName());
	    corpus.append(" ");
	}

        doc.add(new TextField(FIELD_DESCRIPTION, _desc,Field.Store.NO));

	List<Column> columns = entry.getTypeHandler().getColumns();
	if (columns != null) {
	    Object[] values = entry.getTypeHandler().getEntryValues(entry);
	    if(values!=null) {
		for (Column column : columns) {
		    if (!column.getCanSearch()) continue;
		    String field  = getPropertyField(entry.getTypeHandler(),column);
		    Object v= entry.getValue(request,column);
		    if(v==null) continue;
		    //TODO handle latlonbox
		    if(debugIndex) System.err.println("\tindexing column:" + column +" field:"  + field);
		    if(column.isLatLon()) {
			double[] latlon = column.getLatLon(request,values);
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
			if(debugIndex) System.err.println("\t\tenum value=" + v);
			doc.add(new StringField(field, v.toString(),Field.Store.YES));
			if(column.getCanSort())
			    doc.add(new SortedDocValuesField(field+"_sort", new BytesRef(v.toString())));
		    }
		    else if(column.isDouble())  {
			double value = (Double)v;
			if(!Double.isNaN(value)) {
			    doc.add(new DoublePoint(field, (Double)v));
			    if(column.getCanSort())
				doc.add(new SortedNumericDocValuesField(field+"_sort",
									Double.doubleToRawLongBits(Utils.getDouble(v))));
			}

		    }   else if(column.isInteger())  {
			doc.add(new IntPoint(field, (Integer)v));		    
			if(column.getCanSort()) {
			    doc.add(new SortedNumericDocValuesField(field+"_sort", (Integer)v));
			}
		    } else {
			String s = v.toString();
			if(column.getTokenizeSearch()) {
			    doc.add(new TextField(field+"_exact", s,Field.Store.NO));
			}			    

			s = s.toLowerCase();
			corpus.append(s);
			corpus.append(" ");

			if(s.length()>FIELD_MAX_LENGTH) {
			    s = s.substring(0,FIELD_MAX_LENGTH-1);
			}
			doc.add(new StringField(field, s,Field.Store.NO));

			if(column.getCanSort()) {
			    doc.add(new SortedDocValuesField(field+"_sort", new BytesRef(v.toString())));
			}
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
			entryChanged |= getLLMManager().applyEntryExtract(request, entry, llmCorpus);
		    } catch(Throwable thr) {
			//log the error and carry on
			getSessionManager().addSessionMessage(request,
								   "An error occurred doing the LLM extraction for the entry: " + entry.getName()+
								   "<br><b>Error</b>: " + thr.getMessage());

		    }
		}

		if(entryChanged) {
		    List<Entry> tmp = new ArrayList<Entry>();
		    tmp.add(entry);
		    request.putExtraProperty("reindexing","true");
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

	if(debugIndex) {
	    System.err.println("indexing:" + entry);
	}
	//	MetadataManager.debugGetMetadata = true;
	//	getMetadataManager().getMetadata(request,entry);
	//	MetadataManager.debugGetMetadata = false;	
	//	debugIndex=true;

        for (Metadata metadata : getMetadataManager().getMetadata(request,entry)) {
	    MetadataType type = getMetadataManager().getType(metadata);
	    if(type==null) {
		if(debugIndex) {
		    System.err.println("\tno metadata type:" + metadata);
		}
		continue;
	    }
	    if(debugIndex) {
		System.err.println("\tmetadata:" + metadata);
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
	    if(!type.getSearchable()) {
		if(debugIndex) {
		    System.err.println("\tnot searchable:" + metadata);
		}
		continue;
	    }

	    //Don't add to corpus as the TypeHandler.getTextCorpus above does that for us
	    //		System.err.println("MTD:" + metadata.getAttr1().toLowerCase());
	    //		corpus.append(metadata.getAttr1().toLowerCase());
	    //		corpus.append(" ");
	    for(MetadataElement element: getMetadataManager().getSearchableElements(type)) {
		String fieldId = getMetadataField(type.getId()+"_"+element.getIndex());
		String fieldValue = metadata.getAttr(element.getIndex());
		if(element.isEnumeration()) {
		    if(debugIndex)
			System.err.println("enum:" + fieldId +" v:" + fieldValue);
		    doc.add(new StringField(fieldId, fieldValue,Field.Store.NO));
		} else {
		    if(debugIndex)
			System.err.println("string:" + fieldId +" v:" + fieldValue);
		    doc.add(new TextField(fieldId, fieldValue,Field.Store.NO));
		}
	    }
	}

        doc.add(new TextField(FIELD_CORPUS, corpus.toString(),Field.Store.NO));
	FacetsConfig config = new FacetsConfig();
	//        indexWriter.addDocument(doc);
        indexWriter.addDocument(config.build(doc));
    }

    /**
       is the file a pdf, doc, ppt, etc
     */
    public boolean isDocument(Entry entry, String path) {
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
	     path.endsWith("txt") ||	     
	     path.endsWith("pptx") ||	   	   
	     path.endsWith("docx"))) {
	    //	    System.err.println("not doc:" + path);
	    if(entry!=null)
		return isTextFile(entry, entry.getResource().getPath()) ||
		    entry.getTypeHandler().getTypeProperty("canbeindexed",false);

	    return false;
	}
	return true;
    }

    private static TikaConfig getTikaConfigTest() throws Exception {
	if(true)    return TikaUtil.getConfig();
	File f = new File("tika.xml");
	if(!f.exists()) {
	    f = new File("/mnt/ramadda/ramaddahome/tika.xml");
	}
	System.err.println("Tika config:" + f);
	return new TikaConfig(new FileInputStream(f));
    }

    private TikaConfig getTikaConfig(boolean force) throws Exception {
	if(force || indexImages){
	    //	    System.err.println("using TikaConfig");
	    return TikaUtil.getConfig();
	} 
	//	System.err.println("using TikaConfig no image");
	return TikaUtil.getConfigNoImage();
    }

    public String readContents(Request request, Entry entry,
				File f,List<org.apache.tika.metadata.Metadata> metadataList) throws Exception {
	boolean isImage = Utils.isImage(f.getName());
	if(isImage) {
	    if(!request.get(ARG_DOOCR,false) || !isImageIndexingEnabled()) {
		if(debugCorpus)
		    System.err.println("SearchManager.readContents: Not indexing images:" + f.getName());
		return null;
	    }
	    try {
		long t1= System.currentTimeMillis();
		File tmp  =getStorageManager().getUniqueScratchFile("output");
		List<String> commands = new ArrayList<String>();

		Object actionId = request.getExtraProperty("actionid");
		if(actionId!=null) {
		    getActionManager().setActionMessage(actionId,
							"Running image-to-text on: " +entry.getName());

		}

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
		return imageText;
	    } catch(Exception exc) {
		getLogManager().logError("Error running tesseract for:" + f.getName(), exc);
		return null;
	    }
	}

	if(!isDocument(entry, f.getName())) {
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: Not a document:" + f.getName());
	    return null;
	}

	//Don't do really big files 
	if(f.length()>LUCENE_MAX_LENGTH) {
	    if(debugCorpus)
		System.err.println("SearchManager.readContents file too big: " + f.getName() +" " +f.length());
	}

	if(f.length()==0) {
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: empty file: " + f.getName());
	    return null;
	}

	//	System.err.println("Calling TypeHandler.getCorpus");
	//	String corpus = entry.getTypeHandler().getCorpus(request, entry,CorpusType.SEARCH);
	//	if(corpus!=null) return corpus;
	String corpus = null;
	corpus=	    extractCorpus(request, entry,f.toString(),metadataList);
	return corpus;
    }	

    public File getCorpusFile(Request request,Entry entry) {
	return getCorpusFile(request, entry,  entry.getResource().getPath());
    }


    public File getCorpusFile(Request request,Entry entry,String path) {
	File f = new File(path);
	String corpusFileName = "corpus_" + f.length()+"_"+f.getName()+".txt";
	File entryDir = getStorageManager().getEntryDir(entry.getId(), true);
	if(!entryDir.exists()) {
	    System.err.println("ENTRY DIR DOES NOT EXIST:" + entryDir);
	}
        File corpusFile = new File(IOUtil.joinDir(entryDir, corpusFileName));
	return corpusFile;
    }

    public boolean corpusExists(Request request, Entry entry) {
	return getCorpusFile(request,entry).exists();
    }	


    public String extractCorpus(Request request, Entry entry,
				String path,
				List<org.apache.tika.metadata.Metadata> metadataList) throws Exception {
	File f = new File(path);
        File corpusFile = getCorpusFile(request, entry,path);
	if(!request.get(ARG_CORPUS_FORCE,false) && corpusFile.exists()) {
	    //check if the we are doing OCR and the corpus file is empty
	    if(request.get(ARG_DOOCR,false) && corpusFile.length()==0) {
	    } else {
		if(debugCorpus)
		    System.err.println("SearchManager.readContents: corpus file exists and is not empty: length:" + corpusFile.length()+" -- " + corpusFile);
		return  IO.readContents(corpusFile.toString(), SearchManager.class);
	    }
	} 

	if(debugCorpus) {
	    System.err.println("corpus file:" + corpusFile + " exists:" +
			       corpusFile.exists() +" length:" + corpusFile.length());
	}

	if(!f.exists() && path.startsWith("http")) {
	    String url = path;
	    IO.Result result = IO.getHttpResult(IO.HTTP_METHOD_GET,new URL(url),"");
	    if(result.getError()) return null;
	    return result.getResult();
	}

	if(!f.exists()) {
	    return null;
	}
	boolean reIndexing =   request.getExtraProperty("reindexing")!=null;
	String sessionMessage = "Extracting text from: " + getLink(request,entry);
	try {
	    if(!reIndexing) {
		getSessionManager().addRawSessionMessage(request,sessionMessage,entry.getId());
	    }

	    long t1 = System.currentTimeMillis();
	    boolean doOcr = request.get(ARG_DOOCR,false);
	    boolean doOcrConditional = request.get(ARG_DOOCR_CONDITIONAL,false);	    
	    String corpus=null;
	    List<org.apache.tika.metadata.Metadata> tmpList = new  ArrayList<org.apache.tika.metadata.Metadata>();
	    /*
	      if conditional then try to read the corpus without OCR. If we don't get anything then try it with OCR
	     */
	    if(debugCorpus)
		System.err.println("SearchManager.readContents: " + entry.getName());
	    if(doOcrConditional) {
		corpus = readCorpus(request, entry,f,tmpList,false);
		if(corpus==null) corpus="";
		corpus  = corpus.trim();
		if(corpus.length()==0) {
		    if(debugCorpus)
			System.err.println("\tCould not read corpus without OCR");
		    corpus=null;
		    tmpList = new  ArrayList<org.apache.tika.metadata.Metadata>();
		} else {
		    if(debugCorpus)
			System.err.println("\tDid read without OCR");
		    doOcr = false;
		}
	    }
	    if(corpus==null) {
		if(debugCorpus && doOcr)
		    System.err.println("\tdoing OCR");		
		corpus = readCorpus(request, entry,f,tmpList,doOcr);
	    }
	    if(metadataList!=null) metadataList.addAll(tmpList);
	    long t2= System.currentTimeMillis();
	    if(corpus==null) corpus="";
	    corpus  = corpus.trim();
	    if(debugCorpus)
		System.err.println("\tcorpus:" + " time:" + (t2-t1)+" length:" +
				   corpus.length() +" corpus:" + Utils.clip(corpus,50,"...").replace("\n"," "));

	    //	    System.err.println("CORPUS FILE:" + corpusFile);
	    IOUtil.writeBytes(corpusFile, corpus.getBytes());
	    return  corpus;
	} catch(java.util.concurrent.CancellationException cancel) {
	    getSessionManager().addSessionMessage(request,"Text extraction has been cancelled");
	    return null;
	}  catch(Throwable exc) {
	    getLogManager().logError("Error extracting text corpus for entry:" + entry +" file:" + f.getName() +" error:" + exc,exc);
	    getSessionManager().addRawSessionMessage(request,"There was an error extracting text from the entry: " + getLink(request,entry) +
						  " error: " + exc.getMessage());
	    return null;
	} finally {
	    if(!reIndexing) {
		getSessionManager().clearSessionMessage(request,entry.getId(),null);
	    }
	}	
    }

    private String readCorpusInner(final Request request, final Entry entry,final File f,
				   final List<org.apache.tika.metadata.Metadata> metadataList,
				   final boolean doOcr) throws Exception {
	try(InputStream stream = getStorageManager().getFileInputStream(f)) {
	    BufferedInputStream bis = new BufferedInputStream(stream);
            org.apache.tika.metadata.Metadata metadata =   new org.apache.tika.metadata.Metadata();
	    if(metadataList!=null)
		metadataList.add(metadata);
	    TikaConfig config = getTikaConfig(doOcr);
	    Parser parser;
	    if(f.getName().toLowerCase().endsWith("pdf")) {
		PDFParser pdfParser = new PDFParser();
		if(doOcr) {
		    pdfParser.setOcrStrategy("OCR_AND_TEXT_EXTRACTION");
		}
		parser = pdfParser;
	    } else {
		parser = new AutoDetectParser(config);
	    }
	    ParseContext parseContext = new ParseContext();
            parseContext.set(TikaConfig.class, config);
	    BodyContentHandler handler =  new BodyContentHandler(LUCENE_MAX_LENGTH);	
	    try {
		parser.parse(bis, handler, metadata, parseContext);
	    } catch(org.apache.tika.exception.WriteLimitReachedException ignore) {
	    } 
	    return  handler.toString();
	} catch(Exception exc) {
	    throw exc;
	}
    }

    private String readCorpus(final Request request, final Entry entry,final File f,
			      final List<org.apache.tika.metadata.Metadata> metadataList,
			      final boolean doOcr) throws Exception {
	if(!doOcr) {
	    return readCorpusInner(request,entry,f,metadataList,doOcr);
	}
	final ExecutorService executor = Executors.newSingleThreadExecutor();
	final Future<String> future = executor.submit(() -> {
		return readCorpusInner(request,entry,f,metadataList,doOcr);
	    });

	final boolean[] running = {true};
	ActionManager.Action action = (ActionManager.Action) request.getExtraProperty(PROP_INDEX_ACTION);
	Object actionId = null;
	SessionMessage sessionMessage = null;
	if(action==null) {
	    action = new ActionManager.Action(entry) {
		@Override
		public String getRedirectUrl() {
		    return getEntryManager().getEntryUrl(request, entry);
		}

		@Override
		public void setRunning(boolean state) {
		    if(!state && running[0] && this.future!=null) {
			this.future.cancel(true);
		    }
		}
	    };
	    actionId = getActionManager().addAction("","",null,action);
	    String cancelUrl = getActionManager().getCancelUrl(request, actionId);
	    sessionMessage = getSessionManager().addStickySessionMessage(request,entry.getId(),
											HU.href(cancelUrl,"Cancel text extraction"));
	}
	
	action.setFuture(future);
	try {
	    String contents =  future.get();
	    action.setFuture(null);
	    return contents;
	} finally {
	    running[0] = false;
	    if(sessionMessage!=null)
		getSessionManager().clearSessionMessage(request,sessionMessage);
	    //if this was our action then we remove it
	    if(actionId!=null) {
		getActionManager().removeAction(actionId);
	    }
		
	}

    }

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

    private Object SEARCHER_MUTEX = new Object();
    private synchronized IndexSearcher getLuceneSearcher() throws Exception {
	synchronized(SEARCHER_MUTEX) {
	    if(luceneSearcher==null) {
		IndexReader reader = DirectoryReader.open(luceneDirectory);
		luceneSearcher = new IndexSearcher(reader);
	    }
	    DirectoryReader reader = DirectoryReader.openIfChanged((DirectoryReader) luceneSearcher.getIndexReader());
	    if (reader != null) {
		luceneSearcher = new IndexSearcher(reader);
	    }
	    return luceneSearcher;
	}
	/*	if(luceneSearcher==null) {
	    IndexReader reader = DirectoryReader.open(luceneDirectory);
	    luceneSearcher = new IndexSearcher(reader);
	    //	    luceneSearcher = new IndexSearcher(DirectoryReader.open(getLuceneWriter()));
	}
	return luceneSearcher;
	*/
    }

    public Result processEntrySuggest(Request request) throws Exception {
        List<String> names  = new ArrayList<String>();
	request.put(ARG_MAX,20);
	for(Entry entry:  getEntryManager().searchEntries(request)) {
	    String obj = JsonUtil.map(Utils.makeListFromValues("name", JsonUtil.quote(entry.getName()), "id",
						     JsonUtil.quote(entry.getId()),
						     "type",JsonUtil.quote(entry.getTypeHandler().getType()),
						     "typeName",JsonUtil.quote(entry.getTypeHandler().getLabel()),
						     "icon",
						     JsonUtil.quote(entry.getTypeHandler().getTypeIconUrl())));
	    names.add(obj);
	}
	String json = JsonUtil.map(Utils.makeListFromValues("values", JsonUtil.list(names)));
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

    private Query makeAnd(List<Query>queries) {
	BooleanQuery.Builder builder = new BooleanQuery.Builder();
	for(Query query: queries) {
	    builder.add(query, BooleanClause.Occur.MUST);
	}
	return builder.build();
    }        



    
    public List<EntryUtil.EntryCount> getEntryCounts(Request request) throws Exception {
	long t1 = System.currentTimeMillis();
	try {
	    List<EntryUtil.EntryCount> counts =  getEntryCountsInner(request);
	    long t2 = System.currentTimeMillis();
	    Utils.printTimes("getEntryCounts: #" + counts.size() +" time:" ,t1,t2);
	    return counts;

	} catch(Exception ignore) {
	    //for old index without any faceting
	    if(ignore.toString().indexOf("$facets")<0) throw ignore;
	    return new ArrayList<EntryUtil.EntryCount>();
	}
    }

    private List<EntryUtil.EntryCount> getEntryCountsInner(Request request) throws Exception {
	
	IndexSearcher searcher = getLuceneSearcher();
	IndexReader reader = searcher.getIndexReader();
	FacetsConfig config = new FacetsConfig();
	FacetsCollector fc = new FacetsCollector();
	boolean[]hasArea={false};
	Query query = makeQuery(request,hasArea);
	//	System.err.println("Query:" + query);
	searcher.search(query, fc);  
	SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(reader);
	Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
	int max = 100_000;
	FacetResult result = facets.getTopChildren(max, FIELD_TYPE);
	List<EntryUtil.EntryCount> counts = new ArrayList<EntryUtil.EntryCount>();
	if(result==null) return counts;
        for (LabelAndValue lv : result.labelValues) {
	    TypeHandler typeHandler = getRepository().getTypeHandler(lv.label);
	    if(typeHandler!=null) {
		counts.add(new EntryUtil.EntryCount(typeHandler,lv.value.intValue()));
	    }
        }
	return counts;
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

    /** Lower case the values */
    private Term makeTerm(String field, String value) {
	return new Term(field, value.toLowerCase());
    }

    private Query makeTextQuery(String field, String s) {
	s = s.trim().toLowerCase();
	List<Query> ands = new ArrayList<Query>();
	List<String> toks = Utils.splitWithQuotes(s);
	for(String tok:toks) {
	    List<String> toks2 = Utils.split(tok," ",true,true);
	    if(toks2.size()==1) {
		ands.add(new BoostQuery(new WildcardQuery(makeTerm(field, toks2.get(0))),6));
	    } else {
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		for(String tok2:toks2) {
		    builder.add(makeTerm(field,tok2));
		}
		ands.add(builder.build());
	    }
	}
	return makeAnd(ands);
    }

    public void processSearchUrl(Request request, List<Entry> entries, String url) throws Exception {
	Request searchRequest = new Request(getRepository(),request.getUser());
	List<String> args = IO.parseArgs(url);
	for(int i=0;i<args.size();i+=2) {
	    String key = args.get(i);
	    String value = args.get(i+1);
	    searchRequest.put(key,value,false);
	}
	processLuceneSearch(searchRequest,entries);
    }

    public Query makeQuery(Request request,boolean[]hasArea)
	throws Exception {
	List<Query> queries = new ArrayList<Query>();

	String text = request.getUnsafeString(ARG_TEXT,"");
	String searchField = null;
	for(String field: SEARCH_FIELDS) {
	    if(text.indexOf(field+":")>=0) {
		searchField = field;
		text = text.substring((field.length()+1));
		break;
	    }
	}

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
				    phraseBuilder.add(makeTerm(field, pword));
				}
				term = phraseBuilder.build();
			    } else {
				term = new WildcardQuery(makeTerm(field, word));		
			    }
			    if(isName) term = new BoostQuery(term,6);
			    orBuilder.add(term, BooleanClause.Occur.SHOULD);
			}
			multiBuilder.add(orBuilder.build(),BooleanClause.Occur.MUST);
		    }
		    builder.add(multiBuilder.build(),BooleanClause.Occur.SHOULD);
		} else {
		    Query term = new WildcardQuery(makeTerm(field, text));		
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
	    queries.add(makeTextQuery(FIELD_NAME,name));
	}
	String description = request.getUnsafeString(ARG_DESCRIPTION,null);
	if(stringDefined(description)) {
	    queries.add(makeTextQuery(FIELD_DESCRIPTION,description));
	}	
	long dateMin = Long.MIN_VALUE;
	long dateMax = Long.MAX_VALUE;



	for (DateArgument arg : DateArgument.SEARCH_ARGS) {
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
		    queries.add(SortedNumericDocValuesField.newSlowRangeQuery(field, date1!=null?date1.getTime():dateMin,
									      date2!=null?date2.getTime():dateMax));
		}
		continue;
	    }
	    long t1 = date1==null?dateMin:date1.getTime();
	    long t2 = date2==null?dateMax:date2.getTime();	    
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
	hasArea[0] = makeAreaQueries(rectangles, queries, contains,FIELD_NORTH,FIELD_WEST,FIELD_SOUTH,FIELD_EAST);

	int sizeMin =  request.get(ARG_SIZE_MIN,-1);
	int sizeMax =  request.get(ARG_SIZE_MAX,-1);	
	if(sizeMin>=0|| sizeMax>=0) {
	    queries.add(LongPoint.newRangeQuery(FIELD_SIZE,sizeMin>=0?sizeMin:Integer.MIN_VALUE,sizeMax>=0?sizeMax:Integer.MAX_VALUE));
	}

	String user = request.getString(ARG_USER_ID,null);
	if(Utils.stringDefined(user)) {
	    queries.add(new TermQuery(makeTerm(FIELD_CREATOR, user)));
	}

	String mainAncestor = request.getString("mainancestor",null);
	if(mainAncestor!=null) {
	    queries.add(new TermQuery(new Term(FIELD_ANCESTOR, mainAncestor)));
	}
	List<String> ancestors = request.get(ARG_ANCESTOR+"_hidden", request.get(ARG_ANCESTOR,(List<String>)null));
	if(ancestors!=null) {
	    List<Query> ors = new ArrayList<Query>();
	    for(String ancestor: ancestors) {
		if(stringDefined(ancestor)) {
		    ors.add(new TermQuery(new Term(FIELD_ANCESTOR, ancestor)));
		}
	    } 
	    if(ors.size()>1) {
		queries.add(makeOr(ors));
	    } else if(ors.size()==1) {
		queries.add(ors.get(0));
	    }
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
        String metadataPrefix = ARG_METADATA_ATTR;
	Hashtable<MetadataType,MetadataTypeSearchInfo> mmap =new Hashtable<MetadataType,MetadataTypeSearchInfo>();
	List<MetadataTypeSearchInfo>infos = new ArrayList<MetadataTypeSearchInfo>();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(metadataPrefix)) {
                continue;
            }
            if ( !request.defined(arg)) {
                continue;
            }

	    List<String> values = (List<String>) request.get(arg,new ArrayList());
            arg = arg.substring(metadataPrefix.length());
	    List<String> toks = Utils.splitUpTo(arg,"_",2);
	    if(toks.size()!=2) continue;

            String type = toks.get(1);
	    int index = Integer.parseInt(toks.get(0));
	    MetadataType metadataType=getMetadataManager().findType(type);
	    if(metadataType==null) {
		System.err.println("Search error: could not find metadata type:" + type);
		continue;
	    }

	    MetadataElement element = metadataType.getElement(index-1);
	    if(element==null) {
		System.err.println("Search error: could not find metadata element:" + type +" index:" + index);
		continue;
	    }

	    MetadataTypeSearchInfo typeInfo =  mmap.get(metadataType);
	    if(typeInfo==null) {
		mmap.put(metadataType,typeInfo = new MetadataTypeSearchInfo(this,metadataType));
		infos.add(typeInfo);
	    }
	    MetadataElementSearchInfo info =  typeInfo.get(element);
	    for(String value: values) {
		info.addValue(value);
	    }
	}
	for(MetadataTypeSearchInfo typeInfo: infos) {
	    List<Query> metadataQueries=new ArrayList<Query>();
	    for(MetadataElementSearchInfo info: typeInfo.elements) {
		metadataQueries.add(info.makeQuery());
	    }
	    if(metadataQueries.size()==1) {
		queries.add(metadataQueries.get(0));
	    }	 else {
		queries.add(makeAnd(metadataQueries));
	    }
	}
	if(request.defined(ARG_TYPE)) {
	    List<Query> typeQueries = new ArrayList<Query>();
	    for(TypeHandler typeHandler: getRepository().getTypes(request.getUnsafeString(ARG_TYPE))) {
		String type = typeHandler.getType();
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
		    String field = getPropertyField(typeHandler,column);
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
		    } else if(column.isDate()) {
			Date[] dateRange = request.getDateRange(searchArg+"_from",   searchArg+"_to", null);
			Date date1 = dateRange[0];
			Date date2 = dateRange[1];
			if(date1!=null || date2!=null) {
			    System.err.println("date:" + date1 +" d2:" + date2);
			    queries.add(SortedNumericDocValuesField.newSlowRangeQuery(field, date1!=null?date1.getTime():dateMin,
										      date2!=null?date2.getTime():dateMax));
			}
			continue;
		    } else {
			String s = request.getUnsafeString(searchArg,null);
			if(!Utils.stringDefined(s)||s.equals(TypeHandler.ALL)) continue;
			String v = s.toLowerCase();
			List<Query> ors = new ArrayList<Query>();
			if(column.getTokenizeSearch()) {
			    ors.add(new TermQuery(new Term(field+"_exact", s)));
			}			    
			v = v.toLowerCase();
			if(v.indexOf(" ")>=0) {
			    PhraseQuery.Builder phraseBuilder = new PhraseQuery.Builder();
			    for(String tok: Utils.split(v," ",true,true)) {
				phraseBuilder.add(makeTerm(field, tok));
			    }
			    ors.add(phraseBuilder.build());
			} else {
			    ors.add(new WildcardQuery(makeTerm(field, v)));
			}
			if(ors.size()==1) 
			    term = ors.get(0);
			else if(ors.size()>1)
			    term =  makeOr(ors);
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

	//	System.err.println("queries:" + queries);

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
	return query;
    }



    int scnt=0;
    public void processLuceneSearch(Request request, List<Entry> entries)
	throws Exception {
	boolean[]hasArea={false};
	Query query = makeQuery(request,hasArea);
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
			field = getPropertyField(typeHandler,column)+"_sort";
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
	TopDocs       hits     = searcher.search(query, max+skip,sort);
        ScoreDoc[]    docs     = hits.scoreDocs;
	if(debugSearch)
	    debug("SearchManager: lucene query:" + query +" skip:" + skip +" max:" + max);
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
		//This usually happens because of access control
		//		getLogManager().logSpecial("SearchManager.processLuceneSearch - unable to find entry from id:" + id);
                continue;
            }
	    if(hasArea[0] && !entry.isGeoreferenced(request)) {
		continue;
	    }
	    entries.add(entry);
	}
	if(debugSearch)
	    debug("SearchManager: lucene query results:" + entries.size());

    }

    private boolean makeAreaQueries(List<SelectionRectangle> rectangles,
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
	    return true;
	}
	return false;

    }

    public void entriesCreated(Request request, List<Entry> entries) {
        try {
            indexEntries(entries, request, true);
        } catch (Throwable exc) {
            logError("Error indexing entries", exc);
        }
    }

    public void entriesModified(Request request, List<Entry> entries) {
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

    private synchronized void  commit(IndexWriter indexWriter) throws Exception {
	luceneSearcher = null;
	indexWriter.commit();
    }

    public void addStats(StringBuilder sb) throws Exception {
	IndexSearcher searcher = getLuceneSearcher();
	CollectionStatistics stats = searcher.collectionStatistics(FIELD_NAME);
	if(stats==null) {
	    HU.formEntry(sb,"","No Lucene index statistics available");
	    return;
	}

	HU.formEntry(sb,"","Lucene Statistics:");

	HU.formEntry(sb,"#of documents:",""+ stats.docCount());
    }

    public void entriesDeleted(List<String> ids) {
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

    public Result processCapabilities(Request request) throws Exception {
        return new Result("", "text/xml");
    }

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

    public List<RequestUrl> getSearchUrls() throws Exception {
        if (getRegistryManager().getEnabledRemoteServers().size() > 0) {
            //            return getRepository().remoteSearchUrls;
            return remoteSearchUrls;
        }

        return searchUrls;
    }

    public String getSearchUrl(Request request) {
	//        return request.makeUrl(URL_ENTRY_SEARCH, ARG_NAME, WHAT_ENTRIES);
        return request.makeUrl(URL_ENTRY_SEARCH);
    }

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

    private String getSearchButtons(Request request) throws Exception {
        return HU.submit("Search", ARG_SEARCH_SUBMIT);
    }

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

    private void makeSearchForm(Request request, Appendable sb,
                                boolean typeSpecific, boolean addTextField)
	throws Exception {

        sb.append(HU.open(HU.TAG_DIV,
                                 HU.cssClass("ramadda-search-form")));
	Function<String,String> inset = c->{
	    return HU.insetDiv(c, 5, 10, 10, 0);
	};

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

    public Result processSearchType(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        List<String> toks = Utils.split(request.getRequestPath(), "/", true,
                                        true);
        String lastTok = toks.get(toks.size() - 1);
        if (lastTok.equals("type")) {
            sb.append(HU.sectionOpen(null, false));
            HU.open(sb, "div", HU.cssClass("ramadda-links"));
            addSearchByTypeList(request, sb,new Hashtable(),true,true,"",null,null,null);
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

    public void addSearchByTypeList(Request request, Appendable sb,
				    Hashtable props,
				    boolean showHeader,
				    boolean showSearchField,
				    String listStyle,
				    HashSet<String> supers,
				    HashSet<String> cats, HashSet<String> types)
	throws Exception {

	String uid =  HU.getUniqueId("types");
	if(showSearchField) {
	    sb.append("<center>");
	    String args = JU.map("focus",Utils.getProperty(props,"focus","true"));
	    HU.script(sb,HU.call("HtmlUtils.initPageSearch",
				 "'.type-list-item'",
				 "'#" + uid +" .type-list-container'",
				 "'Find Type'",
				 "false",args));
	    sb.append("</center>");
	}
	sb.append(HU.open("div","id",uid));
	sb.append("\n");
        for (EntryManager.SuperType superType :
		 getEntryManager().getCats()) {
	    if(supers!=null && !supers.contains(superType.getName())) continue;
            boolean didSuper = false;
            for (EntryManager.Types typeList : superType.getList()) {
		if(cats!=null && !cats.contains(typeList.getName())) continue;
                boolean didSub = false;
                for (TypeHandler typeHandler : typeList.getList()) {
		    if(types!=null && !types.contains(typeHandler.getType())) continue;
                    int cnt = getEntryUtil().getEntryCount(request,typeHandler);
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

    public Result processEntryBrowseSearchForm(Request request)
	throws Exception {

        StringBuffer sb = new StringBuffer();
        HU.open(sb, "div", HU.cssClass("ramadda-links"));
        getMetadataManager().addToBrowseSearchForm(request, sb);
        HU.close(sb, "div");

        return makeResult(request, "Search Form", sb);
    }

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

    public void addPluginSearchProvider(SearchProvider provider) {
        pluginSearchProviders.add(provider);
	if(searchProviderMap==null) searchProviderMap =     new Hashtable<String, SearchProvider>();    
        searchProviderMap.put(provider.getType(), provider);
        searchProviderMap.put(provider.getId(), provider);	
    }

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

	//A hack for the popup entry select where we want to only select groups
	if(request.getString(ARG_TYPE,"").equals("isgroup")) {
	    request.remove(ARG_TYPE);
	}

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
	    if(searchProviders.size()==0) {
		getLogManager().logSpecial("No search providers selected");
	    }
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

    public Result processEntrySearch(Request request) throws Exception {
	//if it isn't a remote search then check for humans
	if(!Misc.equals(request.getString(ARG_OUTPUT,""), XmlOutputHandler.OUTPUT_XML)) {
	    //	    getLogManager().logInfoAndPrint("search check:" +request.getString(ARG_OUTPUT,"") +" xml:" + XmlOutputHandler.OUTPUT_XML);
	    Result humanResult = getRepository().checkForHuman(request);
	    if(humanResult!=null) {
		return humanResult;
	    }
	}


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
			Hashtable<String,String> idMap  = new Hashtable<String,String>();			
			Hashtable<String,File> filesMap =  new Hashtable<String,File>();
			for (int i = 0; i < children.getLength(); i++) {
			    Element node = (Element) children.item(i);
			    //                    if (!node.getTagName().equals(TAG_ENTRY)) {continue;}
			    List<Entry> entryList =
				getEntryManager().createEntryFromXml(request, node, parentEntry,
								     filesMap,
								     entryMap,
								     idMap,
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
			getSessionManager().addSessionMessage(request,"Error doing search at:" + provider+"<br>Error:" + exc.getMessage());

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

    public static class MetadataTypeSearchInfo {
	SearchManager manager;
	MetadataType type;
	List<MetadataElementSearchInfo> elements= new ArrayList<MetadataElementSearchInfo>();
	Hashtable<MetadataElement,MetadataElementSearchInfo> mmap =new Hashtable<MetadataElement,MetadataElementSearchInfo>();

	public MetadataTypeSearchInfo(SearchManager manager, MetadataType type) {
	    this.manager=manager;
	    this.type = type;
	}

	public MetadataElementSearchInfo get(MetadataElement element) {
	    MetadataElementSearchInfo info = mmap.get(element);
	    if(info==null) {
		mmap.put(element,info  = new MetadataElementSearchInfo(manager,element));
		elements.add(info);
	    }
	    return info;
	}

	@Override
	public int hashCode() {
	    return type.hashCode();
	}

	@Override
	public boolean equals(Object o) {
	    return type.equals(o);
	}

    }

    public static class MetadataElementSearchInfo {
	SearchManager manager;
	MetadataElement element;
	List<String> values= new ArrayList<String>();
	public MetadataElementSearchInfo(SearchManager manager,MetadataElement element) {
	    this.manager=manager;
	    this.element=element;
	}
	public Query makeQuery() {
	    BooleanQuery.Builder builder = new BooleanQuery.Builder();
	    String field = manager.getMetadataField(getFieldId());
	    for(String value: values) {
		Query query;
		boolean not = value.startsWith("!");
		if(not) value = value.substring(1);
		if(isEnumeration()) {
		    query = new TermQuery(new Term(field, value));
		} else {
		    query =manager.makeTextQuery(field,value);
		}
		if(not) {
		    MatchAllDocsQuery matchAllDocsQuery = new MatchAllDocsQuery();
		    builder.add(matchAllDocsQuery, BooleanClause.Occur.SHOULD);
		    builder.add(query,BooleanClause.Occur.MUST_NOT);		
		} else {
		    builder.add(query,BooleanClause.Occur.SHOULD);
		}
	    }
	    return builder.build();
	}

	public void addValue(String v) {
	    values.add(v);
	}

	public boolean isEnumeration() {
	    return element.isEnumeration();
	}

	public String getFieldId() {
	    return element.getParent().getId()+"_"+element.getIndex();

	}

	@Override
	public int hashCode() {
	    return element.hashCode();
	}

	@Override
	public boolean equals(Object o) {
	    return element.equals(o);
	}

    }

    public static String getCorpus(String file) throws Exception {
	try(InputStream stream = new FileInputStream(file)) {
	    TikaConfig config = getTikaConfigTest();
	    Parser parser = new AutoDetectParser(config);
	    ParseContext parseContext = new ParseContext();
            parseContext.set(TikaConfig.class, config);
	    PDFParser pdfParser = new PDFParser();
    //	    System.err.println(PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION );
	    pdfParser.setOcrStrategy("OCR_AND_TEXT_EXTRACTION");
	    BufferedInputStream bis = new BufferedInputStream(stream);
	    org.apache.tika.metadata.Metadata metadata =
		new org.apache.tika.metadata.Metadata();
	    BodyContentHandler handler =  new BodyContentHandler(1_000_000);
	    pdfParser.parse(bis, handler, metadata,parseContext);
	    //	    parser.parse(bis, handler, metadata,new org.apache.tika.parser.ParseContext());
	    String corpus = handler.toString();
	    if(corpus!=null) corpus=corpus.trim();
	    return corpus;
	}
    }

    public static void main(String[]args) throws Exception {
	for(String file: args) {
	    String corpus = getCorpus(file);
	    System.err.println("File:" + file);
	    System.err.println("Corpus: " + corpus);
	}

    }

}

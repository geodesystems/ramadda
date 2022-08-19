/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.aws;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;
import org.ramadda.util.PatternHolder;
import org.ramadda.util.Propper;

import org.ramadda.util.IO;
import org.ramadda.util.S3File;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import ucar.unidata.util.IOUtil;
import org.w3c.dom.Element;

import java.io.*;
import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 */
@SuppressWarnings("unchecked")
public class S3RootTypeHandler extends ExtensibleGroupTypeHandler {

    /**  */
    private SimpleDateFormat yearFormat =
        RepositoryUtil.makeDateFormat("yyyy");

    /**  */
    private SimpleDateFormat yearMonthFormat =
        RepositoryUtil.makeDateFormat("yyyy-MM");

    /**  */
    private SimpleDateFormat yearMonthDayFormat =
        RepositoryUtil.makeDateFormat("yyyy-MM-dd");

    private static Object DATE_MUTEX  = new Object();

    private static TTLCache<String,Propper> propertiesCache = new TTLCache<String,Propper>(1000*60*60);


    /**  */
    private TTLCache<String, List<String>> synthIdCache =
        new TTLCache<String, List<String>>(5 * 60 * 1000);


    /**  */
    private static int IDX = 0;

    /** _more_ */
    public static final int IDX_ROOT = IDX++;

    /**  */
    public static final int IDX_EXCLUDE_PATTERNS = IDX++;

    /**  */
    public static final int IDX_MAX = IDX++;

    /**  */
    public static final int IDX_PERCENT = IDX++;

    /**  */
    public static final int IDX_SIZE_LIMIT = IDX++;


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public S3RootTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param select _more_
     * @param rootEntry _more_
     * @param parentEntry _more_
     * @param synthId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<String> getSynthIds(Request request, SelectInfo select,
                                    Entry rootEntry, Entry parentEntry,
                                    String synthId)
            throws Exception {

        boolean debug = false;
        String cacheKey = parentEntry.getId() + "_" + synthId + "_"
                          + rootEntry.getChangeDate() + "_"
                          + request.getString(ARG_MARKER, "");
        List<String> ids = synthIdCache.get(cacheKey);
        if (ids != null) {
            return ids;
        }

        if (debug) {
            System.err.println("S3RootTypeHandler.getSynthIds: " + " parent:"
                               + parentEntry.getName() + " root: "
                               + rootEntry.getName() + " synthId:" + synthId);
        }


        ids = new ArrayList<String>();

        //Always have to have a root
        String rootId = getRootId(rootEntry);
        if ( !Utils.stringDefined(rootId)) {
            return ids;
        }
        List<PatternHolder> excludes = null;
        String exclude = (String) rootEntry.getValue(IDX_EXCLUDE_PATTERNS);
        int                 max      = rootEntry.getIntValue(IDX_MAX, 1000);
        double percent = rootEntry.getDoubleValue(IDX_PERCENT,
                             (double) -100.0);
        double tmp = rootEntry.getDoubleValue(IDX_SIZE_LIMIT, (double) -1.0);
        if (Double.isNaN(tmp)) {
            tmp = -1;
        }
        long     maxSize = (long) ((tmp == -1)
                                   ? -1
                                   : tmp * 1000 * 1000);
        Object[] values  = rootEntry.getValues();
        if (Utils.stringDefined(exclude)) {
            excludes = PatternHolder.parseLines(exclude);
        }

        if ( !Utils.stringDefined(synthId)) {
            synthId = rootId;
        } else {
            //Check that the synthid is a child of the root
            if ( !synthId.startsWith(rootId)) {
                System.err.println("Error: S3 id:" + synthId
                                   + " is not a child of the root id:"
                                   + rootId);
            }
        }

        //      S3File.debug = true;
        S3File.S3ListResults results = doLs(request, new S3File(synthId),
                                            null, max, percent, maxSize);
        if (results.getMarker() != null) {
            String prevMarker  = request.getString(ARG_MARKER, null);
            String prevMarkers = request.getString(ARG_PREVMARKERS, null);
            if (Utils.stringDefined(prevMarker)
                    || Utils.stringDefined(prevMarkers)) {
                List<String> markers = new ArrayList<String>();
                if (prevMarkers != null) {
                    markers.addAll(Utils.split(prevMarkers, ",", true, true));
                }
                if (Utils.stringDefined(prevMarker)) {
                    markers.add(prevMarker);
                }
                request.putExtraProperty(ARG_PREVMARKERS,
                                         Utils.join(markers, ","));
            }
            request.putExtraProperty(ARG_MARKER, results.getMarker());
        }
        List<S3File> files    = results.getFiles();
        List<String> children = new ArrayList<String>();
        int          cnt      = 0;
        for (S3File file : files) {
            boolean ok = true;
            if ((excludes != null) && (excludes.size() > 0)) {
                if (PatternHolder.checkPatterns(excludes, file.toString())) {
                    continue;
                }
            }
            Entry bucketEntry = createBucketEntry(request, rootEntry, parentEntry,
                                    file);
            if ( !ok) {
                if (debug) {
                    System.err.println("Skipping:" + file);
                }
                continue;
            }
            if (bucketEntry == null) {
                continue;
            }
            ids.add(bucketEntry.getId());
        }
        parentEntry.setChildIds(ids);
        if (debug) {
            System.err.println("CACHING:" + cacheKey);
        }
        synthIdCache.put(cacheKey, ids);

        return ids;
    }

    private Propper getConvertProperties(Request request, Entry entry, String type) throws Exception {
        String cacheKey = entry.getId() + "_" + type+"_"+entry.getChangeDate();
	Propper props = propertiesCache.get(cacheKey);
	if(props!=null) {
	    return props;
	}

	List<Metadata> metadataList =
	    getMetadataManager().findMetadata(request, entry,
					      new String[] {
						  "convert_file"}, true);

	if(metadataList!=null) {
	    for(Metadata metadata: metadataList) {
		if(metadata.getAttr1().equals(type)) {
		    File f = getMetadataManager().getFile(request, entry, metadata,3);
		    boolean exact = metadata.getAttr2().equals("exact");
		    if(f.exists()) {
			if(f.getName().endsWith(".properties")) {
			    Properties properties = new Properties();
			    properties.load(getStorageManager().getInputStream(f.toString()));
			    props = new Propper(exact,properties);
			} else {
			    props = new Propper(exact);
			    for(String line: Utils.split(IOUtil.readContents(getStorageManager().getInputStream(f.toString())),
							 "\n",true,true)) {
				List<String> cols = Utils.tokenizeColumns(line,",");
				String key = cols.get(0);
				cols.remove(0);
				props.add(key,cols);
			    }				
			}
		    }
		}
	    }
	}
	if(props == null) props = new Propper();
	propertiesCache.put(cacheKey,props);
	return props;
    }


    /**
     * _more_
     *
     * @param rootEntry _more_
     * @param parentEntry _more_
     * @param file _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createBucketEntry(Request request,Entry rootEntry, Entry parentEntry,
                                    S3File file)
            throws Exception {
        String dateFlag = null;
        Date   dataDate = null;
        String name     = file.getName();
	String originalName = name;
        int    year     = getYear(name);
        if (year > 0) {
	    synchronized(DATE_MUTEX) {
		dataDate = yearFormat.parse("" + year);
	    }
            dateFlag = "year";
        }

        String parent = parentEntry.getName();
        //Assume month names
        year = getYear(parent);
        if (year > 0) {
            if (name.matches("(0|1)[0-9]")) {
                int month = Integer.parseInt(name);
                if ((month >= 1) && (month <= 12)) {
                    name     = Utils.getMonthName(month - 1);
		    synchronized(DATE_MUTEX) {
			dataDate = yearMonthFormat.parse("" + year + "-" + month);
		    }
                    dateFlag = "yearmonth";
                }
            }
        }

        //If no dataDate then check if the parent's date was set as  yearmonth
        if (dataDate == null) {
            String parentDateFlag =
                (String) parentEntry.getTransientProperty("dateFlag");
            if (parentDateFlag != null) {
                if (parentDateFlag.equals("yearmonth")) {
                    //Does the name match a possible day
                    if (name.matches("(0|1|2|3)[0-9]")) {
                        int day = Integer.parseInt(name);
                        if ((day >= 1) && (day <= 31)) {
			    synchronized(DATE_MUTEX) {
				String yyyymm = yearMonthFormat.format(
								       new Date(parentEntry.getStartDate()));
				dataDate = yearMonthDayFormat.parse(yyyymm + "-"
								    + name);
			    }
                        }
                    }
                }
            }
        }

	Propper props = getConvertProperties(request,  rootEntry, "alias");
	String alias = (String)props.get(name);
	if(alias!=null) name = alias;


        Date createDate = new Date(file.lastModified());
        if (dataDate == null) {
            dataDate = createDate;
        }
        String id = getEntryManager().createSynthId(rootEntry,
                        file.toString());
	Entry cached =   getEntryManager().getEntryFromCache(id);
	if(cached!=null) {
	    return cached;
	}

        boolean isBucketType = false;
        TypeHandler bucketTypeHandler =
            getEntryManager().findDefaultTypeHandler(rootEntry,
                file.toString(), !file.isDirectory());
        //      System.err.println("rootEntry:" + rootEntry +" file:" + file +" "  +
        //                         file.isDirectory() +" type:" + bucketTypeHandler); 
        if (bucketTypeHandler == null) {
            isBucketType      = true;
            bucketTypeHandler = file.isDirectory()
                                ? getRepository().getTypeHandler(
                                    "type_s3_bucket")
                                : getRepository().getTypeHandler(
                                    "type_s3_file");
        }
        Entry  bucketEntry = new Entry(id, bucketTypeHandler);
        String desc        = "";
        if ( !isBucketType) {
            desc = HU.b("AWS/S3 Bucket:") + " " + file.toString();
        } else {
	    Propper templateProps = getConvertProperties(request,  rootEntry, "template");
	    String template = (String) templateProps.get(/*originalName,*/ name,file.isDirectory()?"directory":"file");
	    if(template!=null) {
		//		System.err.println(name + " " +template);
		template = template.replace("\\n","\n");
		desc= "<wiki>\n"+template;
	    }
	}
        Resource resource = file.isDirectory()?new Resource():new Resource(file.toString(), Resource.TYPE_S3,
                                         file.length());
        Object[] values = bucketTypeHandler.makeEntryValues(null);
        bucketEntry.initEntry(name, desc, parentEntry, parentEntry.getUser(),
                              resource, "", Entry.DEFAULT_ORDER,
                              createDate.getTime(), createDate.getTime(),
                              dataDate.getTime(), dataDate.getTime(), values);

	Propper locProps = getConvertProperties(request,  rootEntry, "location");
	List<String> cols = (List<String>)locProps.get(originalName, name);
	if(cols!=null && cols.size()>=2) {
	    bucketEntry.setLocation(Double.parseDouble(cols.get(0)),Double.parseDouble(cols.get(1)));
	}


        if (dateFlag != null) {
            bucketEntry.putTransientProperty("dateFlag", dateFlag);
        }
        if (isBucketType) {
            bucketEntry.setValue("bucket_id", file.toString());
        }
        bucketEntry.setMasterTypeHandler(this);
        getEntryManager().cacheSynthEntry(bucketEntry);
        return bucketEntry;
    }

    /**
     *
     * @param entry _more_
     *  @return _more_
     */
    private String getRootId(Entry entry) {
        String id = (String) entry.getValue(IDX_ROOT);
        if (id != null) {
            id = id.trim();
        }

        return id;
    }


    /**
     *
     * @param s _more_
     *  @return _more_
     */
    private int getYear(String s) {
        if (s.matches("^(1|2)[0-9]+$")) {
            int year = Integer.parseInt(s);
            //Assume this is a valid year range
            if ((year >= 1900) && (year < 2030)) {
                return year;
            }
        }

        return -1;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param rootEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry rootEntry, String id)
            throws Exception {
	long t1 = System.currentTimeMillis();
        String rootId = getRootId(rootEntry);
        if ( !Utils.stringDefined(rootId)) {
            return null;
        }
        id = S3File.normalizePath(id);
        //roll up the path creating the parent entries up to the root
        Entry         parent = rootEntry;
        String        key    = id.replace(rootId, "");
        List<String>  keys   = Utils.split(key, "/", true, true);
        StringBuilder path   = new StringBuilder(rootId);
        //      System.err.println("id:" + id + ":\nrootId:" + rootId+":\nkey:"+ key);
        //      System.err.println("keys:" + keys);
        for (int i = 0; i < keys.size(); i++) {
            String ancestorKey = keys.get(i);
            path.append(ancestorKey);
            S3File s3File = null;
	    if (i < keys.size() - 1) {
                path.append("/");
		String s3Path = path.toString();
		String synthId = getEntryManager().createSynthId(rootEntry,
							    s3Path);
		Entry cached =   getEntryManager().getEntryFromCache(synthId);
		if(cached!=null) {
		    parent = cached;
		    continue;
		}
                s3File = new S3File(path.toString());
            } else {
                //If it is the last one then it might be a file so call createFile which does a listing
		String s3Path = path.toString();
		//Check the cache before we hit S3
		String synthId = getEntryManager().createSynthId(rootEntry,
							    s3Path);
		Entry cached =   getEntryManager().getEntryFromCache(synthId);
		if(cached!=null) {
		    parent = cached;
		    continue;
		}
                s3File = S3File.createFile(path.toString());
            }
            if (s3File == null) {
                System.err.println(
                    "S3RootTypeHandler: Unable to create s3file from:" + path
                    + "\nID:" + id);
            }
            parent = createBucketEntry(request, rootEntry, parent, s3File);
        }
	long t2 = System.currentTimeMillis();
	//	Utils.printTimes("s3",t1,t2);
        //System.err.println("S3 creating:" + id + " key:" + key + " keys:" + keys);
        return parent;
    }

    /**
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param entryNames _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public Entry makeSynthEntry(Request request, Entry parentEntry,
                                List<String> entryNames)
            throws Exception {
        return makeSynthEntry(request, parentEntry,
                              Utils.join(entryNames, "/"));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public boolean isSynthType() {
        return true;
    }

    /**
     * _more_
     *
     * @param base _more_
     * @param path _more_
     *
     * @return _more_
     */
    public static String getS3Path(S3File base, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(base.toString());
        if (Utils.stringDefined(path)) {
            sb.append(path);
        }

        return sb.toString();
    }

    /**
     * _more_
     *
     * @param bucket _more_
     *
     * @param request _more_
     * @param path _more_
     * @param max _more_
     * @param percent _more_
     * @param maxSize _more_
     *
     * @param base _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public S3File.S3ListResults doLs(Request request, S3File base,
                                     String path, int max, double percent,
                                     long maxSize)
            throws Exception {
        S3File newFile = new S3File(getS3Path(base, path));

        return newFile.doList(false, max, percent, maxSize,
                              request.getString(ARG_MARKER, null));
    }

    /**  */
    public static final String ACTION_SEARCH = "s3search";

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, Entry entry,
                              OutputHandler.State state, List<Link> links)
            throws Exception {
        super.getEntryLinks(request, entry, state, links);
        links.add(new Link(getEntryActionUrl(request, entry, ACTION_SEARCH),
                           ICON_SEARCH, "Search S3 Objects",
                           OutputType.TYPE_FILE));
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryAction(Request request, Entry entry)
            throws Exception {
        String action = request.getString(ARG_ACTION, "");
        if ( !action.equals(ACTION_SEARCH)) {
            return super.processEntryAction(request, entry);
        }
        StringBuilder sb = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "S3 Search");

        getS3SearchForm(request, entry, sb);

        String rootId = getRootId(entry);
        S3File file   = new S3File(rootId);
        String text   = request.getString("text", "");
        if (stringDefined(text)) {
            List<String> found = file.doSearch(text);
            if ((found == null) || (found.size() == 0)) {
                sb.append(
                    getPageHandler().showDialogWarning(
                        "Could not find object:" + text));

                return new Result("S3 List", sb);
            }
            sb.append(HU.b("Searching for: ") + text);
            sb.append("<ul>");
            for (String f : found) {
                f = f.trim();
                if ( !rootId.endsWith("/")) {
                    if ( !f.startsWith("/")) {
                        f = "/" + f;
                    }
                }
                String id = getEntryManager().createSynthId(entry,
                                rootId + f);
                sb.append("<li> ");
                String url =
                    HU.url(getRepository().URL_ENTRY_SHOW.toString(),
                           ARG_ENTRYID, id);
                sb.append(HU.href(url, f));
            }
            sb.append("</ul>");
        }


        getPageHandler().entrySectionClose(request, entry, sb);

        return new Result("S3 List", sb);
    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void getS3SearchForm(Request request, Entry entry,
                                 StringBuilder sb)
            throws Exception {
        sb.append(HU.form(getEntryActionUrl(request, entry,
                                            ACTION_SEARCH).toString()));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.hidden(ARG_ACTION, ACTION_SEARCH));
        sb.append(HU.input("text", request.getString("text", ""),
                           HU.SIZE_30
                           + HU.attrs("placeholder", "Search text")));
        sb.append(HU.SPACE2);
        sb.append(HU.submit("Search"));
        sb.append(HU.formClose());
    }


    /**
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param originalEntry _more_
     * @param entry _more_
     * @param tag _more_
     * @param props _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry originalEntry, Entry entry,
                                 String tag, Hashtable props)
            throws Exception {

        if ( !tag.equals("s3search")) {
            return super.getWikiInclude(wikiUtil, request, originalEntry,
                                        entry, tag, props);
        }
        StringBuilder sb = new StringBuilder();
        getS3SearchForm(request, entry, sb);

        return sb.toString();
    }

    /**
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        String id =
            "s3:/noaa-nexrad-level2/2021/02/02/KABR/KABR20210202_001341_V06";
        //      if(id.matches("^s3:/[^/]+")) {
        if (id.matches("^s3:/[^/]+.*")) {
            id = id.replaceAll("^s3:/", "s3://");
            System.err.println("MANGLED: " + id);
        } else {
            System.err.println("NOT MANGLED: " + id);
        }
    }



}

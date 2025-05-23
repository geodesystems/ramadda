/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.aws;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.output.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.IO;
import org.ramadda.util.PatternHolder;
import org.ramadda.util.Propper;
import org.ramadda.util.S3File;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;
import org.ramadda.util.WikiUtil;

import org.w3c.dom.Element;

import ucar.unidata.util.IOUtil;

import java.io.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 */
@SuppressWarnings("unchecked")
public class S3RootTypeHandler extends ExtensibleGroupTypeHandler {

    /**  */
    private static final String ARG_SEARCH_TEXT = "searchtext";

    /**  */
    private static final String ARG_SEARCH_ROOT = "searchroot";

    /**  */
    private static final String[] NAME_ALIASES = { "name", "fullname",
            "stationname" };

    /**  */
    private static final String[] LAT_ALIASES = { "lat", "latitude" };

    /**  */
    private static final String[] LON_ALIASES = { "lon", "long",
                                                  "longitude" };


    /**  */
    private SimpleDateFormat yearFormat =
        RepositoryUtil.makeDateFormat("yyyy");

    /**  */
    private SimpleDateFormat yearMonthFormat =
        RepositoryUtil.makeDateFormat("yyyy-MM");

    /**  */
    private SimpleDateFormat yearMonthDayFormat =
        RepositoryUtil.makeDateFormat("yyyy-MM-dd");

    /**  */
    private static Object DATE_MUTEX = new Object();

    /**  */
    private static TTLCache<String, List<Propper>> propertiesCache =
        new TTLCache<String, List<Propper>>(1000 * 60 * 60);


    /**  */
    private TTLCache<String, List<String>> synthIdCache =
        new TTLCache<String, List<String>>(5 * 60 * 1000);



    /**  */
    private static int IDX = 0;

    /** _more_ */
    public static final int IDX_ROOT = IDX++;

    /**  */
    public static final int IDX_AWS_KEY = IDX++;
    public static final int IDX_AWS_ENDPOINT = IDX++;
    public static final int IDX_AWS_ENDPOINT_REGION = IDX++;        


    /**  */
    public static final int IDX_DO_CACHE = IDX++;

    /**  */
    public static final int IDX_EXCLUDE_PATTERNS = IDX++;

    /**  */
    public static final int IDX_MAX = IDX++;

    /**  */
    public static final int IDX_PERCENT = IDX++;

    /**  */
    public static final int IDX_SIZE_LIMIT = IDX++;

    /**  */
    public static final int IDX_CONVERT_DATES = IDX++;

    /**  */
    public static final int IDX_DATE_PATTERNS = IDX++;


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
            //Check if the children exist. If not then continue
            if (ids.size() > 0) {
                for (String id : ids) {
                    if (getEntryManager().getEntryFromCache(id) == null) {
                        ids = null;

                        break;
                    }
                }
            }
            //            ids = null;
            if (ids != null) {
		/**
                System.err.println("getSynthIds: cached:" + synthId
                                   + " cachekey:" + cacheKey + " marker:"
                                   + request.getString(ARG_MARKER, ""));
		*/

                return ids;
            }
        }

        if (debug) {
            System.err.println("S3RootTypeHandler.getSynthIds: " + " parent:"
                               + parentEntry.getName() + " root: "
                               + rootEntry.getName() + " synthId:" + synthId);
        }


        ids = new ArrayList<String>();

        //Always have to have a root
        String rootId = getRootId(request,rootEntry);
        if ( !Utils.stringDefined(rootId)) {
            return ids;
        }
        List<PatternHolder> excludes = null;
        String exclude = (String) rootEntry.getValue(request,IDX_EXCLUDE_PATTERNS);
        int                 max      = rootEntry.getIntValue(request,IDX_MAX, 1000);
        double percent = rootEntry.getDoubleValue(request,IDX_PERCENT,
                             (double) -100.0);
        double tmp = rootEntry.getDoubleValue(request,IDX_SIZE_LIMIT, (double) -1.0);
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
        long t1 = System.currentTimeMillis();
        S3File.S3Results results = doLs(request, rootEntry,
					createS3File(request,rootEntry, synthId), null,
                                      max, percent, maxSize);
        long t2 = System.currentTimeMillis();
	//        Utils.printTimes("ls:" + synthId, t1, t2);

        String currentMarker = request.getString(ARG_MARKER, null);
        String prevMarkers   = request.getString(ARG_PREVMARKERS, null);
        String nextMarker    = results.getMarker();
        if (Utils.stringDefined(currentMarker)
                || Utils.stringDefined(prevMarkers)) {
            List<String> markers = new ArrayList<String>();
            if (Utils.stringDefined(prevMarkers)) {
                markers.addAll(Utils.split(prevMarkers, ",", true, true));
            }
            if (Utils.stringDefined(currentMarker)) {
                markers.add(currentMarker);
            }
            request.putExtraProperty(ARG_PREVMARKERS,
                                     Utils.join(markers, ","));
            System.err.println("S3 prevMarkers:"
                               + Utils.join(Utils.Y(markers), ","));
        }
        if (nextMarker != null) {
            request.remove(ARG_MARKER);
            request.putExtraProperty(ARG_MARKER, nextMarker);
            System.err.println("S3 nextMarker:" + Utils.X(nextMarker));
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
            Entry bucketEntry = createBucketEntry(request, rootEntry,
                                    parentEntry, file);
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

    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    private List<Propper> getConvertProperties(Request request, Entry entry,
            String type)
            throws Exception {
        String cacheKey = entry.getId() + "_" + type + "_"
                          + new Date(entry.getChangeDate());
        List<Propper> props = propertiesCache.get(cacheKey);
        if (props != null) {
            return props;
        }
        List<Metadata> metadataList =
            getMetadataManager().findMetadata(request, entry,
                new String[] { "convert_file" }, true);
        props = new ArrayList<Propper>();
        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                if (metadata.getAttr1().equals(type)) {
                    File f = getMetadataManager().getFile(request, entry,
                                 metadata, 3);
                    if (f.exists()) {
                        boolean exact = metadata.getAttr2().equals("exact");
                        props.add(
                            Propper.create(
                                exact, f.getName(),
                                getStorageManager().getInputStream(
                                    f.toString())));
                    }
                }
            }
        }
        propertiesCache.put(cacheKey, props);

        return props;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param rootEntry _more_
     * @param parentEntry _more_
     * @param file _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createBucketEntry(Request request, Entry rootEntry,
                                    Entry parentEntry, S3File file)
            throws Exception {

        String id = getEntryManager().createSynthId(rootEntry,
                        file.toString());

        //      System.err.println("CREATE:" + file.getName());
        String dateFlag        = null;
        Date   dataDate        = null;
        String name            = file.getName();
        String parentName      = parentEntry.getName();
        String grandparentName = (parentEntry.getParentEntry() != null)
                                 ? parentEntry.getParentEntry().getName()
                                 : "";
        String originalParentName =
            (String) parentEntry.getTransientProperty("originalname");
        if (originalParentName == null) {
            originalParentName = parentName;
        }
        String originalName = name;

        if (rootEntry.getBooleanValue(request,IDX_CONVERT_DATES, false)) {
            int year = getYear(name);
            if (year > 0) {
                synchronized (DATE_MUTEX) {
                    dataDate = yearFormat.parse("" + year);
                }
                dateFlag = "year";
            }
            //Assume month names
            year = getYear(parentName);
            if (year > 0) {
                if (name.matches("(0|1)[0-9]")) {
                    int month = Integer.parseInt(name);
                    if ((month >= 1) && (month <= 12)) {
                        name = Utils.getMonthName(month - 1);
                        synchronized (DATE_MUTEX) {
                            dataDate = yearMonthFormat.parse("" + year + "-"
                                    + month);
                        }
                        dateFlag = "yearmonth";
                    }
                }
            }

            //If no dataDate then check if the parentName's date was set as  yearmonth
            if (dataDate == null) {
                String parentDateFlag =
                    (String) parentEntry.getTransientProperty("dateFlag");
                if (parentDateFlag != null) {
                    if (parentDateFlag.equals("yearmonth")) {
                        //Does the name match a possible day
                        if (name.matches("(0|1|2|3)[0-9]")) {
                            int day = Integer.parseInt(name);
                            if ((day >= 1) && (day <= 31)) {
                                synchronized (DATE_MUTEX) {
                                    String yyyymm =
                                        yearMonthFormat.format(
                                            new Date(
                                                parentEntry.getStartDate()));
                                    dataDate =
                                        yearMonthDayFormat.parse(yyyymm + "-"
                                            + name);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Propper aliases :
                getConvertProperties(request, rootEntry, "alias")) {
            Object oalias = aliases.get(new String[] {
                                IOUtil.stripExtension(name),
                                originalParentName + "/"
                                + IOUtil.stripExtension(originalName),
                                parentName + "/"
                                + IOUtil.stripExtension(originalName) });
            if (oalias != null) {
                String alias = aliases.getValue(NAME_ALIASES, oalias);
                if (alias != null) {
                    alias = alias.replace("${name}", name).replace(
                        "${parentname}", parentName).replace(
                        "${grandparentname}", grandparentName);
                    name = alias;

                    break;
                }
            }
        }

        if (dataDate == null) {
            for (String line :
                    Utils.split(
                        (String) rootEntry.getValue(request,IDX_DATE_PATTERNS), "\n",
                        true, true)) {
                List<String> toks = Utils.split(line, ";");
                if (toks.size() != 3) {
                    continue;
                }
                Pattern pattern = Pattern.compile(toks.get(0));
                Matcher matcher = pattern.matcher(name);
                //              System.err.println(toks.get(0)+":"+name+":"+ matcher.find());
                if (matcher.find()) {
                    String dttm = toks.get(1);
                    boolean debug = name.indexOf("KBGM20140505_000222_V06")
                                    >= 0;
                    debug = false;
                    if (debug) {
                        System.err.println("name:" + name);
                    }
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (debug) {
                            System.err.println("\t#" + i + "="
                                    + matcher.group(i));
                        }
                        dttm = dttm.replace("${" + (i) + "}",
                                            matcher.group(i));
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat(toks.get(2));
                    dataDate = sdf.parse(dttm);
                    if (debug) {
                        System.err.println("DTTM:" + dttm + " Date:"
                                           + dataDate);
                    }
                }
            }
        }


        Date createDate = new Date(file.lastModified());
        if (dataDate == null) {
            dataDate = createDate;
        }

        boolean isBucketType = false;
        TypeHandler bucketTypeHandler =
            getEntryManager().findDefaultTypeHandler(request,rootEntry,
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
            for (Propper templates :
                    getConvertProperties(request, rootEntry, "template")) {
                String template = (String) templates.get(  /*originalName,*/
                    originalParentName + "/" + originalName,
                    parentName + "/" + originalName, name, file.isDirectory()
                        ? "directory"
                        : "file");
                if (template != null) {
                    template = template.replace("\\n", "\n");
                    desc     = "<wiki>\n" + template;

                    break;
                }
            }
        }
        Resource resource = file.isDirectory()
                            ? new Resource()
                            : new Resource(file.toString(), Resource.TYPE_S3,
                                           file.length());
        Object[] values = bucketTypeHandler.makeEntryValues(null);
        name = name.trim();
        bucketEntry.initEntry(name, desc, parentEntry, parentEntry.getUser(),
                              resource, "", Entry.DEFAULT_ORDER,
                              createDate.getTime(), createDate.getTime(),
                              dataDate.getTime(), dataDate.getTime(), values);


        bucketEntry.putTransientProperty("originalname", originalName);
        //Put the AWS key if we have it
        String key = getAwsKey(request,rootEntry);
        if (key != null) {
            bucketEntry.putTransientProperty(PROP_AWS_KEY,
                                             getAwsKey(request,rootEntry));
            bucketEntry.putTransientProperty(PROP_S3_ENDPOINT,
					     getS3Endpoint(request,rootEntry));
            bucketEntry.putTransientProperty(PROP_S3_ENDPOINT_REGION,
					     getS3Region(request,rootEntry));	    
        }
        for (Propper locProps :
                getConvertProperties(request, rootEntry, "location")) {
            List<String> cols = (List<String>) locProps.get(parentName + "/"
                                    + originalName, originalName, name);
            if (cols != null) {
                String lat = locProps.getValue(LAT_ALIASES, cols);
                String lon = locProps.getValue(LON_ALIASES, cols);
                if ((lat != null) && (lon != null)) {
                    bucketEntry.setLocation(Double.parseDouble(lat),
                                            Double.parseDouble(lon));

                    break;
                }
            }
        }

        if (dateFlag != null) {
            bucketEntry.putTransientProperty("dateFlag", dateFlag);
        }
        if (isBucketType) {
            bucketEntry.setValue("bucket_id", file.toString());
        }
        bucketEntry.setMasterTypeHandler(this);
        if ( !rootEntry.getBooleanValue(request,IDX_DO_CACHE, true)) {
            long ttl = new Date().getTime() + 1000 * 90;
            bucketEntry.setCacheActiveLimit(ttl);
        }
        getEntryManager().cacheSynthEntry(bucketEntry);

        return bucketEntry;

    }

    /**
     *
     * @param entry _more_
     *  @return _more_
     */
    private String getRootId(Request request, Entry entry) {
        String id = (String) entry.getValue(request,IDX_ROOT);
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
        return makeSynthEntry(request, rootEntry, id, null);
    }

    /**
     *
     * @param request _more_
     * @param rootEntry _more_
     * @param id _more_
     * @param cache _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry rootEntry, String id,
                                Hashtable<String, S3File> cache)
            throws Exception {

        String rootId = getRootId(request,rootEntry);
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
                Entry cached = getEntryManager().getEntryFromCache(synthId);
                if (cached != null) {
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
                Entry cached = getEntryManager().getEntryFromCache(synthId);
                if (cached != null) {
                    parent = cached;
                    continue;
                }
                long   t1    = System.currentTimeMillis();
                String spath = path.toString();
                if (cache != null) {
                    s3File = cache.get(spath);
                }
                if (s3File == null) {
                    s3File = S3File.createFile(spath, getAwsKey(request,rootEntry),
					       getS3Endpoint(request, rootEntry),
					       getS3Region(request, rootEntry));
                }
                long t2 = System.currentTimeMillis();
                if (s3File == null) {
                    System.err.println("Null s3File:" + path + ":");
                }
                //              Utils.printTimes("createFile:"+ id,t1,t2);
            }
            if (s3File == null) {
                System.err.println(
                    "S3RootTypeHandler: Unable to create s3file from:" + path
                    + "\nID:" + id +" root entry:" + rootEntry.getName() +" " + rootEntry.getId());
                continue;
            }
            parent = createBucketEntry(request, rootEntry, parent, s3File);
        }
        long t2 = System.currentTimeMillis();

        //      Utils.printTimes("s3",t1,t2);
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
     *
     * @param rootEntry _more_
      * @return _more_
     */
    private String getAwsKey(Request request,Entry rootEntry) {
        return rootEntry.getStringValue(request,IDX_AWS_KEY, null);
    }

    private String getS3Endpoint(Request request,Entry rootEntry) {
        return rootEntry.getStringValue(request,IDX_AWS_ENDPOINT, null);
    }

    private String getS3Region(Request request,Entry rootEntry) {
        return rootEntry.getStringValue(request,IDX_AWS_ENDPOINT_REGION, null);
    }        

    /**
     *
     * @param rootEntry _more_
     * @param path _more_
      * @return _more_
     */
    private S3File createS3File(Request request, Entry rootEntry, String path) {
        return new S3File(path, getAwsKey(request,rootEntry),null,
			  getS3Endpoint(request,rootEntry),
			  getS3Region(request,rootEntry));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param rootEntry _more_
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
    public S3File.S3Results doLs(Request request, Entry rootEntry, S3File base,
                               String path, int max, double percent,
                               long maxSize)
            throws Exception {
        S3File newFile = createS3File(request,rootEntry, getS3Path(base, path));

	//        System.err.println("doLs: " + base + " marker=" + Utils.X(request.getString(ARG_MARKER, null)));
        //      System.err.println(Utils.getStack(5));

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
        StringBuilder sb        = new StringBuilder();
        StringBuilder resultsSB = new StringBuilder();
        getPageHandler().entrySectionOpen(request, entry, sb, "S3 Search");

        String rootId = getRootId(request,entry);
        S3File file   = null;
        String text   = request.getString(ARG_SEARCH_TEXT, "");
        if (request.defined(ARG_SEARCH_ROOT)) {
            String root = request.getString(ARG_SEARCH_ROOT);
            if ( !root.startsWith(rootId)) {
                resultsSB.append(
                    getPageHandler().showDialogWarning(
                        "You can only search under the S3 root:" + rootId));
                text = null;
            } else {
                file = createS3File(request,entry, root);
            }
        } else {
            file = createS3File(request,entry, rootId);
        }


        String marker = null;

        if (stringDefined(text)) {
            List<Entry> entries = new ArrayList<Entry>();
            final List<Propper> proppers = getConvertProperties(request,
                                               entry, "alias");
            S3File.Searcher searcher = new S3File.Searcher() {
                public boolean match(
                        String lookFor,
                        com.amazonaws.services.s3.model.S3ObjectSummary o) {
                    String fileName = new File(o.getKey()).getName();
                    String[] keys = new String[] { fileName,
                            IOUtil.stripExtension(fileName) };
                    for (Propper aliases : proppers) {
                        String alias = aliases.getNamedValue(NAME_ALIASES,
                                           keys);
                        if (Utils.matchesOrContains(alias, lookFor)) {
                            return true;
                        }
                    }

                    return false;
                }
            };

            marker = request.getString(ARG_MARKER, null);
            if ( !request.exists("next")) {
                marker = null;
            }
            S3File.S3Results found = file.doSearch(text, searcher, marker);
            marker = found.getMarker();
            //      System.err.println("found:" + marker);
            String message = found.getMessage();
            if ((found == null) || (found.getFiles().size() == 0)) {
                if (message != null) {
                    resultsSB.append(getPageHandler().showDialogNote(message
                            + "<br>" + "No results found"));
                } else {
                    resultsSB.append(
                        getPageHandler().showDialogWarning(
                            "No results for: " + text));
                }
            } else {
                if (message != null) {
                    resultsSB.append(
                        getPageHandler().showDialogNote(message));
                }
                int cnt = 0;
                Hashtable<String, S3File> cache = new Hashtable<String,
                                                      S3File>();
                for (S3File f : found.getFiles()) {
                    cache.put(f.toString(), f);
                }
                for (S3File f : found.getFiles()) {
                    Entry child = makeSynthEntry(request, entry,
                                      f.toString(), cache);
                    entries.add(child);
                }

                if (entries.size() > 0) {
                    request.remove(ARG_MARKER);
                    Hashtable props =
                        Utils.makeHashtable(OutputConstants.ARG_SHOWCRUMBS,
                                            "true");
                    resultsSB.append(getWikiManager().makeTableTree(request,
                            null, props, entries));
                }
            }
        }
        getS3SearchForm(request, entry, rootId, sb, marker);
        sb.append(resultsSB);
        getPageHandler().entrySectionClose(request, entry, sb);

        return getEntryManager().addEntryHeader(request, entry,
                new Result("S3 Search", sb));


    }


    /**
     *
     * @param request _more_
     * @param entry _more_
     * @param rootId _more_
     * @param sb _more_
     * @param marker _more_
     *
     * @throws Exception _more_
     */
    private void getS3SearchForm(Request request, Entry entry, String rootId,
                                 StringBuilder sb, String marker)
            throws Exception {
        sb.append(HU.formTable());
        sb.append(HU.form(getEntryActionUrl(request, entry,
                                            ACTION_SEARCH).toString()));
        sb.append(HU.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(HU.hidden(ARG_ACTION, ACTION_SEARCH));
        HU.formEntry(sb, "Text:",
                     HU.input(ARG_SEARCH_TEXT,
                              request.getString(ARG_SEARCH_TEXT, ""),
                              HU.SIZE_40
                              + HU.attrs("placeholder", "Search text")));
        HU.formEntry(sb, "Under:",
                     HU.input(ARG_SEARCH_ROOT,
                              request.getString(ARG_SEARCH_ROOT, ""),
                              HU.SIZE_40
                              + HU.attrs("placeholder",
                                         "e.g. " + rootId + "...")));
        String buttons = HU.submit("Search", "search",
                                   makeButtonSubmitDialog(sb,
                                       "Searching..."));

        if (marker != null) {
            sb.append(HU.hidden(ARG_MARKER, marker));
            buttons += HU.SPACE2
                       + HU.submit("Next", "next",
                                   makeButtonSubmitDialog(sb,
                                       "Searching..."));
        }
        HU.formEntry(sb, "", buttons);
        sb.append(HU.formClose());
        sb.append(HU.formTableClose());
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
        StringBuilder sb     = new StringBuilder();
        String        rootId = getRootId(request,entry);
        getS3SearchForm(request, entry, rootId, sb, null);

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

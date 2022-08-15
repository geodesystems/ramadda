/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.plugins.aws;


import org.ramadda.repository.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.SelectInfo;

import org.ramadda.util.S3File;
import org.ramadda.util.PatternHolder;
import org.ramadda.util.TTLCache;
import org.ramadda.util.Utils;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 */
public class S3RootTypeHandler extends ExtensibleGroupTypeHandler {

    /**  */
    private TTLCache<String, List<String>> synthIdCache =
        new TTLCache<String, List<String>>(5 * 60 * 1000);


    private static int IDX=0;
    /** _more_ */
    public static final int IDX_ROOT = IDX++;

    public static final int IDX_EXCLUDE_PATTERNS = IDX++;

    public static final int IDX_MAX  = IDX++;

    public static final int IDX_PERCENT  = IDX++;
    public static final int IDX_SIZE_LIMIT  = IDX++;


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

	//	System.err.println("getSynthIds:" + request);
        boolean debug = false;
        String cacheKey = parentEntry.getId() + "_" + synthId + "_"
	    + rootEntry.getChangeDate()+"_" + request.getString(ARG_MARKER,"");
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
        String rootId = (String) rootEntry.getValue(IDX_ROOT);
        if ( !Utils.stringDefined(rootId)) {
            return ids;
        }
	List<PatternHolder> excludes = null;
        String exclude = (String) rootEntry.getValue(IDX_EXCLUDE_PATTERNS);
        int max = rootEntry.getIntValue(IDX_MAX,1000);
        double percent = rootEntry.getDoubleValue(IDX_PERCENT,(double)-100.0);
        double tmp = rootEntry.getDoubleValue(IDX_SIZE_LIMIT,(double)-1.0);
	if(Double.isNaN(tmp)) tmp = -1;
	long maxSize = (long) (tmp==-1?-1:tmp*1000*1000);
	Object[] values = rootEntry.getValues();
        if (Utils.stringDefined(exclude)) {
	    excludes  =PatternHolder.parseLines(exclude);
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
        S3File.S3ListResults results = doLs(request, new S3File(synthId), null,max,percent,maxSize);
	if(results.getMarker()!=null) {
	    request.putExtraProperty(ARG_MARKER, results.getMarker());
	}
	List<S3File> files = results.getFiles();
        List<String> children = new ArrayList<String>();
        for (S3File file : files) {
	    boolean ok = true;
	    if(excludes!=null && excludes.size()>0) {
		if(PatternHolder.checkPatterns(excludes,file.toString())) {
		    continue;
		}
	    }

            Entry bucketEntry = createBucketEntry(rootEntry, parentEntry,
						  file);
	    if(!ok) {
		if(debug)
		    System.err.println("Skipping:" + file);
		continue;
	    }
            if (bucketEntry == null) {
                continue;
            }
            getEntryManager().cacheSynthEntry(bucketEntry);
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
     * _more_
     *
     * @param rootEntry _more_
     * @param parentEntry _more_
     * @param file _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry createBucketEntry(Entry rootEntry, Entry parentEntry,
                                    S3File file)
            throws Exception {
        String name = file.getName();
        Date   dttm = new Date(file.lastModified());
        if (dttm == null) {
            dttm = new Date();
        }
        String id = getEntryManager().createSynthId(rootEntry,
                        file.toString());
	boolean isBucketType = false;
        TypeHandler bucketTypeHandler =
            getEntryManager().findDefaultTypeHandler(rootEntry, file.toString());
        if (bucketTypeHandler == null) {
	    isBucketType = true;
            bucketTypeHandler = file.isDirectory()?
                getRepository().getTypeHandler("type_s3_bucket"):
                getRepository().getTypeHandler("type_s3_file");		
        }
        Entry  bucketEntry = new Entry(id, bucketTypeHandler);
        String desc        = "";
	if(!isBucketType) desc = HU.b("AWS/S3 Bucket:") +" " +file.toString();
        Resource resource = new Resource(file.toString(), Resource.TYPE_S3,
                                         file.length());
        //      System.err.println("Resource:" +file.toString());
        Object[] values = bucketTypeHandler.makeEntryValues(null);
        bucketEntry.initEntry(name, desc, parentEntry, parentEntry.getUser(),
                              resource, "", Entry.DEFAULT_ORDER,
                              dttm.getTime(), dttm.getTime(), dttm.getTime(),
                              dttm.getTime(), values);

	if(isBucketType) bucketEntry.setValue("bucket_id", file.toString());
        bucketEntry.setMasterTypeHandler(this);

        return bucketEntry;
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
        //TODO: roll up the path creating the parent entries up to the root
        System.err.println("S3 creating:" + id);
        return createBucketEntry(rootEntry, rootEntry, S3File.createFile(id));
    }

    @Override
    public String getFoo() {
	return "this is the s3root type";
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
     * @param base _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public S3File.S3ListResults doLs(Request request, S3File base, String path, int max, double percent, long maxSize)
            throws Exception {
        S3File newFile = new S3File(getS3Path(base, path));
        return newFile.doList(false, max,percent,maxSize,request.getString(ARG_MARKER,null));
    }



}

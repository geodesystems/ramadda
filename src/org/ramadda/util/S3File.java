/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import com.amazonaws.auth.*;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder;

import java.io.*;
import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;



@SuppressWarnings({ "unchecked", "deprecation" })
public class S3File extends FileWrapper {

    public static final int SEARCH_MAX_CALLS = 25;
    public static final int SEARCH_MAX_FOUND = 500;
    public static final int PERCENT_THRESHOLD = 1000;
    public static final String S3PREFIX = "s3:";
    public static boolean debug = false;
    private String bucket;
    private AmazonS3 s3;
    private String accessKey;
    private String secretKey;
    private String endPoint; 
    private String endPointRegion;

    public S3File(String bucket) {
        this(bucket, true);
    }

    public S3File(String bucket,String key) {
	this(bucket);
	setKey(key);
    }


    public S3File(String bucket,String accesskey, String secretKey) {
	this(bucket);
	setKey(accessKey, secretKey);
    }


    public S3File(String bucket,String key, String secret,String endPoint, String region) {
	this(bucket,key, secret);
	this.endPoint = endPoint;
	this.endPointRegion = region;
    }



    public S3File(String bucket, boolean isDirectory) {
        setBucket(bucket);
        Date d = new Date();
        init(this.bucket, new File(this.bucket).getName(), isDirectory, 0,
             d.getTime());
    }


    public S3File(String bucket, String name, long size, Date d) {
        this(bucket);
        init(bucket, name, false, size, d.getTime());
    }


    public void setKey(String key) {
	if(!Utils.stringDefined(key)) return;
	List<String> toks = Utils.splitUpTo(key,":",2);
	if(toks.size()==1) {
	    String envKey = System.getenv().get(key);
	    if(!Utils.stringDefined(envKey)) {
		throw new IllegalArgumentException("No AWS S3 ENV key found:" +Utils.redact(key));
	    }
	    toks = Utils.splitUpTo(envKey,":",2);
	    if(toks.size()!=2) {
		throw new IllegalArgumentException("Bad format for AWS S3 ENV key:" +Utils.redact(envKey));
	    }
	}
	setKey(toks.get(0),toks.get(1));	
    }


    /**
     *  @return _more_
     */
    private AmazonS3 getS3() {
        if (s3 == null) {
	    AWSCredentials credentials=null;
	    if(Utils.stringDefined(accessKey) && Utils.stringDefined(secretKey))  {
		//System.err.println("Using key:" + accessKey +" secret:" + secretKey);
		credentials = new   BasicAWSCredentials(accessKey, secretKey);
	    } else {
		//		System.err.println("****** Using anonymous credentials");
		credentials = new AnonymousAWSCredentials();
	    }
	    if(Utils.stringDefined(endPoint)) {
		s3 = AmazonS3ClientBuilder.standard()
		.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, endPointRegion))
		.withCredentials(new AWSStaticCredentialsProvider(credentials))
		.withPathStyleAccessEnabled(true) // Required for many S3-compatible services
		.build();

	    } else {
		s3 = new AmazonS3Client(credentials);
	    }



        }

        return s3;
    }

    /**
     * @return _more_
     */
    @Override
    public boolean isRemoteFile() {
        return true;
    }



    /**
     *
     * @param bucket _more_
     */
    private void setBucket(String bucket) {
        this.bucket = normalizePath(bucket);
    }


    /**
     *  @return _more_
     */
    @Override
    public FileWrapper[] doListFiles() {
        try {
            S3Results results = doList(false, -1);
            if (results == null) {
                return new FileWrapper[] {};
            }
            List<S3File> files = results.files;
            if (files == null) {
                return null;
            }
            FileWrapper[] fws = new FileWrapper[files.size()];
            for (int i = 0; i < files.size(); i++) {
                fws[i] = files.get(i);
            }

            return fws;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     *
     * @param path _more_
     * @return _more_
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        path = path.trim();
        if ( !path.startsWith(S3PREFIX)) {
            if ( !path.startsWith("/")) {
                path = "/" + path;
            }
            if ( !path.startsWith("//")) {
                path = "/" + path;
            }
            path = S3PREFIX + path;
        }
        //check for s3:/...
        if (path.startsWith(S3PREFIX + "/")
                && !path.startsWith(S3PREFIX + "//")) {
            path = path.replace(S3PREFIX + "/", S3PREFIX + "//");
        }

        //      if(!path.endsWith("/")) path = path+"/";
        return path;
    }

    /**
     *
     * @param bucket _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static S3File createFile(String bucket) throws Exception {
	return createFile(bucket, null);
    }

    public static S3File createFile(String bucket, String key) throws Exception {	
        S3File       tmp   = new S3File(bucket, key);
        List<S3File> files = tmp.doList(true).files;
        if ((files != null) && (files.size() > 0)) {
            return files.get(0);
        }

        return null;
    }


    /**
     * @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doList() throws Exception {
        return doList(false, -1);
    }

    /**
     *
     * @param self _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doList(boolean self) throws Exception {
        return doList(self, -1);
    }

    /**
     *
     * @param max _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doList(int max) throws Exception {
        return doList(false, max);
    }

    /**
     *
     * @param self _more_
     * @param max _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doList(boolean self, int max) throws Exception {
        return doList(self, max, -1, -1, null);
    }

    /**
     *
     * @param key _more_
     *  @return _more_
     */
    private static String getObjectName(String key) {
        return new java.io.File(key).getName();
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Aug 16, '22
     * @author         Enter your name here...
     */
    public static class S3Results {

        /**  */
        String marker;

        /**  */
        List<S3File> files;

	String message;

        /**
         *
         *
         * @param marker _more_
         * @param files _more_
         */
        public S3Results(String marker, List<S3File> files) {
            this.marker = marker;
            this.files  = files;
	}
	public S3Results(String marker, List<S3File> files, String message) {
	    this(marker,files);
	    this.message= message;
        }

        /**
         *  @return _more_
         */
        public String getMarker() {
            return marker;
        }

        /**
         *  @return _more_
         */
        public List<S3File> getFiles() {
            return files;
        }

	public String getMessage() {
	    return message;
	}
    }


    /**
     *
     * @param self _more_
     * @param max _more_
     * @param percent _more_
     * @param maxSize _more_
     * @param marker _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doList(boolean self, int max, double percent,
                                long maxSize, String marker)
            throws Exception {
        boolean debug = false;
        //      debug = true;
        if ( !self && !isDirectory()) {
            return null;
        }
        String theBucket = bucket;
        if ( !self && isDirectory()) {
            if ( !theBucket.endsWith("/")) {
                theBucket = theBucket + "/";
            }
        }
        List<S3File> files = new ArrayList<S3File>();
        String[]     pair  = getBucketAndPrefix(theBucket);
        if (debug) {
            System.err.println("list:" + pair[0] + " key:" + pair[1]);
        }
        ListObjectsV2Request request =
            new ListObjectsV2Request().withBucketName(pair[0]).withDelimiter(
                "/");
        if (Utils.stringDefined(marker)) {
            request.setContinuationToken(marker);
        }
        if (pair[1] != null) {
            request = request.withPrefix(pair[1]);
        }
        String              key = (pair[1] != null)
                                  ? pair[1]
                                  : "";

        ListObjectsV2Result listing = getS3().listObjectsV2(request);
        List<String> commonPrefixes = listing.getCommonPrefixes();
        for (String s : commonPrefixes) {
            String name = getObjectName(s).trim();
            if (name.length() == 0) {
                continue;
            }
            if (debug) {
                System.err.println("\tPREFIX:" + name);
            }
            //Check for a self listing
	    S3File dir;
            if (self && theBucket.endsWith(name)) {
                dir = new S3File(theBucket, true);
                files.add(dir);
            } else {
                dir = new S3File(theBucket + name, true);
                files.add(dir);
            }
	    dir.setKey(accessKey, secretKey);
        }

        for (S3ObjectSummary objectSummary : listing.getObjectSummaries()) {
            if ((objectSummary.getSize() == 0)
                    && key.endsWith(objectSummary.getKey())) {
                continue;
            }
            if ((maxSize > 0) && (objectSummary.getSize() > maxSize)) {
                continue;
            }
            files.add(createS3File(objectSummary,getAccessKey(), getSecretKey()));
        }

        if (debug) {
            System.err.println("FILES:" + files);
        }

        String token = listing.getNextContinuationToken();
        return new S3Results(token, files);
    }


    /**
     *
     * @param objectSummary _more_
      * @return _more_
     */
    public static S3File createS3File(S3ObjectSummary objectSummary) {
	return createS3File(objectSummary,null, null);
    }

    public static S3File createS3File(S3ObjectSummary objectSummary,String accessKey, String secretKey) {

        String path = S3PREFIX + "//" + objectSummary.getBucketName() + "/"
                      + objectSummary.getKey();
        String name = getObjectName(objectSummary.getKey());
        //      System.err.println("\tNEW FILE:" + path);
        S3File file = new S3File(path, name, objectSummary.getSize(),
                                 objectSummary.getLastModified());

	file.setKey(accessKey, secretKey);
        return file;
    }


    /**
     *
     * @param o _more_
     *  @return _more_
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof S3File) {
            return this.bucket.equals(((S3File) o).bucket);
        }

        return false;
    }

    /**
     *  @return _more_
     */
    @Override
    public FileWrapper getParentFile() {
        String p = bucket.replaceAll("(.*)/[^/]+/?$", "$1/");
        if (p.equals("s3://")) {
            return null;
        }
        return new S3File(p,accessKey,secretKey);
    }


    /**
     *  @return _more_
     */
    public java.io.File getFile() {
        return null;
    }

    /**
     *  @return _more_
     */
    @Override
    public int hashCode() {
        return bucket.hashCode();
    }

    /**
     *  @return _more_
     */
    public boolean exists() {
        if ( !Utils.stringDefined(bucket) || bucket.equals("s3://")) {
            return false;
        }

        return true;
    }

    /**
     *
     * @param path _more_
     *  @return _more_
     */
    public static String[] getBucketAndPrefix(String path) {
        path = path.replace(S3PREFIX + "//", "");
        List<String> toks = Utils.splitUpTo(path, "/", 2);

        return new String[] { toks.get(0), (toks.size() > 1)
                                           ? toks.get(1)
                                           : null };
    }


    /**
     *
     * @param bucket _more_
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public void copyFileTo(String bucket, java.io.File file)
            throws Exception {
        String[] pair = getBucketAndPrefix(bucket);
        if (pair[1] == null) {
            System.err.println("Error: bad bucket path:" + bucket);

            return;
        }
        String host = pair[0];
        String path = pair[1];

        ListObjectsV2Request request =
            new ListObjectsV2Request().withBucketName(pair[0]).withDelimiter(
                "/");
        if (pair[1] != null) {
            request = request.withPrefix(pair[1]);
        }
        String                key       = (pair[1] != null)
                                          ? pair[1]
                                          : "";
        ListObjectsV2Result   listing   = getS3().listObjectsV2(request);
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        if (summaries.size() == 0) {
            throw new IllegalArgumentException(
                "Unable to list the S3 bucket:" + bucket);
        }
        S3ObjectSummary objectSummary = summaries.get(0);
        ObjectMetadata object = getS3().getObject(
                                    new GetObjectRequest(
                                        objectSummary.getBucketName(),
                                        objectSummary.getKey()), file);
    }


    /**
     *
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public void copyFileTo(java.io.File file) throws Exception {
        copyFileTo(bucket, file);
    }



    /**
     *
     * @param search _more_
     * @param searcher _more_
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doSearch(String search, Searcher searcher, String marker)
            throws Exception {
        return doSearch(search, searcher, SEARCH_MAX_CALLS, false,marker);
    }

    /**
     *
     * @param search _more_
     * @param searcher _more_
     * @param maxCalls _more_
     * @param verbose _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public S3Results doSearch(String search, Searcher searcher,
				  int maxCalls, boolean verbose,String marker)
            throws Exception {
	//	System.err.println("marker:"  + marker);
        List<S3File> found = new ArrayList<S3File>();
        String[]     pair  = getBucketAndPrefix(this.toString());
        ListObjectsV2Request request =
            new ListObjectsV2Request().withBucketName(pair[0]);
        if (pair[1] != null) {
            request = request.withPrefix(pair[1]);
        }
        String key      = (pair[1] != null)
                          ? pair[1]
                          : "";
        int    cnt      = 0;
        int    numCalls = 0;
        while (true) {
            if (Utils.stringDefined(marker)) {
                request.setContinuationToken(marker);
            }
	    if (found.size() > SEARCH_MAX_FOUND) {
		break;
	    }
            ListObjectsV2Result listing = getS3().listObjectsV2(request);
            for (S3ObjectSummary objectSummary :
                    listing.getObjectSummaries()) {
                cnt++;
                if (Utils.matchesOrContains(objectSummary.getKey(),search)) {
                    found.add(createS3File(objectSummary));
                    continue;
                }
                if ((searcher != null)
                        && searcher.match(search, objectSummary)) {
                    found.add(createS3File(objectSummary));
                    continue;
                }
            }

            if (verbose) {
                System.err.println("#" + cnt + " found:" + found.size());
            }

            if (++numCalls >= maxCalls) {
                break;
            }
            marker = listing.getNextContinuationToken();
            if (marker == null) {
                break;
            }
        }
	return new S3Results(marker, found,cnt>0?"Searched " + cnt +" objects":null);
    }




    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Aug 16, '22
     * @author         Enter your name here...
     */
    public static class MyFileViewer extends FileWrapper.FileViewer {

        /**  */
        boolean download = false;

        /**  */
        boolean makeDirs = false;

        /**  */
        boolean verbose = false;

        /**  */
        boolean overWrite = false;

        /**  */
        int sizeLimit = -1;

        /**  */
        double percent = -1;

        /**  */
        List<String> excludes;

        /**  */
        Appendable buffer;

        /**  */
        int maxLevel = -1;

        /**
         *
         *
         * @param buffer _more_
         * @param maxLevel _more_
         */
        public MyFileViewer(Appendable buffer, int maxLevel) {
            this.buffer   = buffer;
            this.maxLevel = maxLevel;
        }

        /**
         *
         *
         * @param download _more_
         * @param makeDirs _more_
         * @param overWrite _more_
         * @param sizeLimit _more_
         * @param percent _more_
         * @param verbose _more_
         * @param excludes _more_
         */
        public MyFileViewer(boolean download, boolean makeDirs,
                            boolean overWrite, int sizeLimit, double percent,
                            boolean verbose, List<String> excludes) {
            this.download  = download;
            this.makeDirs  = makeDirs;
            this.overWrite = overWrite;
            this.sizeLimit = sizeLimit;
            this.percent   = percent;
            this.verbose   = verbose;
            //      this.verbose = false;
            this.excludes = excludes;
        }

        /**
         *
         * @param msg _more_
         *
         * @throws Exception _more_
         */
        private void print(String msg) throws Exception {
            if (buffer != null) {
                buffer.append(msg);
            } else if (verbose) {
                System.out.print(msg);
            }
        }

        /**
         *
         * @throws Exception _more_
         */
        private void println() throws Exception {
            if (buffer != null) {
                buffer.append("\n");
            } else if (verbose) {
                System.out.println(Utils.ANSI_RESET);
            }
        }

        /**
         *
         * @param f _more_
         * @param children _more_
         *  @return _more_
         */
        private boolean downloadOk(FileWrapper f, FileWrapper[] children) {
            //only check this when there are logs of siblings
            if ((percent > 0) && (children != null)
                    && (children.length > PERCENT_THRESHOLD)) {
                if (Math.random() > percent) {
                    //              System.err.println("skipping:" + f.getName());
                    return false;
                }
            }
            if (sizeLimit <= 0) {
                return true;
            }

            return f.length() < (sizeLimit * 1000000);
        }

        /**
         *
         * @param level _more_
         *
         * @throws Exception _more_
         */
        public void printPrefix(int level) throws Exception {
            for (int i = 0; i < level; i++) {
                print("   ");
            }
        }

        /**
         *
         * @param s _more_
         *  @return _more_
         */
        private String red(String s) {
            if (buffer != null) {
                return "<span style='color:firebrick;'>" + s + "</span>";
            }

            return Utils.ANSI_RED + s + Utils.ANSI_RESET;
        }

        /**
         *
         * @param s _more_
         *  @return _more_
         */
        private String green(String s) {
            if (buffer != null) {
                return "<span style='color:green;'>" + s + "</span>";
            }

            return Utils.ANSI_GREEN + s + Utils.ANSI_RESET;
        }

        /**
         *
         * @param level _more_
         * @param file _more_
         * @param children _more_
         *  @return _more_
         *
         * @throws Exception _more_
         */
        public int viewFile(int level, FileWrapper file,
                            FileWrapper[] children)
                throws Exception {
            if ((maxLevel >= 0) && (level >= maxLevel)) {
                return DO_DONTRECURSE;
            }
            if (excludes != null) {
                for (String exclude : excludes) {
                    if (file.toString().matches(exclude)) {
                        return DO_DONTRECURSE;
                    }
                }
            }

            printPrefix(level);
            if ( !file.isDirectory()) {
                print(red(file.getName()) + " "
                      + Utils.formatFileLength(file.length()));
                if (download && downloadOk(file, children)) {
                    String path = file.toString();
                    print(" downloading... ");
                    java.io.File dir = new java.io.File(".");
                    if (makeDirs) {
                        for (FileWrapper fv : stack) {
                            dir = new java.io.File(dir, fv.getName());
                            if ( !dir.exists()) {
                                print(" mkdir:" + dir + " ");
                                dir.mkdirs();
                            }
                        }
                    }

                    java.io.File dest;
                    if (makeDirs) {
                        //                      System.err.println("DIR:" + dir +" NAME:" + file.getName());
                        dest = new java.io.File(dir, file.getName());
                    } else {
                        //                      System.err.println("NOMAKEDIRS");
                        dest = new java.io.File(file.getName());
                    }

                    if (dest.exists() && !overWrite) {
                        print(" exists");
                    } else {
                        file.copyFileTo(dest);
                    }
                }
                println();

                return DO_DONTRECURSE;
            } else {
                print(green(file.getName()));
                println();

                return DO_CONTINUE;
            }
        }
    }



    /**
     *
     * @param msg _more_
     */
    public static void usage(String msg) {
        if (msg != null) {
            System.err.println(msg);
        }
        System.err.println(
            "Usage:\nS3File \n\t<-key KEY_SPEC (Key spec is either a accesskey:secretkey or an env variable set to accesskey:secretkey)>\n\t<-download  download the files>  \n\t<-nomakedirs don't make a tree when downloading files> \n\t<-overwrite overwrite the files when downloading> \n\t<-sizelimit size mb (don't download files larger than limit (mb)> \n\t<-percent 0-1  (for buckets with many (>100) siblings apply this as percent probablity that the bucket will be downloaded)> \n\t<-recursive  recurse down the tree when listing>\n\t<-search search_term>\n\t<-self print out the details about the bucket> ... one or more buckets");
        Utils.exitTest(0);
    }




    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {

        String dflt =
            "s3://first-street-climate-risk-statistics-for-noncommercial-use/";
        dflt = "s3://noaa-gsod-pds/1985";
        //      debug = true;
        final int[]  cnt         = { 0 };
        List<String> excludes    = new ArrayList<String>();
        String       search      = null;
        boolean      doDownload  = false;
        boolean      makeDirs    = true;
        boolean      overWrite   = false;
        boolean      doSelf      = false;
        boolean      doRecursive = false;
        boolean      verbose     = false;
        int          maxCalls    = 10;
        double       percent     = -1;
        int          sizeLimit   = -1;
	String key = null;
        for (int i = 0; i < args.length; i++) {
            String path = args[i];
            if (path.startsWith("--")) {
                path = path.substring(1);
            }
            if (path.equals("-help")) {
                usage(null);
            }
            if (path.equals("-s3")) {
                continue;
            }
            if (path.equals("default")) {
                path = dflt;
            }
            if (path.equals("-debug")) {
                debug = true;
                continue;
            }
            if (path.equals("-overwrite")) {
                overWrite = true;
                continue;
            }
            if (path.equals("-verbose")) {
                verbose = true;
                continue;
            }
            if (path.equals("-exclude")) {
                if (i == args.length - 1) {
                    usage("Bad exclude arg");
                }
                excludes.add(args[++i]);
                continue;
            }
            if (path.equals("-key")) {
                if (i == args.length - 1) {
                    usage("Bad key arg");
                }
		key = args[++i];
                continue;
            }

            if (path.equals("-maxcalls")) {
                if (i == args.length - 1) {
                    usage("Bad maxcals arg");
                }
                maxCalls = Integer.parseInt(args[++i]);
                continue;
            }
            if (path.equals("-sizelimit")) {
                if (i == args.length - 1) {
                    usage("Bad limit arg");
                }
                sizeLimit = Integer.parseInt(args[++i]);
                continue;
            }
            if (path.equals("-search")) {
                if (i == args.length - 1) {
                    usage("Bad search arg");
                }
                search = args[++i];
                continue;
            }
            if (path.equals("-percent")) {
                if (i == args.length - 1) {
                    usage("Bad percent arg");
                }
                percent = Double.parseDouble(args[++i]);
                continue;
            }
            if (path.equals("-nomakedirs")) {
                makeDirs = false;
                continue;
            }
            if (path.equals("-makedirs")) {
                makeDirs = true;
                continue;
            }
            if (path.equals("-download")) {
                doDownload = true;
                continue;
            }
            if (path.equals("-self")) {
                doSelf = true;
                continue;
            }
            if (path.equals("-recursive")) {
                doRecursive = true;
                verbose     = true;
                continue;
            }
            if (path.startsWith("-")) {
                usage("Unknown arg:" + path);
            }

            if (doSelf) {
                S3File file = createFile(path,key);
                if (file != null) {
                    System.out.println("Got:" + file.toStringVerbose());
                } else {
                    System.out.println("Failed:" + path);
                }
                continue;
            }
            if (search != null) {
                S3File file = new S3File(path,key);
                S3Results results = file.doSearch(search, null, maxCalls,
						      verbose,null);
                if (results.files.size() == 0) {
                    System.err.println("Nothing found");
                } else {
                    System.out.print(Utils.wrap(results.files, "", "\n"));
                }
                continue;
            }

            S3File  f = new S3File(path,key);
            if (doRecursive) {
                FileWrapper.walkDirectory(f, new MyFileViewer(doDownload,
                        makeDirs, overWrite, sizeLimit, percent, verbose,
                        excludes), 0);
                continue;
            }

            if (doDownload) {
                java.io.File dest = new java.io.File(f.getName());
                System.err.println("Downloading:" + f);
                f.copyFileTo(dest);
                continue;
            }

            FileWrapper[] files = f.listFiles();
            if (files == null) {
                System.out.println("No files");
            } else {
                if (files.length == 0) {
                    System.out.println("No files");
                }
                for (FileWrapper fw : files) {
                    if (fw.isDirectory()) {
                        //                      System.out.println("dir:" +fw.getName());
                    } else {
                        //                      System.out.println("file:"+fw.toStringVerbose());
                    }
                }
            }
        }

    }

    /**
     * Interface description
     *
     *
     * @author         Enter your name here...    
     */
    public interface Searcher {

        /**
         *
         * @param s _more_
         * @param objectSummary _more_
          * @return _more_
         */
        public boolean match(String s, S3ObjectSummary objectSummary);
    }

    public void setKey (String accessKey, String secretKey) {
	setAccessKey(accessKey);
	setSecretKey(secretKey);	
    }

     /**
       Set the AccessKey property.

       @param value The new value for AccessKey
    **/
    public void setAccessKey (String value) {
	accessKey = value;
    }

    /**
       Get the AccessKey property.

       @return The AccessKey
    **/
    public String getAccessKey () {
	return accessKey;
    }

    /**
       Set the SecretKey property.

       @param value The new value for SecretKey
    **/
    public void setSecretKey (String value) {
	secretKey = value;
    }

    /**
       Get the SecretKey property.

       @return The SecretKey
    **/
    public String getSecretKey () {
	return secretKey;
    }

    public static final class S3Info {
    }

}

/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Class description
 *
 *
 * @version        $version$, Sun, Aug 7, '22
 * @author         Enter your name here...    
 */
@SuppressWarnings("unchecked")
public class S3File extends FileWrapper {

    public static final String S3PREFIX = "s3:";

    public static boolean debug = false;

    private static String awsPath = "aws";
    
    /**  */
    private static final SimpleDateFormat sdf;
    static {
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(Utils.TIMEZONE_DEFAULT);
    }

    /**  */
    private String bucket;

    /**
     
     *
     * @param bucket _more_
     */
    public S3File(String bucket) {
	this(bucket,true);
    }

    /**
     *
     * @param bucket _more_
     * @param isDirectory _more_
     */
    public S3File(String bucket, boolean isDirectory) {
	setBucket(bucket);
        Date d = new Date();
        init(this.bucket, new File(this.bucket).getName(), isDirectory, 0, d.getTime());
    }

    public static void setAwsPath(String path) {
	awsPath  =path;
    }

    private void setBucket(String bucket) {
	this.bucket  = cleanupBucket(bucket);
    }

    /**
     *
     * @param bucket _more_
     * @param name _more_
     * @param size _more_
     * @param d _more_
     */
    public S3File(String bucket, String name, long size, Date d) {
	this(bucket);
        init(bucket, name, false, size, d.getTime());
    }

    /**
     *
     * @param commands _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static  String run(List<String> commands) throws Exception {
        ProcessBuilder pb      = new ProcessBuilder(commands);
        Process        process = pb.start();
        int            result  = process.waitFor();
        InputStream    is      = process.getInputStream();
        InputStream    es      = process.getErrorStream();	
	if(debug)
	    System.err.println("S3File.run:" + commands);
        String error = IO.readInputStream(es);
	if(error.trim().length()>0) {
	if(debug)
	    System.err.println("Got error:" + error);
	    throw new RuntimeException("Error executing:" + commands+"\nError:" + error);
	}
        String results =  IO.readInputStream(is);
	if(debug)
	    System.err.println("Got results:" + results);
	return results;
    }

    /**
      * @return _more_
     */
    @Override
    public FileWrapper[] doListFiles() {
        try {
	    List<S3File> files = doList(false);
	    if(files==null) return null;
            FileWrapper[] fws = new FileWrapper[files.size()];
            for (int i = 0; i < files.size(); i++) {
                fws[i] = files.get(i);
            }

            return fws;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public static String cleanupBucket(String bucket) {
	if(bucket==null) return null;
	bucket = bucket.trim();
	if(!bucket.startsWith(S3PREFIX)) {
	    if(!bucket.startsWith("/")) bucket = "/"+bucket;
	    if(!bucket.startsWith("//")) bucket = "/"+bucket;		
	    bucket = S3PREFIX+bucket;
	}
	//	if(!bucket.endsWith("/")) bucket = bucket+"/";
	return bucket;
    }

    public static S3File createFile(String bucket) throws Exception {
	S3File tmp = new S3File(bucket);
	List<S3File> files = tmp.doList(true);
	if(files!=null && files.size()>0) return files.get(0);
	return null;
    }

    private S3File createFileFromLine(String parent, String line, boolean self) throws Exception {
	if (line.startsWith("PRE ")) {
	    String sub = line.substring(4).trim();
	    String path;
	    if(self)
		path = parent;
	    else  {
		path = parent;
		if(!path.endsWith("/")) path +="/";
		path += sub;		
	    }
	    return new S3File(path, true);
	} else {
	    //2020-10-06 10:42:25    5908731 FSF_Flood_Model_Technical_Documentation.pdf
	    List<String> toks = Utils.splitUpTo(line, " ", 4);
	    if (toks.size() != 4) {
		return null;
	    }
	    String date = toks.get(0) + " " + toks.get(1);
	    Date   dttm = sdf.parse(date);
	    long   size = Long.parseLong(toks.get(2).trim());
	    String name = toks.get(3).trim();
	    String path = self?parent:parent + name;
	    return new S3File(path, name, size, dttm);
	}
    }	


    public List<S3File> doList() throws Exception {
	return doList(false);
    }


    public List<S3File> doList(boolean self) throws Exception {
	if(!self && !isDirectory()) {
	    return null;
	}
	String theBucket = bucket;
	if(!self && isDirectory()) {
	    if(!theBucket.endsWith("/")) theBucket = theBucket+"/";
	}

	List<String> commands = (List<String>) Utils.makeList(awsPath,
							      "s3", "ls", "--no-sign-request",
							      theBucket);
	String            result = run(commands);
	List<S3File> files  = new ArrayList<S3File>();
	for (String line : Utils.split(result, "\n", true, true)) {
	    S3File file = createFileFromLine(bucket,line,self);
	    if(file!=null) files.add(file);
	}
	return files;
    }



    /**
     *
     * @param o _more_
      * @return _more_
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof S3File) {
            return this.bucket.equals(((S3File) o).bucket);
        }

        return false;
    }

    /**
      * @return _more_
     */
    @Override
    public FileWrapper getParentFile() {
	/*
	String parent = bucket;
	if(parent.endsWith("/")) parent = parent.substring(0,parent.length()-1);
	String parent = bucket.replace("/[^/]+/?$");
	*/
	String p = bucket.replaceAll("(.*)/[^/]+/?$","$1/");
	if(p.equals("s3://")) return null;
	return new S3File(p);
    }


    /**
      * @return _more_
     */
    public java.io.File getFile() {
        return null;
    }

    /**
      * @return _more_
     */
    @Override
    public int hashCode() {
        return bucket.hashCode();
    }

    /**
      * @return _more_
     */
    public boolean exists() {
	if(!Utils.stringDefined(bucket) || bucket.equals("s3://")) return false;
        return true;
    }

    @Override
    public boolean isRemoteFile() {
	return true;
    }

    public static void copyFileTo(String bucket, java.io.File file) throws Exception {
	List<String> commands = (List<String>) Utils.makeList(awsPath,
							      "s3", "cp", "--no-sign-request",
							      bucket,file.toString());
	String            result = run(commands);
    }    


    public void copyFileTo(java.io.File file) throws Exception {
	copyFileTo(bucket,file);
    }    

    /**
     *
     * @param args _more_
     */
    public static void main(String[] args) throws Exception {
	String dflt = "s3://noaa-nexrad-level2";
	if(args.length==0) args=new String[]{dflt};
	final int[] cnt = {0};

        FileWrapper.FileViewer  fileViewer = new FileWrapper.FileViewer() {
		public int viewFile(int level,FileWrapper f) throws Exception {
		    if(cnt[0]++>200)
			return DO_DONTRECURSE;
		    for(int i=0;i<level;i++)
			System.err.print("  ");
		    System.err.print(f.getName());
		    if(!f.isDirectory())
			System.err.print(" "+f.length());
		    System.err.println("");
                if (f.isDirectory()) {
		    return DO_CONTINUE;
		} else {
		    return DO_DONTRECURSE;
		}
            }
        };

	boolean doSelf  = false;
	boolean doRecurse = false;
        for (String path : args) {
	    if(path.equals("default")) path = dflt;
	    if(path.equals("-debug")) {
		debug = true;
		continue;
	    }
	    if(path.equals("-self")) {
		doSelf = true;
		continue;
	    }
	    if(path.equals("-recurse")) {
		doRecurse = true;
		continue;
	    }	    
	    if(doSelf) {
		S3File file = createFile(path);
		if(file!=null) 
		    System.err.println("Got:" + file.toStringVerbose());
		else
		    System.err.println("Failed:" + path);
		continue;
	    }
            FileWrapper        f     = FileWrapper.createFileWrapper(path);
	    if(doRecurse) {
		FileWrapper.walkDirectory(f, fileViewer);
		continue;
	    }

            FileWrapper[] files = f.listFiles();
            if (files == null) {
                System.err.println("Null files");
            } else {
		if(files.length==0) {
		    System.err.println("No files");
		}
                for (FileWrapper fw : files) {
                    System.err.println(fw.toStringVerbose());
                }
            }
        }
    }
}

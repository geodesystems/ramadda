/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;


import ucar.unidata.util.Misc;

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
        init(bucket, new File(this.bucket).getName(), isDirectory, 0, d.getTime());
    }

    private void setBucket(String bucket) {
	if(bucket!=null) bucket = bucket.trim();
	this.bucket = bucket;
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
        return IO.readInputStream(is);
    }

    /**
      * @return _more_
     */
    @Override
    public FileWrapper[] doListFiles() {
        try {
	    if(!isDirectory()) {
		return null;
	    }
	    String b = bucket;
	    if(!b.endsWith("/")) b = b+"/";
            List<String> commands = (List<String>) Utils.makeList("aws",
                                        "s3", "ls", "--no-sign-request",
                                        bucket);
            String            result = run(commands);
            List<FileWrapper> files  = new ArrayList<FileWrapper>();
            for (String line : Utils.split(result, "\n", true, true)) {
                if (line.startsWith("PRE ")) {
                    String sub = line.substring(4).trim();
                    files.add(new S3File(bucket + sub, true));
                } else {
                    //2020-10-06 10:42:25    5908731 FSF_Flood_Model_Technical_Documentation.pdf
                    List<String> toks = Utils.splitUpTo(line, " ", 4);
                    if (toks.size() != 4) {
                        continue;
                    }
                    String date = toks.get(0) + " " + toks.get(1);
                    Date   dttm = sdf.parse(date);
                    long   size = Long.parseLong(toks.get(2).trim());
                    String name = toks.get(3).trim();
                    files.add(new S3File(bucket + name, name, size, dttm));
                }
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
	List<String> commands = (List<String>) Utils.makeList("aws",
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
        FileWrapper.FileViewer         fileViewer = new FileWrapper.FileViewer() {
		public int viewFile(int level,FileWrapper f) throws Exception {
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


        for (String path : args) {
            FileWrapper        f     = FileWrapper.createFileWrapper(path);
	    FileWrapper.walkDirectory(f, fileViewer);
	    if(true) continue;


            FileWrapper[] files = f.listFiles();
            if (files == null) {
                System.err.println("No files");
            } else {
                for (FileWrapper fw : files) {
                    System.err.println("F:" + fw.getName() + " size:"
                                       + fw.length() + " d:"
                                       + new Date(fw.lastModified()));
                }
            }
        }
    }
}

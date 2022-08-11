/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

/**
 * Class description
 *
 *
 * @version        $version$, Sun, Aug 7, '22
 * @author         Enter your name here...
 */
@SuppressWarnings("unchecked")
public class S3File extends FileWrapper {

    public static final int PERCENT_THRESHOLD = 1000;

    /**  */
    public static final String S3PREFIX = "s3:";

    /**  */
    public static boolean debug = false;

    /**  */
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
     *
     * @param bucket _more_
     */
    public S3File(String bucket) {
        this(bucket, true);
    }

    /**
     *
     * @param bucket _more_
     * @param isDirectory _more_
     */
    public S3File(String bucket, boolean isDirectory) {
        setBucket(bucket);
        Date d = new Date();
        init(this.bucket, new File(this.bucket).getName(), isDirectory, 0,
             d.getTime());
    }

    /**
     *
     * @param path _more_
     */
    public static void setAwsPath(String path) {
        awsPath = path;
    }

    /**
     *
     * @param bucket _more_
     */
    private void setBucket(String bucket) {
        this.bucket = cleanupBucket(bucket);
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
     *  @return _more_
     *
     * @throws Exception _more_
     */
    public static String run(List<String> commands) throws Exception {
        if (debug) {
            System.err.println("S3File.run:" + commands);
        }
	String[] results  = Utils.runCommands(commands);
        if (Utils.stringDefined(results[0])) {
            if (debug) {
                System.err.println("Got error:" + results[0]);
            }

            throw new RuntimeException("Error executing:" + commands
                                       + "\nError:" + results[0]);
        }
        if (debug) {
            System.err.println("Got results:" + results[1]);
        }
        return results[1];
    }

    /**
     *  @return _more_
     */
    @Override
    public FileWrapper[] doListFiles() {
        try {
            List<S3File> files = doList(false,-1);
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
     * @param bucket _more_
      * @return _more_
     */
    public static String cleanupBucket(String bucket) {
        if (bucket == null) {
            return null;
        }
        bucket = bucket.trim();
        if ( !bucket.startsWith(S3PREFIX)) {
            if ( !bucket.startsWith("/")) {
                bucket = "/" + bucket;
            }
            if ( !bucket.startsWith("//")) {
                bucket = "/" + bucket;
            }
            bucket = S3PREFIX + bucket;
        }
        //check for s3:/...
        if (bucket.startsWith(S3PREFIX + "/")
                && !bucket.startsWith(S3PREFIX + "//")) {
            bucket = bucket.replace(S3PREFIX + "/", S3PREFIX + "//");
        }

        //      if(!bucket.endsWith("/")) bucket = bucket+"/";
        return bucket;
    }

    /**
     *
     * @param bucket _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public static S3File createFile(String bucket) throws Exception {
        S3File       tmp   = new S3File(bucket);
        List<S3File> files = tmp.doList(true);
        if ((files != null) && (files.size() > 0)) {
            return files.get(0);
        }
        return null;
    }

    /**
     *
     * @param parent _more_
     * @param line _more_
     * @param self _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    private S3File createFileFromLine(String parent, String line,
                                      boolean self)
            throws Exception {
        if (line.startsWith("PRE ")) {
            String sub = line.substring(4).trim();
            String path;
            if (self) {
                path = parent;
            } else {
                path = parent;
                if ( !path.endsWith("/")) {
                    path += "/";
                }
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
            String path = self
                          ? parent
                          : parent + name;

            return new S3File(path, name, size, dttm);
        }
    }


    /**
      * @return _more_
     *
     * @throws Exception _more_
     */
    public List<S3File> doList() throws Exception {
        return doList(false,-1);
    }

    public List<S3File> doList(boolean self) throws Exception {
	return doList(self, -1);
    }

    public List<S3File> doList(int max) throws Exception {
	return doList(false, max);
    }

    /**
     *
     * @param self _more_
      * @return _more_
     *
     * @throws Exception _more_
     */
    public List<S3File> doList(boolean self, int max) throws Exception {
	return doList(self, max,-1);
    }

    public List<S3File> doList(boolean self, int max,double percent) throws Exception {	
        if ( !self && !isDirectory()) {
            return null;
        }
        String theBucket = bucket;
        if ( !self && isDirectory()) {
            if ( !theBucket.endsWith("/")) {
                theBucket = theBucket + "/";
            }
        }

        List<String> commands = (List<String>) Utils.makeList(awsPath, "s3",
                                    "ls", "--no-sign-request", theBucket);
        String       result = run(commands);
        List<S3File> files  = new ArrayList<S3File>();
	List<String> lines = Utils.split(result, "\n", true, true) ;
	for (String line : lines) {
	    if(percent>0 && lines.size()>PERCENT_THRESHOLD) {
		if(Math.random()>percent) {
		    continue;
		}		    
	    }
	    S3File file = createFileFromLine(bucket, line, self);
            if (file != null) {
                files.add(file);
		if(max>0 && files.size()>=max) break;
            }
        }

        return files;
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
        /*
        String parent = bucket;
        if(parent.endsWith("/")) parent = parent.substring(0,parent.length()-1);
        String parent = bucket.replace("/[^/]+/?$");
        */
        String p = bucket.replaceAll("(.*)/[^/]+/?$", "$1/");
        if (p.equals("s3://")) {
            return null;
        }

        return new S3File(p);
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
      * @return _more_
     */
    @Override
    public boolean isRemoteFile() {
        return true;
    }

    static long ms = 0;
    static int cnt = 0;

    /**
     *
     * @param bucket _more_
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public static void copyFileTo(String bucket, java.io.File file)
	throws Exception {
	//This can give the presigned URL

	java.io.File tmp = new java.io.File(file.toString()+".part");
	bucket = bucket.replace(S3PREFIX+"//","");
	List<String> toks = Utils.splitUpTo(bucket,"/",2);
	String host = toks.get(0);
	String path =toks.get(1);
	/*
	String surl  = "https://" + host +".s3.amazonaws.com/" + HtmlUtils.urlEncodeSpace(path);
	List<String> presign = (List<String>) Utils.makeList(awsPath,"s3","presign",bucket);
	String presignUrl = run(presign);
	surl = presignUrl;
	int pause= 500;
	for(int tries=0;tries<10;tries++) {
	    URL url = new URL(surl);
	    URLConnection connection = url.openConnection();
	    HttpURLConnection huc      = (HttpURLConnection) connection;
	    try {
		FileOutputStream fos = new FileOutputStream(tmp);
		IOUtil.writeTo(huc.getInputStream(), fos);
		fos.close();
		tmp.renameTo(file);
	    } catch(Exception exc) {
		//Check for the slow down
		int               response = huc.getResponseCode();
		if(response==503) {
		    System.err.println("****** Had a pause request ******");
		    Misc.sleep(pause);
		    pause+=500;
		    continue;
		}
		throw exc;
	    }
	}
	if(true)
	    return;
	*/

	/*
        List<String> commands = (List<String>) Utils.makeList(awsPath, "s3",
                                    "cp", "--no-sign-request", bucket,
                                    tmp.toString());
	*/
        List<String> commands = (List<String>) Utils.makeList(awsPath, "s3api", "get-object",
							      "--no-sign-request",
							      "--bucket",host,
							      "--key",path,
							     tmp.toString());
	//	System.err.println(commands);
	long t1 = System.currentTimeMillis();
        String result = run(commands);
	long t2 = System.currentTimeMillis();
	tmp.renameTo(file);
	cnt++;
	ms+=(t2-t1);
	//	System.err.println("#: " +bucket +" " + cnt +" " +(ms/cnt));
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

    public static void usage(String msg) {
	System.err.println(msg);
	System.err.println("Usage:\nS3File \n\t<-download  download the files>  \n\t<-makedirs make a tree when downloading files> \n\t<-overwrite overwrite the files when downloading> \n\t<-sizelimit size mb (don't download files larger than limit (mb)> \n\t<-percent 0-1  (for buckets with many (>100) siblings apply this as percent probablity that the bucket will be downloaded)> \n\t<-recurse  recurse down the tree when listing> \n\t<-self print out the details about the bucket> ... one or more buckets");
	Utils.exitTest(0);
    }

    static class MyFileViewer extends FileWrapper.FileViewer {
	boolean download;
	boolean makeDirs;
	boolean verbose;
	boolean overWrite;	
	int sizeLimit = -1;
	double percent = -1;
	List<String> excludes;

	public MyFileViewer(boolean download, boolean makeDirs,boolean overWrite, int sizeLimit, double percent, boolean verbose,List<String> excludes) {
	    this.download = download;
	    this.makeDirs = makeDirs;
	    this.overWrite = overWrite;
	    this.sizeLimit = sizeLimit;
	    this.percent = percent;
	    this.verbose= verbose;
	    this.excludes = excludes;
	}
	private void print(String msg) {
	    if(verbose) 
		System.out.print(msg);
	}
	private void println() {
	    if(verbose) 
		System.out.println(Utils.ANSI_RESET);
	}
	
	private boolean downloadOk(FileWrapper f, FileWrapper[] children) {
	    //only check this when there are logs of siblings
	    if(percent>0 && children!=null && children.length>PERCENT_THRESHOLD) {
		if(Math.random()>percent) {
		    //		    System.err.println("skipping:" + f.getName());
		    return false;
		}
	    }
	    if(sizeLimit<=0) return true;
	    return f.length()<(sizeLimit*1000000);
	}
	public void printPrefix(int level) {
	    for (int i = 0; i < level; i++) {
		print("   ");
	    }
	}
	public int viewFile(int level, FileWrapper f, FileWrapper[] children) throws Exception {
	    for(String exclude: excludes) {
		if(f.toString().matches(exclude))
		    return DO_DONTRECURSE;
	    }

	    printPrefix(level);
	    if ( !f.isDirectory()) {
		print(Utils.ANSI_RED +f.getName() +Utils.ANSI_RESET+ " " + Utils.formatFileLength(f.length()));
		if(download && downloadOk(f,children)) {
		    String path = f.toString();
		    print(" downloading... ");
		    java.io.File dir  = new java.io.File(".");
		    if(makeDirs) {
			for(FileWrapper fv: stack) {
			    dir = new java.io.File(dir,fv.getName());
			    if(!dir.exists()) {
				print(" dir:" +dir +" ");
				dir.mkdirs();
			    }
			}
		    }

		    java.io.File dest;
		    if(makeDirs) {
			dest =  new java.io.File(dir,f.getName());
		    } else {
			dest = new java.io.File(f.getName());
		    }

		    if(dest.exists() && !overWrite) {
			print(" exists");
		    } else {
			//			print(" copying file:" +dest);
			f.copyFileTo(dest);
		    }
		}
		println();
		return DO_DONTRECURSE;
	    }  else {
		print(Utils.ANSI_GREEN+f.getName());
		println();
		return DO_CONTINUE;
	    }
	}
    }




    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        //      debug = true;
        String dflt = "s3://noaa-nexrad-level2";
        if (args.length == 0) {
            args = new String[] { dflt };
        }
        final int[]            cnt        = { 0 };
        List<String> excludes = new ArrayList<String>();
        boolean doDownload =false;
        boolean makeDirs= false;
	boolean overWrite= false;
        boolean doSelf    = false;
        boolean doRecurse = false;
        boolean verbose= false;	
	double percent = -1;
	int sizeLimit = -1;
	for (int i=0;i<args.length;i++) {
	    String path =args[i];
	    
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
	    if(path.equals("-overwrite")) {
		overWrite = true;
		continue;
	    }
	    if(path.equals("-verbose")) {
		verbose= true;	
		continue;
	    }
            if (path.equals("-exclude")) {
		if(i==args.length-1) {
		    usage("Bad exclude arg");
		}
		excludes.add(args[++i]);
                continue;
	    }
            if (path.equals("-sizelimit")) {
		if(i==args.length-1) {
		    usage("Bad limit arg");
		}
		sizeLimit = Integer.parseInt(args[++i]);
                continue;
	    }
            if (path.equals("-percent")) {
		if(i==args.length-1) {
		    usage("Bad percent arg");
		}
		percent = Double.parseDouble(args[++i]);
                continue;
	    }	    

            if (path.equals("-makedirs")) {
                makeDirs= true;
                continue;
	    }
            if (path.equals("-download")) {
                doDownload= true;
                continue;
            }	    
            if (path.equals("-self")) {
                doSelf = true;
                continue;
            }
            if (path.equals("-recurse")) {
                doRecurse = true;
		verbose =true;
                continue;
            }
            if (path.startsWith("-")) {
		usage("Unknown arg:" + path);
	    }
            if (doSelf) {
                S3File file = createFile(path);
                if (file != null) {
		    System.out.println("Got:" + file.toStringVerbose());
                } else {
                    System.out.println("Failed:" + path);
                }
                continue;
            }
            FileWrapper f = FileWrapper.createFileWrapper(path);
	    if (doRecurse) {
                FileWrapper.walkDirectory(f, new MyFileViewer(doDownload, makeDirs,overWrite,sizeLimit,percent,verbose,excludes),0);
                continue;
            }

	    if(doDownload) {
		java.io.File dest = new java.io.File(f.getName());
		System.err.println("Downloading:" + f);
		//		for(int x=0;x<10;x++) {
		    f.copyFileTo(dest);
		    //		}
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
		    if(fw.isDirectory()) {
			System.out.println("dir:" +fw.getName());
		    } else {
			System.out.println("file:"+fw.toStringVerbose());
		    }
                }
            }
        }
    }
}

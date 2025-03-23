/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;
import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.regex.*;

import java.net.URL;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import java.util.Random;

public class Test {
    private static Date startTime;
    private static Object MUTEX=new Object();
    private static int activeThreads = 0;
    private static int numThreads = 50;
    private static int totalRead =0;
    private static int loops = 1000;
    private static int sleep=0;
    private static int timeThreshold = 3000;
    private static boolean showSize = true;
    private static boolean verbose = false;
    private static boolean quiet = false;    
    private static boolean noecho = false;
    private static boolean suddenDeath = false;        
    private static int skip=0;
    private static List<String> randos = new ArrayList<String>();
    int urlCnt=0;

    public Test(List<String> urls) {
	Misc.runInABit(100,new Runnable() {
		public void run() {
		    synchronized(MUTEX) {
			activeThreads++;
		    }
		    runTest(urls);
		    synchronized(MUTEX) {
			numThreads--;
			activeThreads--;
			System.out.println("DONE:" + numThreads);
		    }
		}
	    });
    }

    public  void runTest(List<String> urls) {
	try {
	    int cnt=0;
	    boolean ok = true;
	    while(cnt++<loops && ok) {
		//		System.err.println("cnt:" + cnt);
		boolean didRando = false;
		for(String url:urls) {
		    if(verbose)
			System.out.println("call:" + url);
		    ok=call(url);
		    if(!ok) break;
		    double r = Math.random();
		    if(!didRando && randos.size()>0 && r<0.05) {
			didRando = true;
			for(String u: randos) {
			    System.out.println("random:" + u);
			    ok = call(u);
			    if(!ok) break;
			}
		    }
		}
	    }
	} catch(Exception exc) {
	    System.out.println("error:" + exc);
	    return;
	}
    }

    public boolean call(String url) throws Exception {
	url = url.trim();
	if(url.startsWith("#")) return true;
	boolean print = false;
	if(url.startsWith("print:")) {
	    print=true;
	    url = url.substring("print:".length()).trim();
	}
	if(url.startsWith("children:")) {
	    String id = url.substring("children:".length());
	    String csvUrl ="https://ramadda.org/entry/show?ascending=true&orderby=name&output=default.csv&escape=true&fields=name,id&showheader=false&showheader=false&entryid="+ id;
	    String csv = IO.readContents(csvUrl);
	    for(String line:Utils.split(csv,"\n",true,true)) {
		List<String> toks = Utils.splitUpTo(line,",",2);
		String _url = "https://ramadda.org/entry/show?entryid=" +toks.get(1);
		long t1 = System.currentTimeMillis();
		if(!call(_url)) {
		    System.err.println("failed:" + toks.get(0) +" " + toks.get(1));
		    return false;
		}
		long t2 = System.currentTimeMillis();
		if((t2-t1)>timeThreshold) {
		    System.err.println("******  long time:" + (t2-t1)+ " " + toks.get(0) +" " + toks.get(1));
		}
	    }
	    return true;
	}	    

	if(url.startsWith("echo:")) {
	    if(!noecho)	    System.out.println(url.substring("echo:".length()));
	    return true;
	}
	if(url.startsWith("stop")) {
	    return false;
	}
	    
	if(url.startsWith("sleep ")) {
	    int s = Integer.parseInt(url.substring("sleep ".length()).trim());
	    Misc.sleep(s);
	    return true;
	}
	long expectedSize = -1;
	if(url.startsWith("size:")) {
	    url  =url.substring("size:".length()).trim();
	    int index = url.indexOf(" ");
	    expectedSize = Long.parseLong(url.substring(0,index).trim());
	    url = url.substring(index).trim();
	}

	if(skip>0) {
	    urlCnt++;
	    skip--;
	    return true;
	}



	Date before = new Date();
	IO.Result result = IO.doGetResult(new URL(url));
	if(result.getError()) {
	    String err= result.getResult();
	    String inner = StringUtil.findPattern(err.replace("\n"," "),"<!-- content begin-->(.*)<!-- content end-->");

	    if(inner!=null) {
		inner = Utils.stripTags(inner);
		err  = inner;
	    }
	    System.out.println("read error:" + err);
	    if(suddenDeath) System.exit(1);

	    return true;
	} else if(print) {
	    System.out.println(result.getResult().trim());
	}
	if(expectedSize>=0 && result.getResult().length() != expectedSize) {
	    throw new IllegalStateException("Incorrect size for URL:" + url +" expected size:" + expectedSize +" actual size:" + result.getResult().length());
	}
	synchronized(MUTEX) {
	    totalRead++;
	    Date after = new Date();
	    long time = after.getTime()-before.getTime();
	    String title = StringUtil.findPattern(result.result,"<title>(.*?)</title>");
	    if(title==null) title="";
	    if(timeThreshold>=0 && time>timeThreshold) {
		if(!quiet)
		    System.out.println("#" + urlCnt +" " + title+ " ****** long time:" + (time) +" url:" +url);
	    }
	    long diff = (after.getTime()-startTime.getTime())/1000;
	    int callsPer = diff<=0?0:(int)(totalRead/(double)diff);
	    if(!quiet)
		System.out.println("#" + (urlCnt++)+" total read:" + totalRead + " calls/s:" + callsPer +" time:" + time +" #threads:"+ activeThreads+(!showSize?"":" size:"+result.getResult().length()));

	}
	if(sleep>0) Misc.sleep(sleep);
	return true;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
	startTime = new Date();
	final List<String> urls=new ArrayList<String>();
	for(int i=0;i<args.length;i++) {
	    if(args[i].equals("-help")) {
		System.out.println("usage: -threads <# threads> -loops <#loops> -rando <some random URL> -t <time threshold> -verbose -quiet -noecho -suddendeath -skip <skip N urls> -sleep <pause after each call (ms)> <file> or <url>");
		System.exit(0);
	    }

	    if(args[i].equals("-t")) {
		timeThreshold= Integer.parseInt(args[++i]);
		continue;
	    }


	    if(args[i].equals("-threads")) {
		numThreads = Integer.parseInt(args[++i]);
		continue;
	    }
	    if(args[i].equals("-loops")) {
		loops = Integer.parseInt(args[++i]);
		continue;
	    }
	    if(args[i].equals("-rando")) {
		randos.add(args[++i]);
		continue;
	    }
	    if(args[i].equals("-verbose")) {
		verbose = true;
		continue;
	    }
	    if(args[i].equals("-quiet")) {
		quiet = true;
		continue;
	    }	    

	    if(args[i].equals("-noecho")) {
		noecho = true;
		continue;
	    }
	    if(args[i].equals("-suddendeath")) {
		suddenDeath = true;
		continue;
	    }	    	    
	    if(args[i].equals("-sleep")) {
		sleep = Integer.parseInt(args[++i]);
		continue;
	    }	    	    
	    if(args[i].equals("-skip")) {
		skip = Integer.parseInt(args[++i]);
		continue;
	    }	    	    

	    File f = new File(args[i]);

	    if(f.exists()) {
		String contents = IO.readContents(args[i]);
		Pattern pattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(contents);
		contents = matcher.replaceAll("");
		urls.addAll(Utils.split(contents,"\n",true,true));
		continue;
	    }
	    urls.add(args[i]);
	}
	Random random = new Random();
	System.out.println("num threads:" + numThreads);
	int threads = numThreads;
	for(int i=0;i<threads;i++) {
	    new Test(urls);
	    //Stagger the threads
	    Misc.sleep(random.nextInt(50) + 1);
	}
	
	while(true) {
	    synchronized(MUTEX) {
		if(numThreads<=0) break;
	    }
	    Misc.sleep(500);
	}
	System.out.println("Finished");
	System.exit(0);
    }


}

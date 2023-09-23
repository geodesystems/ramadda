/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util;
import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import java.net.URL;
import ucar.unidata.util.Misc;

public class Test {
    private static Date startTime;
    private static Object MUTEX=new Object();
    private static int numThreads = 50;
    private static int totalRead =0;
    private static int loops = 1000;
    private static int sleep=0;

    public static void runTest(List<String> urls) {
	try {
	    int cnt=0;
	    while(cnt++<loops) {
		int urlCnt=0;
		for(String url:urls) {
		    url = url.trim();
		    if(url.startsWith("#")) continue;
		    if(url.startsWith("stop")) break;
		    if(url.startsWith("sleep ")) {
			int s = Integer.parseInt(url.substring("sleep ".length()).trim());
			Misc.sleep(s);
			continue;
		    }
		    
		    Date before = new Date();
		    IO.Result result = IO.doGetResult(new URL(url));
		    if(result.getError()) {
			System.err.println("read error:" + result.getResult());
			return;
		    }
		    synchronized(MUTEX) {
			totalRead++;
			Date after = new Date();
			long time = after.getTime()-before.getTime();
			if(time>1000) {
			    System.err.println("#" + urlCnt +" long time:" + (time) +" url:" +url);
			}
			long diff = (after.getTime()-startTime.getTime())/1000;
			int callsPer = diff<=0?0:(int)(totalRead/(double)diff);
			System.out.println("#" + (urlCnt++)+" total read:" + totalRead + " calls/s:" + callsPer +" time:" + time);

		    }
		    if(sleep>0) Misc.sleep(sleep);
		}
	    }
	} catch(Exception exc) {
	    System.err.println("error:" + exc);
	    return;
	}
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
		System.err.println("usage: -threads <# threads> -loops <#loops> -sleep <pause after each call (ms)> <file> or <url>");
		System.exit(0);
	    }

	    if(args[i].equals("-threads")) {
		numThreads = Integer.parseInt(args[++i]);
		continue;
	    }
	    if(args[i].equals("-loops")) {
		loops = Integer.parseInt(args[++i]);
		continue;
	    }
	    if(args[i].equals("-sleep")) {
		sleep = Integer.parseInt(args[++i]);
		continue;
	    }	    	    
	    File f = new File(args[i]);

	    if(f.exists()) {
		urls.addAll(Utils.split(IO.readContents(args[i]),"\n",true,true));
		continue;
	    }
	    urls.add(args[i]);
	}
	System.err.println("num threads:" + numThreads);
	int threads = numThreads;
	for(int i=0;i<threads;i++) {
	    Misc.runInABit(1000,new Runnable() {
		    public void run() {
			runTest(urls);
			synchronized(MUTEX) {
			    numThreads--;
			    System.err.println("DONE:" + numThreads);
			}
		    }
		});
	}
	
	while(true) {
	    synchronized(MUTEX) {
		if(numThreads<=0) break;
	    }
	    Misc.sleep(500);
	}
	System.err.println("Finished");
	System.exit(0);
    }


}

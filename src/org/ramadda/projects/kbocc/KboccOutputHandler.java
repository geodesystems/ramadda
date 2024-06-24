/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.kbocc;


import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.HtmlUtils;

import org.w3c.dom.*;

import org.ramadda.util.seesv.Seesv;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;



import java.io.*;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KboccOutputHandler extends OutputHandler {


    public static final OutputType OUTPUT_KBOCC_MERGE =
        new OutputType("Merge KBOCC Files", "kboccmerge", -1, "", "/kbocc/kbocclg.png");

    public KboccOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_KBOCC_MERGE);
    }



    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
	//links.add(makeLink(request, state.getEntry(), OUTPUT_GPX));
    }


    public Result outputEntry(Request request, OutputType outputType,   Entry entry)
            throws Exception {
	return null;
    }

    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {


	if(children.size()==0) {
	    for(Object id: request.get(ARG_ENTRYID,new ArrayList<String>())) {
		Entry entry=getEntryManager().getEntry(request,id.toString());
		if(entry!=null) children.add(entry);
	    }
	}
	if(children.size()==0) {
	    StringBuilder sb = new StringBuilder();
	    getPageHandler().sectionOpen(request,sb,"Merge KBOCC Files",false);
	    sb.append(getPageHandler().showDialogWarning("No entries selected"));
	    getPageHandler().sectionClose(request,sb);
	    return new Result("KBOCC Merge",sb);
	}

	File dir =getStorageManager().createProcessDir();
	String [] cmds1= {"-delimiter", "?", "-skiplines", "1", "-set", "0", "0", "number", "-set", "1", "0",
	    "Date Time", "-set", "2", "0", "Temperature", "-notcolumns", "0,3-10",
	    "-indateformats", "MM/dd/yy hh:mm:ss a;MM/dd/yyyy HH:mm", "GMT-4",
	    "-outdateformat", "iso8601", "GMT", "-convertdate", "date_time",
	    "-outdateformat", "yyyy-MM-dd HH:mm Z", "UTC", "-indateformat", "iso8601",
	    "GMT", "-extractdate", "date_time", "year", "-extractdate", "date_time", "hours_in_year",
	    "-notcolumns", "date_time", "-lastcolumns", "0" ,"-print"};


	List<File> files = new ArrayList<File>();
	String site="";
	for(int i=0;i<children.size();i++) {
	    Entry child = children.get(i);
	    if(i==0) site = child.getStringValue(request,"location","site");
	    File   tmpFile = new File(IOUtil.joinDir(dir, "file" + i+".csv"));
	    files.add(tmpFile);
	    FileOutputStream fos = new FileOutputStream(tmpFile);
	    Seesv seesv = new Seesv(cmds1,
				    new BufferedOutputStream(fos), null);
	    InputStream inputStream =getStorageManager().getFileInputStream(child.getResource().getTheFile());
	    seesv.setInputStream(inputStream);
	    seesv.run(null);
	    inputStream.close();
	}


	File   allFile = new File(IOUtil.joinDir(dir, "all.csv"));
	PrintWriter writer =    new PrintWriter(new FileOutputStream(allFile));
	writer.println("Year,Hours in year,Temperature");
	for(File f:files) {
	    int cnt=0;
	    FileInputStream fis = new FileInputStream(f);
	    BufferedReader reader =   new BufferedReader(new InputStreamReader(fis));
	    String line;
	    while((line=reader.readLine())!=null) {
		cnt++;
		if(cnt==1) continue;
		writer.println(line);
	    }
	    fis.close();
 	}
	writer.close();

	String[] cmds2 = {"-makefields", "year","temperature","hours_in_year", "",
			  "-sortby","hours_in_year", "up","",
			  "-formatdateoffset","hours_in_year","hours_in_year","-firstcolumns","month_day_hour","-print"};


	File   mergedFile = new File(IOUtil.joinDir(dir, "merged.csv"));
	FileOutputStream fos = new FileOutputStream(mergedFile);
	Seesv seesv = new Seesv(cmds2,new BufferedOutputStream(fos), null);
	InputStream inputStream =getStorageManager().getFileInputStream(allFile);
	seesv.setInputStream(inputStream);
	seesv.run(null);
	fos.close();
	inputStream.close();
	request.setReturnFilename(site+"_merged.csv");
	return new Result(new FileInputStream(mergedFile),"text/csv");
    }



}

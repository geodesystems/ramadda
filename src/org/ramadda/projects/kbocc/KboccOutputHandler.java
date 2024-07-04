/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.projects.kbocc;
import org.ramadda.repository.*;
import org.ramadda.repository.output.*;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.seesv.SeesvException;
import ucar.unidata.util.IOUtil;
import org.w3c.dom.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class KboccOutputHandler extends OutputHandler {

    public static final OutputType OUTPUT_KBOCC_MERGESITE =
        new OutputType("Merge KBOCC Files", "kboccmergesite", -1, "", "/kbocc/kbocclg.png");
    public static final OutputType OUTPUT_KBOCC_MAKEALL =
        new OutputType("Merge KBOCC Files", "kboccmakeall", -1, "", "/kbocc/kbocclg.png");    

    public KboccOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_KBOCC_MERGESITE);
        addType(OUTPUT_KBOCC_MAKEALL);	
    }

    private String getSite(Request request,List<Entry> children) {
	String site="";
	for(int i=0;i<children.size();i++) {
	    Entry child = children.get(i);
	    site = child.getStringValue(request,"site","site");
	    if(stringDefined(site)) return site;
	}
	return "kboccsite";
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
	    getPageHandler().sectionOpen(request,sb,"KBOCC Process",false);
	    sb.append(getPageHandler().showDialogWarning("No entries selected"));
	    getPageHandler().sectionClose(request,sb);
	    return new Result("KBOCC Process",sb);
	}

	//Process the makeall
	File dir =getStorageManager().createProcessDir();
	if(outputType.equals(OUTPUT_KBOCC_MERGESITE)) return processMergeSite(request, group, dir,children);
	//These are the commands from kbocctypes.xml
	List <File> processedFiles = new ArrayList<File>();
	String year=null;
	for(Entry child: children) {
	    if(year==null)
		year=child.getStringValue(request,"datayear",null);
	    String[]cmds1={"-delimiter","?","-skiplines","1","-set","0","0","number","-set","1",
			   "0","Date Time","-set","2","0","Temperature","-notcolumns","0,3-10",
			   "-indateformats","MM/dd/yy hh:mm:ss a;MM/dd/yyyy HH:mm",   "GMT-4",
			   "-outdateformat","iso8601","GMT","-convertdate","date_time","-indateformat","iso8601","GMT","-extractdate",
			   "date_time","hours_in_year","-add",
			   "latitude,longitude",   child.getLatitude(request)+","+child.getLongitude(request),
			   //"-addheader", "date_time.type date date_time.format iso8601 temperature.unit F",
			   "-print"
	    };
	    File file = child.getResource().getTheFile();
	    List<File> tmp = new ArrayList<File>();
	    tmp.add(file);
	    tmp = Seesv.applySeesv(dir,cmds1,tmp);
	    processedFiles.add(tmp.get(0));
	}

	String [] cmds2= {"-outdateformat","yyyy-MM-dd", "GMT","-formatdate","date_time","-extractdate", "date_time","days_in_year",
			 "-unique","days_in_year","exact","-columns", "date_time,temperature,latitude,longitude","-print"};
	List<File> files2 = Seesv.applySeesv(dir,cmds2,processedFiles);
	String filename = year!=null?"kbocc_map_" + year+".csv":"kbocc_map.csv";
	File   finalFile = new File(IOUtil.joinDir(dir, filename));
	PrintWriter writer =    new PrintWriter(new FileOutputStream(finalFile));
	for(int i=0;i<files2.size();i++) {
	    File f= files2.get(i);
	    int cnt=0;
	    FileInputStream fis = new FileInputStream(f);
	    BufferedReader reader =   new BufferedReader(new InputStreamReader(fis));
	    String line;
	    while((line=reader.readLine())!=null) {
		cnt++;
		if(i>0 && cnt==1) continue;
		writer.println(line);
	    }
	    fis.close();
 	}
	writer.close();
	request.setReturnFilename(filename);
	return new Result(new FileInputStream(finalFile),"text/csv");
    }

    public Result processMergeSite(Request request,  Entry group, File dir, List<Entry> children)
            throws Exception {

	String [] cmds1= {"-delimiter", "?", "-skiplines", "1",
			  "-set", "0", "0", "number",
			  "-set", "1", "0",   "Date Time",
			  "-set", "2", "0", "Temperature",
			  "-notcolumns", "0,3-10",
			  "-indateformats", "MM/dd/yy hh:mm:ss a;MM/dd/yyyy HH:mm", "GMT-4",
			  "-outdateformat", "iso8601", "GMT",
			  "-convertdate", "date_time",
			  "-outdateformat", "yyyy-MM-dd HH:mm Z", "UTC",
			  "-indateformat", "iso8601",  "GMT",
			  "-extractdate", "date_time", "year",
			  "-extractdate", "date_time", "hours_in_year",
			  "-notcolumns", "date_time",
			  "-lastcolumns", "0" ,
			  "-print"};


	
	List<File> files = new ArrayList<File>();
	for(Entry child: children) {
	    File file = child.getResource().getTheFile();
	    if(!file.exists()) continue;
	    try {
		File processedFile =  Seesv.applySeesv(dir,cmds1,file);
		files.add(processedFile);
	    } catch(Exception exc) {
		String msg = "Error processing KBOCC merge:"  + child.getName() + " " + child.getId() +" " +
		    exc.getMessage();
		getLogManager().logError(msg,exc);
		throw new SeesvException(msg);
	    }
	}


	String site= getSite(request,children);

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

	List<File> allFiles = new ArrayList<File>();
	allFiles.add(allFile);
	List<File> finalFiles = Seesv.applySeesv(dir,cmds2,allFiles);
	File   mergedFile = finalFiles.get(0);
	request.setReturnFilename("kbocc_" + Utils.makeID(site)+"_merged.csv");
	return new Result(new FileInputStream(mergedFile),"text/csv");
    }
}

/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.output;


import org.ramadda.repository.*;
import org.ramadda.repository.auth.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.data.services.PointOutputHandler;
import org.ramadda.data.services.PointTypeHandler;

import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.sql.Clause;

import org.ramadda.util.sql.SqlUtil;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

import java.io.File;

import java.net.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import java.util.regex.*;

import java.util.zip.*;

/**
 *
 *
 *
 * @author RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class CsvOutputHandler extends OutputHandler {

    public static final String ARG_WHAT = "what";
    public static final String WHAT_IDS = "ids";
    public static final String WHAT_WGET = "wget";
    public static final String WHAT_XLSX = "xlsx";    
    public static final String WHAT_CSVAPI = "csvapi";
    public static final String WHAT_WRAPPER_R = "wrapper_r";
    public static final String WHAT_WRAPPER_PYTHON= "wrapper_python";
    public static final String WHAT_WRAPPER_MATLAB= "wrapper_matlab";        

    public static final OutputType OUTPUT_CSV = new OutputType("CSV Listing",
                                                    "default.csv",
                                                    OutputType.TYPE_FEEDS|
							       OutputType.TYPE_FORSEARCH,
                                                    "", ICON_CSV);
    public static final OutputType OUTPUT_IDS = new OutputType("IDS",
                                                    "default.ids",
							       OutputType.TYPE_FORSEARCH,
							       "", ICON_CSV);    

    public static final OutputType OUTPUT_XLSX = new OutputType("XLSX Export",
                                                    "xlsx",
                                                    OutputType.TYPE_OTHER|    OutputType.TYPE_FORSEARCH,
                                                    "", ICON_CSV);




    public static final OutputType OUTPUT_ENTRYCSV =
        new OutputType("Entry CSV", "entry.csv", OutputType.TYPE_FEEDS, "",
                       ICON_CSV);

    public static final OutputType OUTPUT_WRAPPER_MATLAB = new OutputType("Matlab Wrapper",
									  "wrapper_matlab",
									  OutputType.TYPE_OTHER,
									  "", "/icons/matlab.png");
    public static final OutputType OUTPUT_WRAPPER_R = new OutputType("R Wrapper",
									  "wrapper_r",
									  OutputType.TYPE_OTHER,
									  "", "/icons/r.png");    

    public static final OutputType OUTPUT_WRAPPER_PYTHON= new OutputType("Python Wrapper",
									  "wrapper_python",
									  OutputType.TYPE_OTHER,
									  "", "/icons/python.png");

    public CsvOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CSV);
        addType(OUTPUT_XLSX);
        addType(OUTPUT_IDS);
        addType(OUTPUT_ENTRYCSV);
	addType(OUTPUT_WRAPPER_MATLAB);
	addType(OUTPUT_WRAPPER_R);
	addType(OUTPUT_WRAPPER_PYTHON);		
    }

    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_CSV));
            links.add(makeLink(request, state.getEntry(), OUTPUT_XLSX));	    
            links.add(makeLink(request, state.getEntry(), OUTPUT_ENTRYCSV));
	    if(state.getEntry().getTypeHandler().isType("type_point")||
	       state.getEntry().getTypeHandler() instanceof PointTypeHandler) {
		links.add(makeLink(request, state.getEntry(), OUTPUT_WRAPPER_MATLAB));
		links.add(makeLink(request, state.getEntry(), OUTPUT_WRAPPER_R));
		links.add(makeLink(request, state.getEntry(), OUTPUT_WRAPPER_PYTHON));
	    }
	}
    }

    public static final String ARG_FIELDS = "fields";

    public static final String ARG_DELIMITER = "delimiter";

    public static final String ARG_FIXEDWIDTH = "fixedwidth";

    public static final String ARG_FULLHEADER = "fullheader";

    private Result makeStream(Request request, InputStream is) throws Exception {
	return request.returnStream("entries.csv",  getMimeType(OUTPUT_CSV),is);	    
    }

    public Result listIds(Request request, Entry group,
			  List<Entry> entries) throws Exception {
	String what = request.getString(ARG_WHAT,"ids");
	StringBuilder sb = new StringBuilder();
	String mime = "text/csv";
	if(what.equals(WHAT_IDS)) {
	    request.setReturnFilename("ids.csv",false);
	    sb.append("id\n");
	} else 	if(what.equals(WHAT_WGET)) {
	    sb.append("#This was generated by RAMADDA to download the given entry files\n");
	    sb.append("WGET=wget\n");
	    request.setReturnFilename("wget.sh",false);
	    mime = "application/x-sh";
	} else 	if(what.equals(WHAT_CSVAPI)) {
	    sb.append("#This was generated by RAMADDA to download the given entry files\n");
	    sb.append("WGET=wget\n");
	    request.setReturnFilename("csvget.sh",false);	    
	    mime = "application/x-sh";
	} else if(what.equals(WHAT_WRAPPER_R)) {
	    mime = "text/x-r";
	    if (group.isDummy()) {
		request.setReturnFilename("ramadda_download.r",false);
	    } else {
		request.setReturnFilename(Utils.makeID(group.getName())+"_download.r",false);
	    }
	} else if(what.equals(WHAT_WRAPPER_PYTHON)) {
	    mime = "text/x-python";
	    if (group.isDummy()) {
		request.setReturnFilename("ramadda_download.py",false);
	    } else {
		request.setReturnFilename(Utils.makeID(group.getName())+"_download.py",false);
	    }
	} else if(what.equals(WHAT_WRAPPER_MATLAB)) {
	    mime = "text/x-matlab";
	    if (group.isDummy()) {
		request.setReturnFilename("ramadda_download.m",false);
	    } else {
		request.setReturnFilename(Utils.makeID(group.getName())+"_download.m",false);
	    }	    
	}
	HashSet<String> seen = new HashSet<String>();
	int urlCnt=0;
	boolean isMatlab =      what.equals(WHAT_WRAPPER_MATLAB);
	boolean isR =      what.equals(WHAT_WRAPPER_R);
	boolean isPython =      what.equals(WHAT_WRAPPER_PYTHON);		

	for(Entry entry: entries) {
	    if(what.equals(WHAT_IDS)) {
		sb.append(entry.getId());
		sb.append("\n");
	    } else  if(what.equals(WHAT_WGET) ||
		       isMatlab || isR || isPython) {
		if(what.equals(WHAT_WGET)) {
		    if(!entry.isFile()) {
			sb.append("#entry: " + entry.getName() +" is not a file\n");
			continue;
		    }
		} else {
		    if(!entry.getTypeHandler().isType("type_point")) {
			String comment =isMatlab?"%":"#";
			sb.append(comment+"entry: " + entry.getName() +" is not point data\n");
			continue;
		    }
		}
		String url;
		String file;
		if(what.equals(WHAT_WGET)) {
		    url = getEntryManager().getEntryResourceUrl(request,  entry,ARG_INLINE_DFLT,true,true);
		    file = getStorageManager().getFileTail(entry);
		} else {
		    PointOutputHandler poh = getRepository().getPointOutputHandler();
		    url =  poh.getCsvApiUrl(request, entry);
		    file = Utils.makeID(entry.getName())+".csv";
		    if(what.equals(WHAT_WRAPPER_R)) {
			if(urlCnt>0) {
			    sb.append(",\n");
			}
			urlCnt++;
			url = request.getAbsoluteUrl(url);
			sb.append("list(url = \"" + url +"\", name = \"" + file+"\")");
			continue;

		    } else   if(what.equals(WHAT_WRAPPER_PYTHON)) {
			//    {"url": "https://example.com/file1.csv", "filename": "file1.csv"},
			//    {"url": "https://example.com/file2.csv", "filename": "file2.csv"}
			if(urlCnt>0) {
			    sb.append(",\n");
			}
			urlCnt++;
			url = request.getAbsoluteUrl(url);
			sb.append("{\"url\": \"" + url +"\", \"filename\": \"" + file+"\"}");
			continue;
		    } else   if(what.equals(WHAT_WRAPPER_MATLAB)) {			
			//    'https://example.com/data2.csv', 'data2.csv'; ...
			//    'https://example.com/data3.csv', 'data3.csv' ...
			urlCnt++;
			url = request.getAbsoluteUrl(url);
			sb.append(HU.squote(url) +"," +HU.squote(file)+";");
			sb.append(" ...");
			continue;
		    }
		}
		int cnt=0;
		file = file.trim();
		String tmp=file;
		while(seen.contains(tmp)) {
		    cnt++;
		    tmp = cnt+"_" + file;
		}
		file = tmp;
		seen.add(file);

		sb.append("echo ");
		sb.append(HU.quote("downloading " + file));
		sb.append("\n");
		sb.append("${WGET}  -q -nv --no-check-certificate -O ");
		sb.append(HU.quote(file));
		sb.append(" ");
		sb.append(HU.quote(request.getAbsoluteUrl(url)));
		sb.append("\n");
	    }

	}
	if(what.equals(WHAT_WRAPPER_R)) {
	    String template =
		getStorageManager().readUncheckedSystemResource("/org/ramadda/repository/resources/code/template.r");
	    template = template.replace("${urls}",sb.toString());
	    sb  = new StringBuilder(template);
	} else if(what.equals(WHAT_WRAPPER_PYTHON)) {
	    String template =
		getStorageManager().readUncheckedSystemResource("/org/ramadda/repository/resources/code/template.py");
	    template = template.replace("${urls}",sb.toString());
	    sb  = new StringBuilder(template);
	} else if(what.equals(WHAT_WRAPPER_MATLAB)) {
	    String template =
		getStorageManager().readUncheckedSystemResource("/org/ramadda/repository/resources/code/template.m");
	    template = template.replace("${urls}",sb.toString());
	    sb  = new StringBuilder(template);	    
	}

        return new Result("", sb, mime);

    }



    public Result listEntries(Request request, List<Entry> entries)
            throws Exception {
	InputStream is =IO.pipeIt(new IO.PipedThing(){
		public void run(OutputStream os) {
		    PrintStream           pw  = new PrintStream(os);
		    try {
			listEntries(request, pw,entries);
		    } catch(Exception exc) {
			getLogManager().logError("Making CSV",exc);
			pw.println("Making JSON:" + exc);
		    }
		}});
	return  makeStream(request, is);
    }

    public void listEntries(Request request, Appendable sb,List<Entry> entries)
	throws Exception {	

        String  delimiter      = request.getString(ARG_DELIMITER, ",");
        boolean fixedWidth     = request.get(ARG_FIXEDWIDTH, false);
        boolean showFullHeader = request.get(ARG_FULLHEADER, false);
        boolean showHeader     = request.get("showheader", true);
        boolean escape = request.get("escape", false) || showFullHeader;
        String filler = request.getString("filler", " ");

        String fieldsArg =
            request.getString(
                ARG_FIELDS,
                "name,id,type,description,startdate,enddate,entry_url,north,south,east,west,url,fields");

        StringBuffer header      = new StringBuffer();
        List<String> toks        = Utils.split(fieldsArg, ",", true, true);
        List<String> fieldNames  = new ArrayList<String>();
        List<String> fieldLabels = new ArrayList<String>();
        for (int i = 0; i < toks.size(); i++) {
            String       tok   = toks.get(i);
            String       field = tok;
            String       label = tok;
            List<String> pair  = Utils.splitUpTo(tok, ";", 2);
            if (pair.size() > 1) {
                field = pair.get(0);
                label = pair.get(1);
            }
            fieldNames.add(field);
            fieldLabels.add(label);
            if (header.length() > 0) {
                header.append(",");
            }
            String type = "string";
            if (field.equals("type")) {
                type = "enumeration";
            } else if (field.equals("icon")) {
                type = "image";
            } else if (field.equals("startdate")) {
                type = "date";
            } else if (field.equals("enddate")) {
                type = "date";				
            } else if (field.equals("entry_url")) {
                type = "url";
            } else if (field.equals("url")) {
                type = "url";
            } else if (field.equals("latitude")) {
                type = "double";
            } else if (field.equals("longitude")) {
                type = "double";
            } else if (field.equals("north")) {
                type = "double";
            } else if (field.equals("south")) {
                type = "double";
            } else if (field.equals("east")) {
                type = "double";
            } else if (field.equals("west")) {
                type = "double";
            } else if (field.equals("description")) {}
            else if (field.equals("size")) {
                type = "integer";
            }
            if (showHeader) {
		addHeader(header, field, label, type, escape,
			  showFullHeader);
            }
        }

        int[] maxStringSize = null;
        for (Entry entry : entries) {
            List<Column> columns = entry.getTypeHandler().getColumns();
            if (columns == null) {
                continue;
            }
            if ((maxStringSize == null)
                    || (maxStringSize.length < columns.size())) {
                maxStringSize = new int[columns.size()];
                for (int i = 0; i < maxStringSize.length; i++) {
                    maxStringSize[i] = 0;
                }
            }
            for (int col = 0; col < columns.size(); col++) {
                Column column = columns.get(col);
                if ( !column.getCanExport()) {
                    continue;
                }
                if (column.isString()) {
                    String s = sanitize(escape,
                                        (String)entry.getValue(request,column));
		    if(s!=null)
			maxStringSize[col] = Math.max(maxStringSize[col],
						      s.length());
                }
            }
        }

        if (maxStringSize != null) {
            //            for (int i = 0; i < maxStringSize.length; i++) {
            //                System.err.println("i:" + i + " " + maxStringSize[i]);
            //            }
        }

        Hashtable<String, Column> columnMap = null;

	int entryCnt=0;
        for (Entry entry : entries) {
	    entryCnt++;
            if (entryCnt==1) {
		String headerString =header.toString();
                if (fieldNames.contains("fields")) {
                    List<Column> columns =
                        entry.getTypeHandler().getColumns();

                    if (columns != null) {
                        String tmp = null;
                        int    cnt = 0;
                        for (int col = 0; col < columns.size(); col++) {
                            Column column = columns.get(col);
                            if ( !column.getCanExport()) {
                                continue;
                            }
                            if (tmp == null) {
                                tmp = ",";
                            } else {
                                tmp += ",";
                            }
                            tmp += column.getName();
                            if (fixedWidth) {
                                tmp += ((maxStringSize[col] > 0)
                                        ? "(max:" + maxStringSize[col] + ")"
                                        : "");
                            }

                        }
                        if (tmp == null) {
                            tmp = "";
                        }
                        headerString = headerString.replace(",fields", tmp);
                    }
                }
                if (showFullHeader) {
                    sb.append("#fields=");
                }
                sb.append(headerString);
                sb.append("\n");
            }

            int      colCnt = 0;
            for (String field : fieldNames) {
                if (colCnt != 0) {
                    sb.append(delimiter);
                }
                colCnt++;
                if (field.equals("name")) {
                    sb.append(sanitize(escape, entry.getName()));
                } else if (field.equals("startdate")) {
                    sb.append(getDateHandler().formatDate(request, entry,entry.getStartDate()));
                } else if (field.equals("enddate")) {
                    sb.append(getDateHandler().formatDate(request, entry,entry.getEndDate()));
                } else if (field.equals("fullname")) {
                    sb.append(sanitize(escape, entry.getFullName()));
                } else if (field.equals("type")) {
                    sb.append(entry.getTypeHandler().getType());
                } else if (field.equals("icon")) {
                    sb.append(getPageHandler().getIconUrl(request, entry));
                } else if (field.equals("id")) {
                    sb.append(entry.getId());
                } else if (field.equals("entry_url")) {
                    String url = request.makeUrl(repository.URL_ENTRY_SHOW,
                                     ARG_ENTRYID, entry.getId());
                    url = HtmlUtils.urlEncodeSpace(url);
                    url = request.getAbsoluteUrl(url);
                    sb.append(url);
                } else if (field.equals("url")) {
                    if (entry.getResource().isUrl()) {
                        sb.append(
                            entry.getTypeHandler().getPathForEntry(
								   request, entry,false));
                    } else if (entry.getResource().isFile()) {
                        String url =
                            entry.getTypeHandler().getEntryResourceUrl(
                                request, entry);
                        url = HtmlUtils.urlEncodeSpace(url);
                        url = request.getAbsoluteUrl(url);
                        sb.append(url);
                    } else {}
                } else if (field.equals("latitude")) {
                    sb.append(""+entry.getLatitude(request));
                } else if (field.equals("longitude")) {
                    sb.append(""+entry.getLongitude(request));
                } else if (field.equals("north")) {
                    sb.append(""+entry.getNorth(request));
                } else if (field.equals("south")) {
                    sb.append(""+entry.getSouth(request));
                } else if (field.equals("east")) {
                    sb.append(""+entry.getEast(request));
                } else if (field.equals("west")) {
                    sb.append(""+entry.getWest(request));
                } else if (field.equals("description")) {
                    sb.append(sanitize(escape, entry.getDescription()));
                } else if (field.equals("size")) {
                    sb.append(""+entry.getResource().getFileSize());
                } else if (field.equals("fields")) {
                    List<Column> columns =
                        entry.getTypeHandler().getColumns();
                    if (columns != null) {
                        int cnt = 0;
                        for (int col = 0; col < columns.size(); col++) {
                            Column column = columns.get(col);
                            if ( !column.getCanExport()) {
                                continue;
                            }
                            if (cnt > 0) {
                                sb.append(delimiter);
                            }
                            String s = sanitize(escape,entry.getStringValue(request, column,null));
                            sb.append(s);
                            if (fixedWidth) {
                                if (column.isString()) {
                                    int length = s.length();
                                    while (length < maxStringSize[col]) {
                                        sb.append(filler);
                                        length++;
                                    }
                                }
                            }
                            cnt++;
                        }
                    }
                } else {
                    if (columnMap == null) {
                        columnMap = new Hashtable<String, Column>();
                        List<Column> columns =
                            entry.getTypeHandler().getColumns();
                        if (columns != null) {
                            for (int col = 0; col < columns.size(); col++) {
                                Column column = columns.get(col);
                                if ( !column.getCanExport()) {
                                    continue;
                                }
                                columnMap.put(column.getName(), column);
                            }
                        }
                    }
                    Column column = columnMap.get(field);
                    if (column != null) {
                        String s = sanitize(escape,entry.getStringValue(request,column,""));
                        sb.append(s);
                        if (fixedWidth) {
                            if (column.isString()) {
                                int length = s.length();
                                while (length
                                        < maxStringSize[column.getColumnIndex()]) {
                                    sb.append(filler);
                                    length++;
                                }
                            }
                        }
                    } else {
                        sb.append("unknown:" + field);
                    }
                }
            }
            sb.append("\n");
        }

    }

    /**
     *
     * @param sb _more_
     * @param s _more_
     * @param label _more_
     * @param type _more_
     * @param escape _more_
     * @param full _more_
     *
     * @throws Exception _more_
     */
    private void addHeader(Appendable sb, String s, String label,
                           String type, boolean escape, boolean full)
            throws Exception {
        sb.append(sanitize(escape, s));
        if (full) {
            sb.append("[");
            sb.append(" type=\"" + ((type != null)
                                    ? type
                                    : "string") + "\" ");
            if (label != null) {
                sb.append(" label=\"" + sanitize(true, label) + "\" ");
            }
            sb.append("]");
        }
    }

    public String sanitize(boolean escape, String s) {
        if (s == null) {
            return "";
        }
	if(!escape) {
	    return Seesv.cleanColumnValue(s);
	}
        s = s.replaceAll("\r\n", " ");
        s = s.replaceAll("\r", " ");
        s = s.replaceAll("\n", " ");
        //quote the columns that have commas in them
        if (s.indexOf(",") >= 0) {
            if (escape) {
                s = s.replaceAll(",", "_comma_");
            } else {
                //Not sure how to escape the quotes
                s = s.replaceAll("\"", "'");
                //wrap in a quote
                s = "\"" + s + "\"";
            }
        }

        return s;
    }

    public Result listTypes(Request request, List<TypeHandler> typeHandlers)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        for (TypeHandler theTypeHandler : typeHandlers) {
            sb.append(SqlUtil.comma(theTypeHandler.getType(),
                                    theTypeHandler.getDescription()));
            sb.append("\n");
        }

        return new Result("", sb, getMimeType(OUTPUT_CSV));
    }

    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_CSV)) {
            return repository.getMimeTypeFromSuffix(".csv");
        }
        if (output.equals(OUTPUT_XLSX)) {
            return repository.getMimeTypeFromSuffix(".xlsx");
        }	

        return super.getMimeType(output);
    }

    public Result listXlsx(final Request request, final List<Entry> entries)
            throws Exception {
	InputStream is =IO.pipeIt(new IO.PipedThing(){
		public void run(OutputStream os) {
		    try {
			listXlsx(request, os,entries);
		    } catch(Exception exc) {
			getLogManager().logError("Making XLSX",exc);
		    }
		}});
	return request.returnStream("entries.xlsx",  "application/excel",is);	    
    }

    public void listXlsx(final Request request, OutputStream outputStream,final List<Entry> allEntries)
            throws Exception {
	String NA = "";
	Hashtable props = new Hashtable();

        boolean showCategories = request.get(ARG_SHOWCATEGORIES,
                                             Utils.getProperty(props,
                                                 ARG_SHOWCATEGORIES, true));
        boolean showAllTypes = request.get("showAllTypes",
                                             Utils.getProperty(props,
                                                 "showAllTypes", false));

	if(showAllTypes) showCategories = true;

        Hashtable<String, List<Entry>> map = new Hashtable<String,
                                                 List<Entry>>();
        List<String> displayColumns = null;
	String tmpColumns = Utils.getProperty(props, "columns", null);
	if(tmpColumns!=null) {
	    displayColumns = new ArrayList<String>();
	    for(String col: Utils.split(tmpColumns,",",true,true)) {
		if(!col.startsWith("#")) displayColumns.add(col);

	    }
	}
        boolean showColumns = Utils.getProperty(props, "showColumns", true);
        boolean showDate = Utils.getProperty(props, "showDate", true);
        boolean showCreateDate = Utils.getProperty(props, "showCreateDate", false);
        boolean showChangeDate = Utils.getProperty(props, "showChangeDate", true);
        boolean showFromDate = Utils.getProperty(props, "showFromDate", false);
        boolean showToDate = Utils.getProperty(props, "showToDate", false);		
        boolean showEntryDetails = Utils.getProperty(props, "showEntryDetails",  true);	
        List<String> types = new ArrayList<String>();
        for (Entry entry : allEntries) {
            TypeHandler  typeHandler = entry.getTypeHandler();
            String       type        = typeHandler.getType();
            List<Column> columns     = typeHandler.getColumns();
            boolean      hasFields   = false;
            if (columns != null) {
                for (Column column : columns) {
                    if (column.getCanList() && column.getCanShow()
                            && (column.getRows() <= 1)) {
                        hasFields = true;
                    }
                }
            }
            if ( !hasFields) {
                if (typeHandler.isGroup()) {
                    type = "Folders";
                } else if (entry.isFile()) {
                    type = "Files";
                }
            }

            if ( !showCategories) {
                type = "entries";
            }

	    if(showAllTypes) type  =entry.getTypeHandler().getType();
            List<Entry> entries = map.get(type);
            if (entries == null) {
                entries = new ArrayList<Entry>();
                map.put(type, entries);
                types.add(type);
            }
            entries.add(entry);
        }
        int          typeCnt  = 0;
	Workbook workbook = new XSSFWorkbook(); 
	CreationHelper creationHelper = workbook.getCreationHelper();
	CellStyle dateStyle = workbook.createCellStyle();

	dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        for (String type : types) {
            List<Entry>  entries     = map.get(type);
            TypeHandler  typeHandler = entries.get(0).getTypeHandler();
	    Sheet sheet = workbook.createSheet(typeHandler.getLabel());


	    int rowCnt=0;
	    final Row[] row = {null};
	    final int[] colCnt={0};

    
	    Consumer<String> add = (value)->{
		Cell cell = row[0].createCell(colCnt[0]++);
		cell.setCellValue(value);
	    };


	    BiConsumer<Entry,Long> fmt = (entry,date)->{
		Date d =new Date(date);
		Cell cell = row[0].createCell(colCnt[0]++);
		if(getDateHandler().isNullDate(d)) {
		    cell.setCellValue("");
		} else  {
		    cell.setCellValue(d);
		    cell.setCellStyle(dateStyle);
		}
	    };

	    List<Column> columns     = typeHandler.getColumns();
	    List<MetadataType> showMetadata = null;
	    String tmp = Utils.getProperty(props,"showMetadata",null);
	    if(tmp!=null) {
		showMetadata = new ArrayList<MetadataType>();
		for(String mtdType:Utils.split(tmp,",",true,true)) {
		    MetadataType mtd = getMetadataManager().findType(mtdType);
		    if(mtd!=null) showMetadata.add(mtd);
		}
	    }
            boolean haveFiles = false;
	    List<String> headers = new ArrayList<String>();
	    if(displayColumns!=null) {
		for(String col: displayColumns) {
		    String label=Utils.getProperty(props,col+"Label",null);
		    if(label==null) {
			label = typeHandler.getFieldLabel(col);
		    }
		    headers.add(label);
		}
	    } else {
		headers.add("Name");
		headers.add("Link");
		if (showDate) {
		    headers.add(Utils.getProperty(props,"dateLabel","Date"));
		}
		if (showCreateDate) {
		    headers.add(Utils.getProperty(props,"createDateLabel","Create Date"));
		}
		if (showChangeDate) {
		    headers.add(Utils.getProperty(props,"changeDateLabel","Change Date"));
		}
		if (showFromDate) {
		    headers.add(Utils.getProperty(props,"fromDateLabel","From Date"));
		}
		if (showToDate) {
		    headers.add(Utils.getProperty(props,"toDateLabel","To Date"));
		}
	    }
	    if(displayColumns==null) {
		for (Entry entry : entries) {
		    if (entry.isFile()) {
			haveFiles = true;
			break;
		    }
		}
		if (haveFiles) {
		    headers.add("File Size");
		    headers.add("File Download");
		}

		if (columns != null) {
		    for (Column column : columns) {
			if ((column.getRows() <= 1) && column.getCanShow()) {
			    if (column.getCanList() && 
				Utils.getProperty(props,
						  "show" + column.getName(), showColumns)) {
				headers.add(column.getLabel());
			    }
			}
		    }
		}
	    }

	    if(showMetadata!=null) {
		for(MetadataType mtd: showMetadata) {
		    headers.add(mtd.getName());
		}
	    }


	    row[0] = sheet.createRow(rowCnt++);
	    for(String header: headers) {
		add.accept(header);
	    }


	    int numCols = headers.size();

	    int cnt = 0;
            for (Entry entry : entries) {
		boolean canEdit = getAccessManager().canDoEdit(request, entry);
		typeHandler = entry.getTypeHandler();
		columns     = typeHandler.getColumns();
		String name = getEntryDisplayName(entry);
		EntryLink entryLink = showEntryDetails?getEntryManager().getAjaxLink(request, entry, name):null;
		colCnt[0]=0;
		row[0] = sheet.createRow(rowCnt++);
		if(displayColumns!=null) {
		    for(String col: displayColumns) {
			String value=null;
			if(col.equals(TypeHandler.FIELD_NAME)) {
			    String entryIcon = getPageHandler().getEntryIconImage(request, entry);
			    String url = getEntryManager().getEntryUrl(request, entry);
			    continue;
			} else if(col.equals(TypeHandler.FIELD_FILE)) {
			    if (entry.isFile()) {
				String downloadLink =
				    HU.href(entry.getTypeHandler().getEntryResourceUrl(request, entry),
					    HU.img(getIconUrl(ICON_DOWNLOAD), msg("Download"),""));
				add.accept(entry.getResource().getFileSize()+"");
			    } else {
				add.accept(NA);
			    }
			    continue;
			} else if(col.equals(TypeHandler.FIELD_CREATEDATE)) {
			    fmt.accept(entry,entry.getCreateDate());
			    continue;
			} else if(col.equals(TypeHandler.FIELD_CHANGEDATE)) {
			    fmt.accept(entry,entry.getChangeDate());
			    continue;
			} else if(col.equals(TypeHandler.FIELD_FROMDATE)) {
			    fmt.accept(entry,entry.getStartDate());
			    continue;
			} else if(col.equals(TypeHandler.FIELD_TODATE)) {
			    fmt.accept(entry,entry.getEndDate());
			    continue;
			} else {
			    Column column = typeHandler.getColumn(col);
			    if(column==null) {
				add.accept(NA);
				continue;
			    }
			    if(column.isDate()) {
				Object o = entry.getValue(request, column);
				if(o==null) {
				    add.accept(NA);
				} else {
				    Date date = (Date)o;
				    fmt.accept(entry,date.getTime());
				}
				continue;
			    }
			    String v = entry.getStringValue(request, column,"");
			    String s = entry.getTypeHandler().decorateValue(request, entry, column, v);
			    if (column.isNumeric()) {
				add.accept(v);
			    } else {
				add.accept(v);
			    }
			}
		    }
		}  else {
		    String entryIcon = getPageHandler().getEntryIconImage(request, entry);
		    String url = getEntryManager().getEntryUrl(request, entry);
		    url = request.getAbsoluteUrl(url);
		    add.accept(name);
		    add.accept(url);
		    if (showDate) {
			fmt.accept(entry,entry.getStartDate());
		    }

		    if (showCreateDate) {
			fmt.accept(entry,entry.getCreateDate());

		    }
		    if (showChangeDate) {
			fmt.accept(entry,entry.getChangeDate());
		    }
		    if (showFromDate) {
			fmt.accept(entry,entry.getStartDate());
		    }
		    if (showToDate) {
			fmt.accept(entry,entry.getEndDate());
		    }		
		    if (haveFiles) {
			String downloadUrl=request.getAbsoluteUrl(entry.getTypeHandler().getEntryResourceUrl(request, entry));

			if (entry.isFile()) {
			    add.accept(""+entry.getResource().getFileSize());
			    add.accept(downloadUrl);
			} else {
			    add.accept(NA);
			    add.accept(NA);
			}
		    }

		    if (columns != null) {
			for (Column column : columns) {
			    if (column.getCanShow() && (column.getRows() <= 1)) {
				if (column.getCanList()
				    && Utils.getProperty(props,
							 "show" + column.getName(), showColumns)) {
				    String s = entry.getStringValue(request, column,"");
				    if (column.isNumeric()) {
					add.accept(s);
				    } else {
					add.accept(s);
				    }
				}
			    }
			}
		    }
		}

		if(showMetadata!=null) {
		    List<Metadata> metadataList = getMetadataManager().getMetadata(request,entry);
		    for(MetadataType mtd: showMetadata) {
			List<Metadata> byType = getMetadataManager().getMetadata(request,entry,metadataList, mtd.getId());
			if(byType!=null && byType.size()>0) {
			    add.accept(byType.get(0).getAttr1());
			} else {
			    add.accept(NA);
			}
		    }
		}
            }
	}

	workbook.write(outputStream);
	workbook.close();
    }




    @Override
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry) 
            throws Exception {
	List<Entry> children = new ArrayList<Entry>();
	children.add(entry);
	return outputGroup(request, outputType, entry, children);
    }

    @Override
    public Result outputGroup(Request request, OutputType outputType,
                              Entry group, List<Entry> children)
            throws Exception {
	//Handle the "Choose" type from the entry search
	String what = request.getString(ARG_WHAT,"");

	if(what.equals("json")) {
	    return getRepository().getJsonOutputHandler().outputGroup(request,
								      JsonOutputHandler.OUTPUT_JSON,
								      group, children);
	}

	//Check for the wrappers
	if(outputType.getId().startsWith("wrapper")) {
	    if(outputType.equals(OUTPUT_WRAPPER_MATLAB)) 
		what=WHAT_WRAPPER_MATLAB;
	    else if(outputType.equals(OUTPUT_WRAPPER_R)) 
		what=WHAT_WRAPPER_R;
	    else 
		what=WHAT_WRAPPER_PYTHON;	    	    
	    outputType = OUTPUT_IDS;
	    request.put(ARG_WHAT, what);
	}

        if (what.equals(WHAT_XLSX) || OUTPUT_XLSX.equals(outputType)) {
	    if (group.isDummy()) {
		request.setReturnFilename("results.xlsx");
	    } else {
		request.setReturnFilename(group.getName() + ".xlsx");
	    }
	    return listXlsx(request,children);
	}


        if (!what.equals("csv") && OUTPUT_IDS.equals(outputType)) {
            return listIds(request, group, children);
	}



        if (group.isDummy()) {
            request.setReturnFilename("results.csv");
        } else {
            request.setReturnFilename(group.getName() + ".csv");
        }


        if (OUTPUT_ENTRYCSV.equals(outputType)) {
            List<Entry> tmp = new ArrayList<Entry>();
            tmp.add(group);

            return listEntries(request, tmp);
        }

        return listEntries(request, children);
    }

}

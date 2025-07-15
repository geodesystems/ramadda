/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.repository.importer;


import org.ramadda.repository.*;
import org.ramadda.repository.type.TypeHandler;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.metadata.Metadata;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;
import org.ramadda.util.seesv.DataProvider;
import org.ramadda.util.seesv.Filter;
import org.ramadda.util.seesv.Processor;
import org.ramadda.util.seesv.Row;
import org.ramadda.util.seesv.TextReader;

import ucar.unidata.xml.XmlUtil;


import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

import java.util.List;



@SuppressWarnings("unchecked")
public class CsvImporter extends ImportHandler {
    public static final XmlUtil XU = null;    
    public static final String TYPE_CSV = "CSV";
    public static final String ARG_CSV_TYPE = "csv.type";

    public CsvImporter(Repository repository) {
        super(repository);
    }


    @Override
    public void addImportTypes(List<TwoFacedObject> importTypes,
                               Appendable formBuffer) {
        super.addImportTypes(importTypes, formBuffer);
        importTypes.add(new TwoFacedObject("CSV Import", TYPE_CSV));
    }


    @Override
    public InputStream getStream(Request request, Entry parent,
                                 String fileName, InputStream stream,
				 StringBuilder message)
	throws Exception {
	boolean isMine  = request.getString(ARG_IMPORT_TYPE, "").equals(TYPE_CSV);
	if(!isMine && !Utils.stringDefined(request.getString(ARG_IMPORT_TYPE, ""))) {
	    isMine = fileName.toLowerCase().endsWith(".csv");
	}

        if (!isMine){
            return null;
        }

	HashSet seenMessage=new HashSet();
	StringBuilder myMessage =new StringBuilder();
        final StringBuffer sb = new StringBuffer("<entries>\n");
	Processor myProcessor = new Processor() {
		int entryCnt=0;
		Row headerRow;
		int typeIdx=-1;
		int nameIdx=-1;
		int privateIdx=-1;
		int descIdx=-1;
		int idIdx=-1;
		int parentIdx=-1;
		int dateIdx=-1;
		int dateIdxFrom=-1;
		int dateIdxTo=-1;		
		int latitudeIdx=-1;
		int longitudeIdx=-1;		
		Hashtable<String,Integer> metadataIdx= new Hashtable<String,Integer>();
		Hashtable<String,Integer> thumbnailIdx= new Hashtable<String,Integer>();
		Hashtable<String,Integer> newIdx= new Hashtable<String,Integer>();
		Hashtable<String,Integer> columnIdx= new Hashtable<String,Integer>();		
		String currentType="";
		TypeHandler  currentTypeHandler=null;
		int cnt=0;
		@Override
		public org.ramadda.util.seesv.Row handleRow(TextReader textReader,
							    org.ramadda.util.seesv.Row row) {
		    try {
			//Check for comments
			if(row.size()>0 && row.getString(0,"").trim().startsWith("#")) return row;
			cnt++;
			//get the indices
			if(headerRow==null) {
			    headerRow = row;
			    for(int i=0;i<row.size();i++) {
				String field = row.getString(i);
				field = field.replace("\uFEFF", "");
				String _field=field.toLowerCase().trim();
				_field = _field.replace(":","_semicolon");
				_field=Utils.makeID(_field);
				_field = _field.replace("_semicolon",":");
				//				System.err.println("FIELD:" + _field +" " + _field.length());
				if(_field.equals("name")) {
				    nameIdx=i;
				} else if(_field.equals("type")) {
				    typeIdx=i;
				} else if(_field.equals("date")) {
				    dateIdx = i;
				} else if(_field.equals("fromdate")) {
				    dateIdxFrom = i;
				} else if(_field.equals("fromto")) {
				    dateIdxTo = i;
				} else if(_field.equals("latitude")) {
				    latitudeIdx = i;
				} else if(_field.equals("longitude")) {
				    longitudeIdx = i;				    
				} else if(_field.equals("private")) {
				    privateIdx=i;				    
				} else if(_field.equals("description")) {
				    descIdx=i;
				} else if(_field.equals("id")) {
				    idIdx=i;
				} else if(_field.equals("parent")) {
				    parentIdx=i;

				} else if(_field.startsWith("new:")) {
				    String type= _field.substring("new:".length()).trim();
				    newIdx.put(type,i);				    
				} else if(_field.startsWith("metadata:")) {
				    String mtd= _field.substring("metadata:".length()).trim();
				    metadataIdx.put(mtd,i);
				} else if(_field.startsWith("thumbnail:")) {
				    String mtd= _field.substring("thumbnail:".length()).trim();
				    thumbnailIdx.put(mtd,i);				    
				} else if(_field.startsWith("property:")) {
				    String mtd= _field.substring("property:".length()).trim();
				    metadataIdx.put(mtd,i);				    
				} else if(_field.startsWith("column:")) {
				    String prop= _field.substring("column:".length()).trim();
				    columnIdx.put(prop,i);
				} else {
				    columnIdx.put(_field,i);

				}
			    }
			    if(typeIdx==-1) throw new IllegalArgumentException("input data must have a \"type\" column");
			    if(nameIdx==-1) throw new IllegalArgumentException("input data must have a \"name\" column");			    
			    return row;
			}	
			if(!row.indexOk(typeIdx)) return row;
			String tmpType = row.getString(typeIdx,"");
			if(Utils.stringDefined(tmpType)) {
			    currentType = tmpType;
			    currentTypeHandler = getRepository().getTypeHandler(currentType);
			}
			if(!Utils.stringDefined(currentType)) {
			    throw new IllegalArgumentException("No type defined");
			}
			String attrs = "";
			if(row.indexOk(nameIdx)) {
			    String name = row.getString(nameIdx,"");
			    attrs += XU.attrs("name",name, "type",currentType);
			}
			entryCnt++;
			String id = null;
			if(idIdx>=0 && row.indexOk(idIdx)) {
			    id = row.getString(idIdx,"");
			}
			if(!stringDefined(id)) {
			    id   = "entry_" + (entryCnt);
			}
			attrs+=XU.attrs("id",id);

			if(dateIdx>=0) {
			    String v = row.getString(dateIdx,"");
			    attrs+=XU.attrs("fromdate",v);
			    attrs+=XU.attrs("todate",v);			    
			}
			if(dateIdxFrom>=0) {
			    String v = row.getString(dateIdxFrom,"");
			    attrs+=XU.attrs("fromdate",v);
			}
			if(dateIdxTo>=0) {
			    String v = row.getString(dateIdxTo,"");
			    attrs+=XU.attrs("todate",v);
			}						


			if(latitudeIdx>=0) {
			    String v = row.getString(latitudeIdx,"");
			    attrs+=XU.attrs("latitude",v);
			}
			if(longitudeIdx>=0) {
			    String v = row.getString(longitudeIdx,"");
			    attrs+=XU.attrs("longitude",v);
			}			


			if(parentIdx>=0 && row.indexOk(parentIdx)) {
			    String parent = row.getString(parentIdx,"");
			    if(Utils.stringDefined(parent)) {
				attrs+=XU.attrs("parent",parent);
			    }
			}			

			sb.append(XU.openTag("entry",attrs));
			if(descIdx>=0 && row.indexOk(descIdx)) {
			    String desc = row.getString(descIdx,"");
			    if(Utils.stringDefined(desc)) {
				sb.append(XU.openTag("description",""));
				XU.appendCdata(sb,desc);
				sb.append(XU.closeTag("description"));
			    }
			}
			for (String prop : columnIdx.keySet()) {
			    int idx = columnIdx.get(prop);
			    if(!row.indexOk(idx)) continue;
			    if(prop.indexOf(".")>0) {
				List<String>propToks = Utils.splitUpTo(prop,".",2);
				String propType = propToks.get(0);
				if(!propType.equals(currentType)) continue;
				prop = propToks.get(1);
			    }

			    if(currentTypeHandler!=null && currentTypeHandler.getColumn(prop)==null) {
				if(!seenMessage.contains(prop)) {
				    myMessage.append(HU.div("Column: " + prop));
				    seenMessage.add(prop);
				}
				continue;
			    }
			    String v = row.getString(idx,"");
			    if(!Utils.stringDefined(v)) continue;
			    sb.append(XU.openTag(prop,""));
			    XU.appendCdata(sb,v);
			    sb.append(XU.closeTag(prop));
			}



			if(privateIdx>=0 && row.indexOk(privateIdx)) {
			    String v = row.getString(privateIdx,"").toLowerCase();
			    if(v.equals("true") || v.equals("yes")) {			    
				sb.append("<permissions><permission action=\"view\"><role role=\"none\"/></permission></permissions>\n");
			    }
			}

			//TODO
			for (String thumb : thumbnailIdx.keySet()) {
			    int idx = thumbnailIdx.get(thumb);
			    if(!row.indexOk(idx)) continue;
			    String v = row.getString(idx,"");
			    if(!Utils.stringDefined(v)) continue;
			}

			for (String mtdType : metadataIdx.keySet()) {
			    int idx = metadataIdx.get(mtdType);
			    if(!row.indexOk(idx)) continue;
			    String v = row.getString(idx,"");
			    if(!Utils.stringDefined(v)) continue;
			    //<access><![CDATA[admin,user]]></access>
			    //<access:admin,user>
			    String access = StringUtil.findPattern(v,"(<access:[^>]*>)");
			    if(access!=null) {
				v = v.replace(access,"");
				access = access.replace("<access:","").replace(">","");
				System.err.println("V:" + v +" access:" + access);
			    }

			    v = v.replace("\\:","_semicolon_").replace("_blank_","");
			    List<String> toks = Utils.split(v,";",true,true);
			    for(String tok: toks) {
				List<String> subToks = Utils.split(tok,":");
				sb.append(XU.openTag("metadata",XU.attrs("type",mtdType)));
				int index=0;
				for(String mtdValue: subToks) {
				    mtdValue =  mtdValue.replace("_semicolon_",":");
				    index++;
				    sb.append(XU.openTag("attr",
							 XU.attrs("index",""+index,
								  "encoded","false")));
				    XU.appendCdata(sb,mtdValue);
				    sb.append(XU.closeTag("attr"));
				} 
				if(access!=null) {
				    sb.append(XU.openTag("access",""));				    
				    XU.appendCdata(sb,access);
				    sb.append(XU.closeTag("access"));
				}				    
				sb.append(XU.closeTag("metadata"));
			    }
			}
			sb.append(XU.closeTag("entry"));
			for (String newType : newIdx.keySet()) {
			    int idx = newIdx.get(newType);
			    if(!row.indexOk(idx)) continue;
			    String v = row.getString(idx,"");
			    if(!Utils.stringDefined(v)) continue;
			    for(String name:Utils.split(v,";",true,true)) {
				String newAttrs="";
				StringBuffer contents=null;
				int index = name.toLowerCase().indexOf("description:");
				if(index>=0) {
				    String desc = name.substring(index+1).substring("description".length()).trim();
				    name = name.substring(0,index).trim();
				    if(Utils.stringDefined(desc)) {
					contents= new StringBuffer();
					contents.append(XU.openTag("description",""));
					XU.appendCdata(contents, desc);
					contents.append(XU.closeTag("description"));
				    }
				}
				newAttrs=XU.attrs("type",newType,"name",name,"parent",id);

				sb.append(XU.openTag("entry",newAttrs));
				if(contents!=null) sb.append(contents);
				sb.append(XU.closeTag("entry"));
			    }
			}
			return row;
		    } catch (Exception exc) {
			fatal(textReader, "Processing CSV import", exc);
			return null;
		    }
		}
	    };
	

	InputStream  source = new FileInputStream(fileName);
	//	for(NamedInputStream input: sources) {
	TextReader                textReader = new TextReader();

	textReader.addProcessor(myProcessor);
	Seesv csvUtil = new Seesv(new ArrayList<String>());
	textReader.setInput(new NamedInputStream("input",
						 csvUtil.makeInputStream(new IO.Path(fileName))));
	
	//						 new BufferedInputStream(source)));
	DataProvider.CsvDataProvider provider =
	    new DataProvider.CsvDataProvider(textReader,0);
	csvUtil.process(textReader, provider,0);
        sb.append("</entries>\n");
	source.close();
	if(myMessage.length()>0) {
	    message.append(getPageHandler().showDialogWarning("Some columns were not processed:" +
							      myMessage));
	}

	message.append(HU.div("New entries:"));
        return new ByteArrayInputStream(sb.toString().getBytes());
    }


}

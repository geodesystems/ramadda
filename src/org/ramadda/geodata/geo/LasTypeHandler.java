/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.geodata.geo;


import org.ramadda.data.point.text.*;
import org.ramadda.data.record.*;
import org.ramadda.data.services.PointTypeHandler;
import org.ramadda.data.services.RecordTypeHandler;
import org.ramadda.repository.*;
import org.ramadda.repository.type.*;

import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.seesv.Seesv;

import org.w3c.dom.*;

import java.io.*;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;


/**
 */
public class LasTypeHandler extends BoreholeLogTypeHandler {




    public LasTypeHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
    }



    @Override
    public RecordFile doMakeRecordFile(Request request, Entry entry,
                                       Hashtable properties,
                                       Hashtable requestProperties)
            throws Exception {
        return new LasRecordFile(getRepository(), entry, new IO.Path(getPathForEntry(request, entry,true)));
    }


    public static class LasRecordFile extends CsvFile {
        Repository repository;

        Entry entry;

	List<String> fields  = new ArrayList<String>();
        public LasRecordFile(Repository repository, Entry entry, IO.Path path)
	    throws IOException {
            super(path);
            this.repository = repository;
            this.entry      = entry;
        }




        @Override
        public InputStream doMakeInputStream(boolean buffered)
                throws Exception {
	    InputStream input = makeInputStream(false);
	    BufferedReader br   = new BufferedReader(new InputStreamReader(input));
	    String line;
	    boolean inBlock  = false;
	    while((line= br.readLine())!=null) {
		if(line.startsWith("~A")) {
		    break;
		}
		if(inBlock) {
		    if(line.startsWith("~")) inBlock=false;
		}
		
		if(line.startsWith("~Curve Information Block")) {
		    inBlock=true;
		    continue;
		}
		if(line.startsWith("#")) continue;
		if(inBlock) {
		    List<String> toks = Utils.splitUpTo(line.trim()," ",2);
		    if(toks.size()==0) continue;
		    String tok = toks.get(0).toLowerCase();
		    if(tok.equals("dept")) tok = "depth";
		    if(toks.size()>1) {
			toks = Utils.splitUpTo(toks.get(1).trim()," ",2);
			if(toks.size()>0) {
			    String unit = toks.get(0);
			    unit  = unit.replaceAll("^\\.","");
			    tok = tok+"[unit=\"" + unit +"\"]";
			}

		    }
		    fields.add(tok);
		}
	    }
	    return super.doMakeInputStream(buffered);
        }

        public VisitInfo prepareToVisit(VisitInfo visitInfo)
                throws Exception {
            super.prepareToVisit(visitInfo);
	    putFields(fields);
            return visitInfo;
        }




    }




}

/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ramadda.projects.asdi;

import org.ramadda.data.point.*;
import org.ramadda.data.point.text.*;

import org.ramadda.util.Utils;
import java.util.ArrayList;
import java.util.List;


/**
 */

public class NoaaIsdFile extends CsvFile {


    public static int STATION = 0; 
    public static int DATE = 1; 
    public static int SOURCE = 2; 
    public static int LATITUDE = 3; 
    public static int LONGITUDE = 4; 
    public static int ELEVATION = 5; 
    public static int NAME = 6; 
    public static int REPORT_TYPE = 7; 
    public static int CALL_SIGN = 8; 
    public static int QUALITY_CONTROL = 9;
    public static int FIRST_STOP = QUALITY_CONTROL;

    public static int WND = 10; 
    public static int CIG = 11; 
    public static int VIS = 12; 
    public static int TMP = 13; 
    public static int DEW = 14; 
    public static int SLP = 15; 
    public static int AA1 = 16; 
    public static int AY1 = 17; 
    public static int GA1 = 18; 
    public static int GF1 = 19; 
    public static int KA1 = 20; 
    public static int MW1 = 21; 
    public static int EQD = 22; 


    String FIELDS= "STATION,DATE,SOURCE,LATITUDE,LONGITUDE,ELEVATION,NAME,REPORT_TYPE,CALL_SIGN,QUALITY_CONTROL,WND_DIR,WND_QC,WND_TYPE,WND_SPEED,CIG_AGL,CIG_QC,CIG_TYPE,CIG_CAVOK_CODE,VIS,VIS_QC,VIS_VAR,VIS_VAR_QC,TMP,TMP_QC,DEW,DEW_QC,PRESSURE,PRESSURE_QC";


    private List<String> header;


    /**
     * ctor
     *
     * @param filename _more_
     *
     * @throws IOException On badness
     */
    public NoaaIsdFile(String filename)  throws java.io.IOException {
        super(filename);
    }


    boolean hdr = true;
    public List<String> processTokens(TextRecord record, List<String> toks,
                                      boolean isHeader) {
	
	//Don't do this now because we have skiplines=1 set in the noaatypes file
	if(false && hdr) {
	    System.err.println("HEADER");
	    hdr= false;
	    this.header = Utils.split(FIELDS,",");
	    if(false) {
	    for(int i=0;i<header.size();i++) {
		if(i>0)	System.out.print(",");
		else System.out.print("fields=");
		String s = header.get(i).toLowerCase();
		String type = "double";
		String extra = "";
		String label = Utils.makeLabel(s);
		if(s.indexOf("qc")>=0) type="string";
		if(i==STATION) type="enumeration";
		else if(i== DATE) {
		    type="date";
		    extra =" format=\"yyyyMMdd'T'HHmmss\" ";
		} else if(i== SOURCE) type="enumeration";
		else if(i==NAME || i==REPORT_TYPE) type="string";
		else if(i==CALL_SIGN || i == QUALITY_CONTROL) type="enumeration";
		System.out.print(s+"[type=" + type+" label=\"" + label+"\" " + extra+"]");
	    }
	    }
	    System.out.println("");
	    return this.header;
	}
	List<String> newToks = new ArrayList<String>();
	for(int i=0;i<toks.size() && i<=SLP;i++) {
	    String tok = toks.get(i);
	    if(i<=FIRST_STOP) {
		newToks.add(tok);
		continue;
	    }
	    if(i == EQD) {
		newToks.add(tok);
		continue;
	    }
	    newToks.addAll(Utils.split(tok,","));
	}
        return newToks;
    }    

}

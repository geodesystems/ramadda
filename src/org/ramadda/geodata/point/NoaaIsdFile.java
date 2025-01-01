/**
Copyright (c) 2008-2025 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/


package org.ramadda.geodata.point;

import org.ramadda.data.record.*;
import org.ramadda.util.IO;
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
     *
     * @throws IOException On badness
     */
    public NoaaIsdFile(IO.Path path)  throws java.io.IOException {
        super(path);
    }

    @Override
    public boolean isMissingValue(BaseRecord record, RecordField field,
				  double v) {
	return v == 999.9 || v==9999.9 || v==9999 || v==99999 || v==999999;
    }



    int cnt = 0;
    boolean hdr = true;
    public List<String> processTokens(TextRecord record, List<String> toks,
                                      boolean isHeader) {
	List<String> newToks = new ArrayList<String>();
	cnt++;
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
	    List<String>toks2 = Utils.split(tok,",");
	    if(i==WND) {
		double v = Double.parseDouble(toks2.get(3));
		v = v/10.0;
		toks2.set(3,""+v);
	    }
	    if(i==SLP || i==TMP || i==DEW) {
		double v = Double.parseDouble(toks2.get(0));
		v = v/10.0;
		toks2.set(0,""+v);
	    }
	    newToks.addAll(toks2);
	}
	//	if(cnt<10) System.err.println(newToks);
        return newToks;
    }    

}

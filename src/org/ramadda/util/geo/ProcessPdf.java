package org.ramadda.util.geo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.cos.*;

import java.io.File;
import java.util.Map;
import java.util.HashSet;

public class ProcessPdf {
    static HashSet seen = new HashSet();

    public static Object getValue(COSBase obj) throws Exception {
	if(obj instanceof COSString) {
	    COSString cs = (COSString) obj;
	    return cs.getString();
	}
	if(obj instanceof COSInteger) {
	    COSInteger cs = (COSInteger) obj;
	    return ""+cs.intValue();
	}
	if(obj instanceof COSNumber) {
	    COSNumber cs = (COSNumber) obj;
	    return ""+cs.doubleValue();
	}


	if(obj instanceof COSName) {
	    COSName cs = (COSName) obj;
	    return cs.getName();
	}
	return null;

    }
    public static void process(COSBase obj,String prefix) throws Exception {

	Object value = getValue(obj);
	if(value!=null) {
	    System.out.println(prefix+value);
	    return;
	}
	

	if(seen.contains(obj)) {
	    System.out.println(prefix+"seen " + obj.getClass().getName());
	    return;
	}	    
	seen.add(obj);
	if ( obj instanceof COSDictionary) {
	    processDict((COSDictionary) obj,prefix);
	    return;
	}

	if ( obj instanceof COSArray) {
	    processArray((COSArray) obj,prefix);
	    return;
	}		    


	if(obj instanceof COSObject) {
	    COSObject cs = (COSObject) obj;
	    COSBase base = cs.getObject();
	    Object baseValue = getValue(base);
	    if(baseValue!=null) {
		System.out.println(prefix+baseValue);
		return;
	    }		 
	    System.out.println(prefix+" Object:" + base.getClass().getName());
	    process(base,prefix);
	    return;
	}			

	System.out.println(prefix+"object:" + obj.getClass().getName() +" value:" + obj);
    }



    public static void processDict(COSDictionary dict, String prefix) throws Exception {
	int cnt = 0;
	for (COSName key : dict.keySet()) {
	    cnt++;
	    //	    if(!key.getName().equals("LGIDict")) continue;
	    COSBase value = dict.getItem(key);
	    Object rawValue = getValue(value);
	    if(rawValue!=null) {
		System.out.println(prefix+ "*" +key.getName()+ ":" + rawValue);
		continue;
	    }
	    System.out.println(prefix+ "*" +key.getName()+ ":" + value.getClass().getName());
	    process(value,prefix+"  ");
	}
	if(cnt==0) System.out.println(prefix+"no keys");
    }

    public static void processArray(COSArray array,String prefix) throws Exception {
	System.out.println(prefix+"array:" + array.size());
	for(int j=0;j<array.size();j++) {
	    COSBase element = array.get(j);
	    process(element,prefix+"  ");
	}
    }



    public static void main(String[] args) throws Exception {
        try (PDDocument doc = PDDocument.load(new File(args[0]))) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
		System.out.println("page:" + i);
                COSDictionary pageDict = doc.getPage(i).getCOSObject();
		//		processDict(pageDict,"");
		//		if(true) continue;
                // Look for LGIDict (TerraGo GeoPDF)
                COSBase base = pageDict.getDictionaryObject("LGIDict");
		if(base!=null) process(base,"");
		/*
                if ( lgiDictBase instanceof COSDictionary) {
		    COSDictionary lgiDict = (COSDictionary) lgiDictBase;
		    //		    System.out.println("dict:" + lgiDict);

                    COSArray bounds = (COSArray) lgiDict.getDictionaryObject("Bounds");
                    if (bounds != null && bounds.size() == 4) {
                        double minLon = ((COSNumber) bounds.get(0)).doubleValue();
                        double minLat = ((COSNumber) bounds.get(1)).doubleValue();
                        double maxLon = ((COSNumber) bounds.get(2)).doubleValue();
                        double maxLat = ((COSNumber) bounds.get(3)).doubleValue();

                        System.out.printf("Page %d Bounds: [%f, %f] to [%f, %f]%n",
                                i + 1, minLon, minLat, maxLon, maxLat);
                    }
		    }*/

                // Look for OGC GeoPDF /Measure or /Viewport dictionaries (optional: more complex)
            }
        }
    }
}

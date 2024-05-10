/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;


import org.ramadda.data.record.filter.*;
import org.ramadda.util.IO;
import org.ramadda.util.Utils;
import org.ramadda.util.XlsUtil;
import org.ramadda.util.TTLCache;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.*;


/**
 * The core class. This represents a file that can either be read or written.
 * It holds a filename and will create a RecordIO when dealing with files.
 * Derived classes need to overwrite the doMakeRecord method.
 *
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public abstract class RecordFile {

    private static TTLCache<String, Integer> pointCountCache = new TTLCache<String,Integer>(5*60*1000,"RecordFile Point Count" );

    /** debug */
    public static boolean debug = false;

    /** _more_ */
    public static final String PROP_DATEFORMAT = "dateformat";

    /** _more_ */
    public static final String PROP_UTCOFFSET = "utcoffset";

    /** _more_ */
    public static final String PROP_PRECISION = "precision";

    /** _more_ */
    public static final String PROP_FORMAT = "format";



    /** force compile */
    private static final RecordVisitorGroup dummy1 = null;

    /** force compile */
    private static final RecordCountVisitor dummy2 = null;

    /** force compile */
    private static final GeoRecord dummy3 = null;

    /** The file */
    private IO.Path path;

    /** default skip factor */
    private int defaultSkip = -1;

    /** how many records in the file */
    private long numRecords = 0;

    /** general properties */
    private Hashtable properties;

    /** _more_ */
    private Hashtable requestProperties;

    /** _more_ */
    private Object[] fileMetadata;

    /** _more_ */
    private String descriptionFromFile;

    /** _more_ */
    private String nameFromFile;

    /** _more_ */
    private Hashtable fileProperties = new Hashtable();


    /** _more_ */
    private int[] ymdhmsIndices;

    /** _more_ */
    private int dateIndex = -1;

    /** _more_ */
    private int timeIndex = -1;

    /** _more_ */
    private StringBuffer dttm = new StringBuffer();

    /** _more_ */
    private Date baseDate;


    private List<String[]> unitPatterns;

    /** _more_ */
    private SimpleDateFormat[] mySdfs;

    private SimpleDateFormat outputDateFormat;


    /** _more_ */
    private static SimpleDateFormat[][] SDFS = {
        { makeDateFormat("yyyy") },
        { makeDateFormat("yyyy-MM"), makeDateFormat("yyyy-MMM"),
          makeDateFormat("yyyy-MMMM"), }, { makeDateFormat("yyyy-MM-dd") },
        { makeDateFormat("yyyy-MM-dd-HH") },
        { makeDateFormat("yyyy-MM-dd-HH-mm") },
        { makeDateFormat("yyyy-MM-dd-HH-mm-ss") },
    };


    /** _more_ */
    private RecordFileContext context;

    /** _more_ */
    private File cacheFile;

    /**
     * ctor
     */
    public RecordFile() {
	//Create a dummy path
	path=new IO.Path("dummy"+ new Date().getTime());

    }

    /**
     * _more_
     *
     * @param properties _more_
     */
    public RecordFile(Hashtable properties) {
        this.properties = properties;

    }

    /**
     * _more_
     *
     * @param properties _more_
     */
    public RecordFile(IO.Path path, Hashtable properties) {
        this.path   = path;
        this.properties = properties;

    }

    /**
     * ctor
     *
     *
     * @param filename The file
     *
     */
    public RecordFile(IO.Path path) {
        this(path, null);

    }



    /**
     * _more_
     *
     * @param context _more_
     * @param properties _more_
     */
    public RecordFile(IO.Path path, RecordFileContext context,
                      Hashtable properties) {
        this(path, properties);
        this.context = context;

    }

    /**
     *  @return _more_
     */
    public RecordFileContext getRecordFileContext() {
        return context;
    }

    /** _more_ */
    protected static Hashtable filesBeingWritten = new Hashtable();



    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public File checkCachedFile() throws Exception {
        File file = getCacheFile();
	if (file != null) {
	    int cnt = 0;
	    //Wait at most 10 seconds
	    long t1 = System.currentTimeMillis();
	    while (cnt++ < 100) {
		if (filesBeingWritten.get(file) == null) {
		    break;
		}
		Misc.sleep(100);
            }
	    long t2 = System.currentTimeMillis();
	    if(t2-t1>50) {
		System.err.println("checkCache time:" + (t2-t1) +"  cnt:" + cnt);
	    }
        }

        return file;
    }



    /**
     * Set the CacheFile property.
     *
     * @param value The new value for CacheFile
     */
    public void setCacheFile(File value) {
        cacheFile = value;
    }

    /**
     * Get the CacheFile property.
     *
     * @return The CacheFile
     */
    public File getCacheFile() {
        return cacheFile;
    }


    /**
     * _more_
     *
     * @param field _more_
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getContextProperty(RecordField field, String key,
                                     String dflt) {
        if (context == null) {
            return dflt;
        }
        String v = context.getFieldProperty(field.getName(), key);
        if (v == null) {
            return dflt;
        }

        return v;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object[] getFileMetadata() {
        return fileMetadata;
    }

    /**
     * _more_
     *
     * @param metadata _more_
     */
    public void setFileMetadata(Object[] metadata) {
        fileMetadata = metadata;
    }




    /**
     * _more_
     */
    public void initAfterClone() {
        fields = null;
    }

    /**
     * _more_
     *
     * @param properties _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException On badness
     * @throws Exception _more_
     */
    public RecordFile cloneMe(IO.Path path, Hashtable properties,
                              Hashtable requestProperties)
            throws CloneNotSupportedException, Exception {
        RecordFile that = cloneMe();
        that.initAfterClone();
        that.setPath(path);
        this.requestProperties = requestProperties;
        if (properties == null) {
            properties = getPropertiesForFile(path.getPath(),
					      that.getPropertiesFileName());
        }
        that.setProperties(properties);

        return that;
    }

    /** _more_ */
    private Hashtable<String, String> classProperties;

    /**
     * _more_
     *
     * @param props _more_
     *
     * @throws Exception _more_
     */
    public void initClassProperties(Hashtable<String, String> props)
            throws Exception {}


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Hashtable getClassProperties() throws Exception {
        if (classProperties == null) {
            String propertiesFile = "/"
                                    + getClass().getName().replaceAll("\\.",
                                        "/") + ".properties";

            Hashtable<String, String> tmp = new Hashtable<String, String>();
            initClassProperties(tmp);
            tmp.putAll(
                Utils.getProperties(IOUtil.readContents(propertiesFile, "")));
            classProperties = tmp;
        }

        return classProperties;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPropertiesFileName() {
        return "record.properties";
    }



    /**
     * _more_
     *
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Hashtable getProperties(File[] files) throws Exception {
        Hashtable p = new Hashtable();
        for (File f : files) {
            if ( !f.exists()) {
                continue;
            }
            String contents = IOUtil.readContents(f);
            p.putAll(Utils.getProperties(contents));
        }

        return p;
    }





    /**
     * _more_
     *
     * @param file _more_
     * @param defaultCommonFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Hashtable getPropertiesForFile(String file,
            String defaultCommonFile)
            throws Exception {
        File   f      = new File(file);
        File   parent = f.getParentFile();
        String commonFile;
        if (parent == null) {
            commonFile = defaultCommonFile;
        } else {
            commonFile = parent + File.separator + defaultCommonFile;
        }
        File[] propertiesFiles = new File[] { new File(commonFile),
                new File(IOUtil.stripExtension(file) + ".properties"),
                new File(file + ".properties"), };

        return getProperties(propertiesFiles);
    }



    /**
     * clone me
     *
     * @return my clone
     *
     * @throws CloneNotSupportedException On badness
     */
    public RecordFile cloneMe() throws CloneNotSupportedException {
        return (RecordFile) super.clone();
    }


    /**
     * Can this recordfile load  the given file
     *
     * @param file the file
     *
     * @return default is false
     */
    public boolean canLoad(String file) {
        return false;
    }

    /**
     * Check to see if this file is capable of the given action
     *
     * @param action the action
     *
     * @return is capable
     */
    public boolean isCapable(String action) {
        String p = (String) getProperty(action);
        if (p != null) {
            return p.trim().equals("true");
        }

        return false;
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param suffixes _more_
     * @param checkForNumberSuffix _more_
     *
     * @return _more_
     */
    public boolean canLoad(String file, String[] suffixes,
                           boolean checkForNumberSuffix) {
        for (String suffix : suffixes) {
            if (file.endsWith(suffix)) {
                return true;
            }
        }
        if ( !checkForNumberSuffix) {
            return false;
        }
        file = file.trim();
        while (file.matches(".*\\.\\d+\\z")) {
            file = IOUtil.stripExtension(file);
            for (String suffix : suffixes) {
                if (file.endsWith(suffix)) {
                    return true;
                }
            }
        }

        return false;
    }



    /**
     * Get a property
     *
     * @param key property key
     *
     * @return property value
     */
    public Object getProperty(Object key) {
        if (properties == null) {
            return null;
        }

        return properties.get(key);
    }


    /**
     * _more_
     *
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String prop, String dflt) {
        String s= getProperty(properties, prop, dflt);
	if(s!=null) s = s.trim();
	return s;
    }


    /**
     * _more_
     *
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String prop, boolean dflt) {
        return getProperty(prop, "" + dflt).equals("true");
    }

    public int getProperty(String prop, int dflt) {
	String v=   getProperty(prop,null);
	if(v!=null) return Integer.parseInt(v);
	return dflt;
    }    


    /**
     * _more_
     *
     * @param field _more_
     * @param properties _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(RecordField field, Hashtable properties,
                              String prop, String dflt) {
        String value = getProperty(properties, prop, (String) null);
        if (value == null) {
            String fieldProp = "field." + field.getName() + "." + prop;
            value = (String) getProperty(fieldProp, (String) null);
        }
        if (value == null) {
            value = (String) getProperty(prop, (String) null);
        }
        if (value == null) {
	    //For now don't do this as it bubbles up to the repository and there are lots of calls to this
	    //	    value = getContextProperty(field, prop, dflt);
        }

        if (value == null) {
            value = dflt;
        }

        return value;
    }


    /**
     * _more_
     *
     * @param properties _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(Hashtable properties, String prop,
                              String dflt) {
        if (properties == null) {
            return dflt;
        }
        String value = (String) properties.get(prop);
        if (value != null) {
            return value;
        }

        return dflt;
    }



    /**
     * put a property
     *
     * @param key key
     * @param value value
     */
    public void putProperty(Object key, Object value) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(key, value);
    }


    /**
     * _more_
     *
     * @param buff _more_
     *
     * @throws Exception On badness
     */
    public void getInfo(Appendable buff) throws Exception {}

    /**
     * Make the input io object
     *
     *
     * @param visitInfo _more_
     * @return The RecordIO for reading the file
     *
     * @throws Exception _more_
     */
    public RecordIO doMakeInputIO(VisitInfo visitInfo) throws Exception {
        return doMakeInputIO(visitInfo, false);
    }


    /**
     * Make the input io object
     *
     *
     * @param visitInfo _more_
     * @param buffered If true then make a bufferedinputstream
     *
     * @return The RecordIO
     * @throws Exception _more_
     */
    public RecordIO doMakeInputIO(VisitInfo visitInfo, boolean buffered)
            throws Exception {
        return new RecordIO(doMakeInputStream(visitInfo,buffered));
    }





    public InputStream doMakeInputStream(VisitInfo visitInfo,boolean buffered) throws Exception {
	return doMakeInputStream(buffered);
    }


    /**
     * Make the input stream
     *
     * @param buffered If true then make a BufferedInputStream wrapper around the InputStream
     *
     * @return The input stream
     *
     * @throws Exception _more_
     */
    public InputStream doMakeInputStream(boolean buffered) throws Exception {
	return makeInputStream(buffered);
    }

    public final InputStream makeInputStream(boolean buffered) throws Exception {	
        IO.Path  path = getNormalizedFilename();
        File file = getCacheFile();
	if(debug) {
	    System.err.println("RecordFile.makeInputStream:" + path);
	    System.err.println("RecordFile.makeInputStream: cache file:"+ file);
	}
	//not now
	if(false && file!=null) {
	    //Only cache if it is a URL
	    if(file.exists()) {
		System.err.println("RecordFile.makeInputStream: have cache file:"+ file);
	    } else  if(path.getPath().startsWith("http")) {
		System.err.println("RecordFile.makeInputStream: writing URL to cache file:"+ file);
		InputStream inputStream = IO.doMakeInputStream(path, buffered);
		FileOutputStream fos = new FileOutputStream(file);
		IOUtil.writeTo(inputStream, fos);
		fos.flush();
		IOUtil.close(fos);
		IOUtil.close(inputStream);

	    }
	    if(file.exists()) {
		return new BufferedInputStream(new FileInputStream(file));
	    }
	}


        if (debug) {
            System.err.println("RecordFile.doMakeInputStream path:" + path);
        }


        if (path.getPath().toLowerCase().endsWith(".xls")) {
	    return XlsUtil.xlsToCsv(path,-1, getProperty("xls.sheet",0));
        }

        if (path.getPath().toLowerCase().endsWith(".xlsx")) {
	    return XlsUtil.xlsxToCsv(path,-1, getProperty("xls.sheet",0));
        }	

        if (path.getPath().endsWith(".zip") || getProperty("isZip", false)) {
            InputStream    fis = IO.getInputStream(path.getPath());
            ZipInputStream zin = new ZipInputStream(fis);
            ZipEntry       ze  = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    continue;
                }
                String p = ze.getName().toLowerCase();
                if (p.endsWith(".csv") || p.endsWith(".tsv")) {
                    return zin;
                }
                //Apple health
                if (p.equals("export.xml")) {
                    return zin;
                }
            }

            throw new IllegalArgumentException(
                "Could not find csv file in source zip file");
        }

        try {
            return IO.doMakeInputStream(path, buffered);
        } catch (Exception exc) {
            System.err.println("Error reading data:" + path);
            throw exc;
        }
    }



    /**
     * Make the output stream. Note - this always makes a BufferedOutputStream wrapper around the
     * FileOutputStream
     *
     * @return The output stream
     *
     * @throws IOException On badness
     */
    public DataOutputStream doMakeOutputStream() throws IOException {
        return new DataOutputStream(
            new BufferedOutputStream(
				     new FileOutputStream(getNormalizedFilename().getPath()), 10000));
    }

    /**
     * _more_
     *
     * @param properties _more_
     */
    public void setProperties(Hashtable properties) {
	if(this.properties!=null && properties!=null) this.properties.putAll(properties);
	else  this.properties = properties;
    }


    /**
     * Make a Record. This calls the doMakeRecord factory method to actually
     * make the record. It then sets the quickscan flag on the record if needed
     *
     * @param visitInfo Holds visit state
     *
     * @return The record
     */
    public BaseRecord makeRecord(VisitInfo visitInfo) {
        BaseRecord record = doMakeRecord(visitInfo);
	if(outputDateFormat!=null) record.setOutputDateFormat(outputDateFormat);
        if (visitInfo.getQuickScan()) {
            //            System.err.println("quick scan");
            record.setQuickScan(true);
        } else {
            //            System.err.println("full scan");
        }

        return record;
    }


    public void setOutputDateFormat(SimpleDateFormat sdf) {
	outputDateFormat=sdf;
    }
	    

    /**
     * Factory method for creating a BaseRecord object.
     *
     *
     * @param visitInfo Visit state
     * @return The BaseRecord object
     */
    public abstract BaseRecord doMakeRecord(VisitInfo visitInfo);


    /** _more_ */
    private List<RecordField> fields;


    /**
     * Get the fields for the default record
     *
     * @return List of fields in the default record
     */
    public List<RecordField> getFields() {
        return getFields(false);
    }


    /**
     * _more_
     *
     * @param failureOk _more_
     *
     * @return _more_
     */
    public List<RecordField> getFields(boolean failureOk) {
        if (fields == null) {
            fields = doMakeFields(failureOk);
        }

        return fields;
    }


    /**
     * _more_
     *
     * @param f _more_
     */
    public void setFields(List<RecordField> f) {
        fields = f;
    }

    /**
     * _more_
     *
     *
     * @param failureOk _more_
     * @return _more_
     */
    public List<RecordField> doMakeFields(boolean failureOk) {
        BaseRecord        record = makeRecord(new VisitInfo());
        List<RecordField> fields = record.getFields();

        return new ArrayList<RecordField>(fields);
    }


    /**
     * Get the fields that are marked as searchable
     *
     * @return List of searchable fields
     */
    public List<RecordField> getSearchableFields() {
        List<RecordField> fields = getFields();
        List<RecordField> result = new ArrayList<RecordField>();
        for (RecordField field : fields) {
            if (field.getSearchable()) {
                result.add(field);
            }
        }

        return result;
    }




    /**
     * Get the fields that are marked as being chartable. Note - we should generalize this as finding
     * fields with a certain flag set.
     *
     * @return Chartable fields
     */
    public List<RecordField> getChartableFields() {
        List<RecordField> fields = getFields();
        List<RecordField> result = new ArrayList<RecordField>();
        for (RecordField attr : fields) {
            if (attr.getChartable()) {
                result.add(attr);
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param currentRecord _more_
     *
     * @return _more_
     */
    public BaseRecord makeNextRecord(BaseRecord currentRecord) {
        return currentRecord;
    }

    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public VisitInfo prepareToVisit(VisitInfo visitInfo) throws Exception {
        return visitInfo;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public VisitInfo doMakeVisitInfo() {
        return new VisitInfo();
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     *
     * @return _more_
     */
    public int getSkip(VisitInfo visitInfo) {
        if ((visitInfo != null) && (visitInfo.getSkip() >= 0)) {
            return visitInfo.getSkip();
        }
        if (defaultSkip >= 0) {
            return defaultSkip;
        }

        return 0;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSkip() {
        return defaultSkip;
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public BaseRecord.ReadStatus readNextRecord(VisitInfo visitInfo,
            BaseRecord record)
            throws Exception {
        visitInfo.addRecordIndex(1);
        BaseRecord.ReadStatus status =
            record.readNextRecord(visitInfo.getRecordIO());
        return status;
    }

    /**
     * _more_
     */
    public void doQuickVisit() {
        try {
            VisitInfo tmpVisitInfo = new VisitInfo();
	    tmpVisitInfo.setQuickScan(true);
            RecordIO  recordIO     = doMakeInputIO(tmpVisitInfo, true);
            if (recordIO.isOk()) {
                tmpVisitInfo.setRecordIO(recordIO);
                prepareToVisit(tmpVisitInfo);
                recordIO.close();
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     *
     * @param visitor _more_
     * @throws Exception On badness
     */
    public void visit(RecordVisitor visitor) throws Exception {
        visit(visitor, doMakeVisitInfo(), null);
    }


    /**
     * _more_
     *
     * @param visitor _more_
     * @param filter _more_
     *
     * @throws Exception On badness
     */
    public void visit(RecordVisitor visitor, RecordFilter filter)
            throws Exception {
        visit(visitor, doMakeVisitInfo(), filter);
    }



    private int countRecords() throws Exception {
	int cnt = 0;
	VisitInfo visitInfo = new VisitInfo();
	RecordIO  recordIO     = doMakeInputIO(visitInfo, true);
	if (recordIO.isOk()) {
	    visitInfo.setRecordIO(recordIO);
	    prepareToVisit(visitInfo);
            BaseRecord record = makeRecord(visitInfo);
	    record.setSkipProcessing(true);
            while (true) {
		BaseRecord.ReadStatus status = readNextRecord(visitInfo, record);
		if (status == BaseRecord.ReadStatus.EOF) {
		    break;
		}
		cnt++;
	    }
	    recordIO.close();
	}
	return cnt;
	
    }
	
    public int  getSkipToLast(int last) throws Exception {
	if(path==null || path.getPath()==null) return 0;
	int reallySkip = 0;
	int numRecords = -1;
	Integer num = pointCountCache.get(path.getPath());
	if(num==null) {
	    long t1  = System.currentTimeMillis();
	    num = new Integer(countRecords());
	    long t2  = System.currentTimeMillis();
	    //		Utils.printTimes("RecordFile.countRecords",t1,t2);
	}
	pointCountCache.put(path.getPath(),num);
	numRecords = num;
	if(numRecords>last) {
	    reallySkip = numRecords-last;
	}
	return reallySkip;
    }


    /**
     * _more_
     *
     * @param visitor _more_
     * @param visitInfo _more_
     * @param filter _more_
     *
     * @throws Exception On badness
     */
    public void visit(RecordVisitor visitor, VisitInfo visitInfo,
                      RecordFilter filter)
            throws Exception {

        //        System.err.println("RecordFile: " + getClass().getName() + ".visit");
        if (visitInfo == null) {
            visitInfo = new VisitInfo();
        }

	int last = visitInfo.getLast();
        int      skip     = getSkip(visitInfo);
	int reallySkip = 0;
	if(last>=0) {
	    reallySkip = getSkipToLast(last);
	}
	
        RecordIO recordIO = doMakeInputIO(visitInfo, skip == 0);
        visitInfo.setRecordIO(recordIO);
        visitInfo = prepareToVisit(visitInfo);
        if (visitInfo == null) {
            return;
        }
        //Start at -1 since we always increment below
        visitInfo.setRecordIndex(-1);
        boolean ok = true;
        try {
            BaseRecord record = makeRecord(visitInfo);
	    if(reallySkip>0) {
		record.setSkipProcessing(true);
	    }

            if (visitInfo.getStart() > 0) {
                skip(visitInfo, record, visitInfo.getStart());
                visitInfo.setRecordIndex(visitInfo.getStart());
            }
            int  cnt = 0;
            long t1  = System.currentTimeMillis();
	    boolean haveStartDate = visitInfo.getStartDate()!=null;
	    long startDate =0L;
	    if(haveStartDate) {
		startDate = visitInfo.getStartDate().getTime();
	    }

	    //	    System.err.println("startDate:" + haveStartDate +" " +visitInfo.getStartDate());
	    boolean haveEndDate = visitInfo.getEndDate()!=null;
	    long endDate =0L;
	    if(haveEndDate) {
		endDate = visitInfo.getEndDate().getTime();
	    }


	    int skipCnt = 0;
            while (true) {
                if ((visitInfo.getStop() > 0)
                        && (visitInfo.getRecordIndex()
                            > visitInfo.getStop())) {
                    break;
                }
                try {
                    BaseRecord.ReadStatus status = readNextRecord(visitInfo, record);
                    if (status == BaseRecord.ReadStatus.EOF) {
			if(last==1) {
                            visitor.visitRecord(this, visitInfo, record);
			}
                        break;
                    }
		    if(reallySkip>0) {
			reallySkip--;
			if(reallySkip<=0) {
			    record.setSkipProcessing(false);
			}
			continue;
		    }
		    if(last==1) {
			continue;
		    }

                    record.index = visitInfo.getRecordIndex();
                    if (status == BaseRecord.ReadStatus.OK) {
                        if ( !processAfterReading(visitInfo, record)) {
                            continue;
                        }
			if(haveStartDate) {
			    if(record.getRecordTime()<startDate) {
				skipCnt++;
				//				if((skipCnt%1000) ==0)   System.err.println("Skip:" + skipCnt);
				continue;
			    }
			}
			if(haveEndDate) {
			    if(record.getRecordTime()>endDate) {
				//Note: This assumes that the dates in the data are ordered
				break;

			    }
			}

			//if((cnt%1000)==0) System.err.println("record #:" + cnt);
                        if ((filter == null)
                                || filter.isRecordOk(record, visitInfo)) {
                            cnt++;
                            if ((visitInfo.getMax() > 0)
                                    && (cnt > visitInfo.getMax())) {
				//				System.err.println("maxed out:" + cnt +" visitinfo:" + visitInfo);
                                break;
                            }
                            if ( !visitor.visitRecord(this, visitInfo, record)) {
                                break;
			    }
                        }
                    }
                    record = makeNextRecord(record);
                    if (record == null) {
                        return;
                    }
                    skip = getSkip(visitInfo);
                    if (skip > 0) {
                        skip(visitInfo, record, skip);
                    }
                } catch (java.io.EOFException oef) {
                    //Bad form to catch an exception as logic but...
                    //                    oef.printStackTrace();
                    break;
                } catch (Exception exc) {
                    throw exc;
                }
            }
            if (ok) {
                visitorFinished(visitor, visitInfo);
            }
            long t2 = System.currentTimeMillis();
            if (debug) {
                System.err.println("RECORD: # visited:" + cnt + " in time:"
                                   + (t2 - t1) + "ms" +" skipCnt:" + record.skipCnt);
            }
        } finally {
            try {
                recordIO.close();
            } catch (Exception ignore) {}
        }
    }

    /**
     * _more_
     *
     * @param indices _more_
     */
    public void setYMDHMSIndices(int[] indices) {

        ymdhmsIndices = indices;
        if (ymdhmsIndices != null) {
            mySdfs = getDateFormat(ymdhmsIndices);
        }
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param record _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public boolean processAfterReading(VisitInfo visitInfo, BaseRecord record)
            throws Exception {


        if (ymdhmsIndices != null) {
            setDateFromYMDHMS(record, ymdhmsIndices);
        } else if ((mySdfs != null)
                   && ((dateIndex != -1) || (timeIndex != -1))) {
            setDateFromDateAndTimeIndex(record);

        }

        return true;
    }

    /**
     * _more_
     *
     * @param visitor _more_
     * @param visitInfo _more_
     *
     * @throws Exception _more_
     */
    public void visitorFinished(RecordVisitor visitor, VisitInfo visitInfo)
            throws Exception {
        visitor.finished(this, visitInfo);
    }


    /**
     * _more_
     *
     *
     * @param pw _more_
     * @throws Exception On badness
     */
    public void printCsv(final PrintWriter pw) throws Exception {
        final int[]   cnt     = { 0 };
        RecordVisitor visitor = new RecordVisitor() {
            public boolean visitRecord(RecordFile file, VisitInfo visitInfo,
                                       BaseRecord record) {
                if (cnt[0] == 0) {
                    record.printCsvHeader(visitInfo, pw);
                }
                cnt[0]++;
                record.printCsv(visitInfo, pw);

                return true;
            }
        };
        visit(visitor);
    }


    /**
     * _more_
     *
     * @param pw _more_
     * @param fields _more_
     *
     * @throws Exception On badness
     */
    public void printCsv(final PrintWriter pw, final List<RecordField> fields)
            throws Exception {
        RecordVisitor visitor = new CsvVisitor(pw, fields);
        visit(visitor);
    }


    /**
     * _more_
     *
     *
     * @param visitInfo _more_
     * @param record _more_
     * @param howMany _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean skip(VisitInfo visitInfo, BaseRecord record, int howMany)
            throws Exception {
        visitInfo.addRecordIndex(howMany);
        //        System.err.println ("RecordFile.skip: io.skip= " + howMany);
        visitInfo.getRecordIO().getDataInputStream().skipBytes(howMany
                * record.getRecordSize());

        return true;
    }


    /**
     * _more_
     *
     * @param index _more_
     *
     * @return _more_
     *
     * @throws Exception On badness
     */
    public BaseRecord getRecord(int index) throws Exception {
        //TODO: not sure about the visitInfo
        VisitInfo  visitInfo = new VisitInfo();
        RecordIO   recordIO  = doMakeInputIO(visitInfo, false);
        BaseRecord record    = (BaseRecord) makeRecord(new VisitInfo());
        skip(new VisitInfo(recordIO), record, index);
        record.readNextRecord(recordIO);
        return record;
    }


    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public RecordIO readHeader(RecordIO recordIO) throws Exception {
        return recordIO;
    }

    /**
     * _more_
     *
     * @param recordIO _more_
     *
     * @throws Exception On badness
     */
    public void writeHeader(RecordIO recordIO) throws Exception {}


    /**
     * _more_
     *
     * @param recordOutput _more_
     * @param visitInfo _more_
     * @param filter _more_
     * @param writeHeader _more_
     *
     *
     * @return _more_
     * @throws Exception On badness
     */
    public VisitInfo write(RecordIO recordOutput, VisitInfo visitInfo,
                           RecordFilter filter, boolean writeHeader)
            throws Exception {
        if (visitInfo == null) {
            visitInfo = doMakeVisitInfo();
        }
        RecordIO recordInput = doMakeInputIO(visitInfo,
                                             getSkip(visitInfo) == 0);
        recordInput = readHeader(recordInput);
        visitInfo.setRecordIO(recordInput);
        if (writeHeader) {
            writeHeader(recordOutput);
        }
        try {
            writeRecords(recordInput, recordOutput, visitInfo, filter);
        } finally {
            try {
                recordInput.close();
            } catch (Exception ignore) {}
        }

        return visitInfo;
    }




    /**
     * _more_
     *
     * @param recordInput _more_
     * @param recordOutput _more_
     * @param visitInfo _more_
     * @param filter _more_
     *
     * @throws Exception On badness
     */
    public void writeRecords(RecordIO recordInput, RecordIO recordOutput,
                             VisitInfo visitInfo, RecordFilter filter)
            throws Exception {
        BaseRecord record = makeRecord(visitInfo);
        int        index  = 0;
        if (visitInfo.getStart() > 0) {
            skip(visitInfo, record, visitInfo.getStart());
            index = visitInfo.getStart();
        }
        long t1 = System.currentTimeMillis();
        while (true) {
            if ((visitInfo.getStop() > 0) && (index > visitInfo.getStop())) {
                break;
            }
            try {
                BaseRecord.ReadStatus status =
                    record.readNextRecord(recordInput);
                if (status != BaseRecord.ReadStatus.OK) {
                    break;
                }

                record.index = index;
                if ((filter == null)
                        || filter.isRecordOk(record, visitInfo)) {
                    visitInfo.incrCount();
                    record.write(recordOutput);
                }
                record = makeNextRecord(record);
                if (record == null) {
                    return;
                }
                int skip = getSkip(visitInfo);

                if (skip > 0) {
                    skip(visitInfo, record, skip);
                    index += skip;
                } else {
                    index++;
                }
            } catch (java.io.EOFException oef) {
                //Bad form to catch an exception as logic but...
                break;
            }
        }
        long t2 = System.currentTimeMillis();
        //            System.err.println("Time:" + (t2 - t1));
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return path==null?"no path":path.getPath();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public IO.Path getPath() {
        return path;
    }

    public String getFilename() {
	return getPath().getPath();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public IO.Path getNormalizedFilename() {
	if(this.path==null || this.path.getPath()==null) return null;
        String path        = Utils.normalizeTemplateUrl(this.path.getPath());
        String pathReplace = (String) getProperty("pathReplace");
        if (pathReplace != null) {
            List<String> toks = StringUtil.splitUpTo(pathReplace, ":", 2);
            if (toks.size() == 2) {
                String from = toks.get(0).replaceAll("_semicolon_", ":");
                String to   = toks.get(1).replaceAll("_semicolon_", ":");
                path = path.replaceAll(from, to);
                if (debug) {
                    System.err.println(
                        "RecordFile.getNormalizedFilename changed path:"
                        + path);
                }
            }
        }
	return  new IO.Path(this.path,path);
    }



    /**
     */
    public void setPath(IO.Path path) {
        this.path = path;
    }

    /**
     * _more_
     *
     * @param skip _more_
     */
    public void setDefaultSkip(int skip) {
        defaultSkip = skip;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public long getNumRecords() {
        return numRecords;
    }


    /**
     * _more_
     *
     * @param n _more_
     */
    public void setNumRecords(long n) {
        numRecords = n;
    }




    /**
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            InputStream inputStream =
                new BufferedInputStream(new FileInputStream(arg), 1000000);
            DataInputStream dis = new DataInputStream(inputStream);

        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHtmlDescription() {
        return "";
    }

    /**
     * _more_
     *
     * @param format _more_
     *
     * @return _more_
     */
    public static SimpleDateFormat makeDateFormat(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param v _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(BaseRecord record, RecordField field,
                                  double v) {
        double missing = field.getMissingValue();

        //        System.err.println("isMissing:" + v +" " + missing);
        return missing == v;
    }


    /**
     * _more_
     *
     * @param record _more_
     * @param field _more_
     * @param s _more_
     *
     * @return _more_
     */
    public boolean isMissingValue(BaseRecord record, RecordField field,
                                  String s) {
	return Utils.isStandardMissingValue(s);
    }


    /** _more_ */
    public static final String FILE_SEPARATOR = "_file_";

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getOriginalFilename(String name) {
        File f = new File(name);
        name = f.getName();
        int idx = name.indexOf(FILE_SEPARATOR);
        if (idx >= 0) {
            name = name.substring(idx + FILE_SEPARATOR.length());
        }

        return name;
    }



    /**
     * _more_
     *
     *
     * @param ymdhmsIndices _more_
     *
     * @return _more_
     */
    private SimpleDateFormat[] getDateFormat(int[] ymdhmsIndices) {
        int goodCnt = 0;
        for (int i = 0; i < ymdhmsIndices.length; i++) {
            if (ymdhmsIndices[i] < 0) {
                break;
            }
            goodCnt++;
        }

        if (goodCnt == 0) {
            return null;
        }

        return SDFS[goodCnt - 1];
    }


    /*
     * This gets called after a record has been read
     * It extracts and creates the record date/time
     */

    /**
     * _more_
     *
     * @param record _more_
     * @param indices _more_
     *
     * @throws Exception On badness
     */
    private void setDateFromYMDHMS(BaseRecord record, int[] indices)
            throws Exception {
        dttm.setLength(0);
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0) {
                break;
            }
            if (i > 0) {
                dttm.append("-");
            }
            dttm.append(getString(record, indices[i]));
        }
        setDate(record, dttm.toString());
    }



    /**
     * _more_
     *
     * @param record _more_
     *
     * @throws Exception On badness
     */
    public void setDateFromDateAndTimeIndex(BaseRecord record)
            throws Exception {
        dttm.setLength(0);
        getDateTimeString(record, dttm, dateIndex, timeIndex);

        setDate(record, dttm.toString());
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Date setDate(BaseRecord record, String dttm) throws Exception {
        if (mySdfs != null) {
            for (SimpleDateFormat sdf : mySdfs) {
                try {
                    Date date = sdf.parse(dttm);
                    record.setRecordTime(date.getTime());

                    return date;
                } catch (Exception exc) {
                    //                    System.err.println("Bad date:" + dttm + " " + exc);
                }
            }
        }

        return null;
    }



    /**
     * _more_
     *
     * @param record _more_
     * @param dttm _more_
     * @param dateIndex _more_
     * @param timeIndex _more_
     *
     * @throws Exception _more_
     */
    public void getDateTimeString(BaseRecord record, StringBuffer dttm,
                                  int dateIndex, int timeIndex)
            throws Exception {
        if (dateIndex >= 0) {
            dttm.append(getString(record, dateIndex));
        }
        if (timeIndex >= 0) {
            if (dateIndex >= 0) {
                dttm.append(" ");
            }
            //Not sure if we want to pad as the date format might handle it
            String timeField = getString(record, timeIndex);
            if (timeField.length() == 1) {
                dttm.append("000");
            } else if (timeField.length() == 2) {
                dttm.append("00");
            } else if (timeField.length() == 3) {
                dttm.append("0");
            }
            dttm.append(timeField);
        }
    }


    /**
     * _more_
     *
     * @param record _more_
     * @param index _more_
     *
     * @return _more_
     */
    public String getString(BaseRecord record, int index) {
        if (record.hasObjectValue(index)) {
            return record.getObjectValue(index).toString();
        } else {
            int v = (int) record.getValue(index);

            return Integer.toString(v);
        }
    }



    /**
     *  Set the NameFromFile property.
     *
     *  @param value The new value for NameFromFile
     */
    public void setNameFromFile(String value) {
        nameFromFile = value;
    }

    /**
     *  Get the NameFromFile property.
     *
     *  @return The NameFromFile
     */
    public String getNameFromFile() {
        return nameFromFile;
    }

    /**
     *  Set the DescriptionFromFile property.
     *
     *  @param value The new value for DescriptionFromFile
     */
    public void setDescriptionFromFile(String value) {
        descriptionFromFile = value;
    }

    /**
     *  Get the DescriptionFromFile property.
     *
     *  @return The DescriptionFromFile
     */
    public String getDescriptionFromFile() {
        return descriptionFromFile;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getFileProperties() {
        return fileProperties;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void putFileProperty(String name, Object value) {
        fileProperties.put(name, value);
    }


    /**
     * _more_
     *
     * @param dateIndex _more_
     * @param timeIndex _more_
     */
    public void setDateTimeIndex(int dateIndex, int timeIndex) {
        this.dateIndex = dateIndex;
        this.timeIndex = timeIndex;
        String pattern = "yyyy-MM-dd";
        if (timeIndex >= 0) {
            pattern += " HHmm";
        }
        mySdfs = new SimpleDateFormat[] {
            makeDateFormat(getProperty(PROP_DATEFORMAT, pattern)) };


    }



    /**
     * Set the Sdf property.
     *
     * @param value The new value for Sdf
     */
    public void setSdf(SimpleDateFormat value) {
        mySdfs = new SimpleDateFormat[] { value };
    }

    /**
     * Get the Sdf property.
     *
     * @return The Sdf
     */
    public SimpleDateFormat[] getSdf() {
        return mySdfs;
    }

    /**
     *  Set the DateIndex property.
     *
     *  @param value The new value for DateIndex
     */
    public void setDateIndex(int value) {
        dateIndex = value;
    }

    /**
     *  Get the DateIndex property.
     *
     *  @return The DateIndex
     */
    public int getDateIndex() {
        return dateIndex;
    }

    /**
     *  Set the TimeIndex property.
     *
     *  @param value The new value for TimeIndex
     */
    public void setTimeIndex(int value) {
        timeIndex = value;
    }

    /**
     *  Get the TimeIndex property.
     *
     *  @return The TimeIndex
     */
    public int getTimeIndex() {
        return timeIndex;
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String cleanFieldName(String s) {
        s = s.toLowerCase();
        s = s.replaceAll("\\s+", "_");

        return s;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTextHeader() {
        return "";
    }

    /**
     * Set the BaseDate property.
     *
     * @param value The new value for BaseDate
     */
    public void setBaseDate(Date value) {
        baseDate = value;
    }

    /**
     * Get the BaseDate property.
     *
     * @return The BaseDate
     */
    public Date getBaseDate() {
        return baseDate;
    }

    /**
       Set the UnitPatterns property.

       @param value The new value for UnitPatterns
    **/
    public void setUnitPatterns (List<String[]> value) {
	unitPatterns = value;
    }

    /**
       Get the UnitPatterns property.

       @return The UnitPatterns
    **/
    public List<String[]> getUnitPatterns () {
	return unitPatterns;
    }




}

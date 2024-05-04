/**
Copyright (c) 2008-2021 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.point.text;


import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.text.StringTokenizer;

import org.ramadda.data.point.*;

import org.ramadda.data.record.*;

import org.ramadda.util.NamedChannel;
import org.ramadda.util.Station;
import org.ramadda.util.Utils;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.seesv.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.WrapperException;

import java.io.*;

import java.nio.*;
import java.nio.channels.*;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/** This is generated code from generate.tcl. Do not edit it! */
public class TextRecord extends DataRecord {

    public static final boolean debugDate = false;


    /** _more_ */
    public static final int ATTR_FIRST =
        org.ramadda.data.point.PointRecord.ATTR_LAST;

    /** _more_ */
    private SimpleDateFormat yearFormat = Utils.makeDateFormat("yyyy-MM");
    private SimpleDateFormat sdf;



    /** _more_ */
    private String delimiter = ",";

    /** _more_ */
    private boolean delimiterIsSpace = false;

    /** _more_ */
    private boolean delimiterIsSpaces = false;

    /** _more_ */
    private boolean delimiterIsCommasOrSpaces = false;

    /** _more_ */
    protected String firstDataLine = null;


    /** _more_ */
    private String[] tokens;

    /** _more_ */
    private int[] indices;

    /** _more_ */
    private boolean[] rawOK;


    /** _more_ */
    private Date baseDate;

    /** _more_ */
    private String baseDateString;

    /** _more_ */
    private TextReader textReader;

    /** _more_ */
    private int[] fixedWidth = null;

    /** _more_ */
    private String currentLine = "";

    /** _more_ */
    private boolean bePickyAboutTokens = true;

    /** _more_ */
    private boolean lineWrap = false;

    /** _more_ */
    private boolean matchUpColumns = false;

    /**  */
    private StringTokenizer tokenizer;

    /**  */
    private List<String> tokenList = new ArrayList<String>();

    private boolean cleanInput = false;

    /** _more_ */
    private int badCnt = 0;

    private int dateErrorCnt;
    private int doubleErrorCnt=0;    
    /**
     * _more_
     */
    public TextRecord() {}


    /**
     * _more_
     *
     * @param that _more_
     */
    public TextRecord(TextRecord that) {
        super(that);
        tokens  = null;
        indices = null;
    }


    /**
     * _more_
     *
     * @param file _more_
     * @param fields _more_
     */
    public TextRecord(RecordFile file, List<RecordField> fields) {
        super(file, fields);
        initFields(fields);
    }


    /**
     * _more_
     *
     * @param file _more_
     */
    public TextRecord(RecordFile file) {
        super(file);
    }

    /**
     * _more_
     *
     * @param file _more_
     * @param dummyBigEndian _more_
     */
    public TextRecord(RecordFile file, boolean dummyBigEndian) {
        this(file);
    }

    public void setCleanInput(boolean v) {
	cleanInput = v;
    }

    /**
     * _more_
     *
     * @param fields _more_
     */
    @Override
    public void initFields(List<RecordField> fields) {
        super.initFields(fields);
        tokens  = new String[numDataFields];
        indices = new int[numDataFields];
        int idx = 0;
        for (int i = 0; i < fields.size(); i++) {
            RecordField field = fields.get(i);
            //            System.err.println("  field:" + i +" " + field.getName());
        }


        for (int i = 0; i < fields.size(); i++) {
            RecordField field = fields.get(i);

            if (field.getSynthetic() || field.hasDefaultValue()
                    || field.getSkip()) {
                continue;
            }

            indices[idx++] = field.getIndex();
        }
        for (int i = 0; i < fields.size(); i++) {
            RecordField field = fields.get(i);
            if (field.getSynthetic() || field.hasDefaultValue()
                    || field.getSkip()) {
                continue;
            }
            if (field.getColumnWidth() > 0) {
                fixedWidth = new int[tokens.length];
                int widthIdx = 0;
                for (int j = 0; j < fields.size(); j++) {
                    field = fields.get(j);
                    if (field.getSynthetic() || field.hasDefaultValue()
                            || field.getSkip()) {
                        continue;
                    }
                    fixedWidth[widthIdx++] = field.getColumnWidth();
                }

                break;
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getTokens() {
        return tokens;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLine() {
        return currentLine;
    }

    /**
     *  Set the Delimiter property.
     *
     *  @param value The new value for Delimiter
     */
    public void setDelimiter(String value) {
        delimiter = value;
        if (( !delimiter.equals("\t") && (delimiter.trim().length() == 0))
                || delimiter.equals(CsvFile.DELIMITER_SPACE)) {
            delimiterIsSpace = true;
            delimiter        = " ";
        } else if (delimiter.equals(CsvFile.DELIMITER_SPACES)) {
            delimiterIsSpaces = true;
            delimiterIsSpace  = true;
        } else if (delimiter.equals("commasorspaces")) {
            delimiterIsSpace          = true;
            delimiterIsCommasOrSpaces = true;
        } else {
            delimiterIsSpace = false;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFileDelimiter() {
        return delimiter;
    }


    /**
     *  Get the Delimiter property.
     *
     *  @return The Delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }









    private void debugLine(String s){
	s = s.replace("\n","_N");
	System.err.println(s+"******");
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
    public String readNextLine(RecordIO recordIO) throws Exception {
        boolean debug = false;
        if (textReader == null) {
            textReader = recordIO.getTextReader();
	    if(cleanInput) {
		textReader.setCleanInput(true);
	    }
        }
        while (true) {
            if (firstDataLine != null) {
                currentLine   = firstDataLine;
                firstDataLine = null;
            } else {
                currentLine = recordIO.getAndClearPutback();
		//if(debug && currentLine!=null)	    debugLine("PUT BACK:" + currentLine);
		if (currentLine == null) {
		    currentLine = textReader.readLine();
		    //  if(debug)debugLine("READ LINE 1:" + currentLine);
		}
	    }
	    if (currentLine == null) {
		if (debug) {
		    System.err.println("TextRecord: currentLine is null");
		}
		return null;
	    }
	    if ( !lineOk(currentLine)) {
		if (debug) {
		    debugLine("TextRecord: currentLine not ok:" + currentLine);
		}
		continue;
	    }

	    if (debug) {
		debugLine("TextRecord: currentLine:" + currentLine);
	    }

	    return currentLine;
	}
    }

    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public boolean isLineValidData(String line) {
        return ((TextFile) getRecordFile()).isLineValidData(line);
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
    @Override
    public ReadStatus read(RecordIO recordIO) throws Exception {
        String line = null;
        if ((tokens != null) && (tokens.length == 0)) {
	    //            System.err.println("TextRecord.read zero length tokens array");
	    //            return ReadStatus.EOF;
        }

        try {
            int fieldCnt;
            if (tokenizer == null) {
                tokenizer = Utils.getTokenizer(delimiter);
            }
            while (true) {
                line = readNextLine(recordIO);
                if (line == null) {
                    return ReadStatus.EOF;
                }
		//		debugLine("READ LINE:" + line);
		//This gets set when we're making the record count or when we are seeking to the last records
		if(skipProcessing) {
		    skipCnt++;
		    return ReadStatus.OK;
		}

                if (matchUpColumns && (rawOK == null)) {
                    List<String> toks = Utils.tokenizeColumns(line,tokenizer);
	    
                    toks = ((TextFile) getRecordFile()).processTokens(this,
                            toks, true);
                    rawOK = new boolean[toks.size()];
                    HashSet<String> seen = new HashSet<String>();
                    for (RecordField field : fields) {
                        seen.add(field.getName().trim());
                        seen.add(field.getLabel().trim());
                    }
                    for (int idx = 0; idx < toks.size(); idx++) {
                        String tok = toks.get(idx);
                        tok        = tok.trim();
                        rawOK[idx] = seen.contains(tok);
                        if ( !rawOK[idx]) {
                            String _tok = tok.replaceAll(",",
                                              " ").replaceAll("\"", "'");
                            rawOK[idx] = seen.contains(_tok);
                        }
                    }

                    continue;
                }
                if (isLineValidData(line)) {
                    break;
                }
            }

            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = "";
            }


            if (delimiterIsSpace || (fixedWidth != null)) {
                if ( !split(recordIO, line, fields)) {
                    return ReadStatus.SKIP;
                }
            } else {
                tokenList.clear();
                List<String> toks = Utils.tokenizeColumns(line, tokenizer,
                                        tokenList);
                toks = ((TextFile) getRecordFile()).processTokens(this, toks,
                        false);
		if(toks.size()>tokens.length)
		    tokens=new String[toks.size()];
                if (bePickyAboutTokens && (toks.size() != tokens.length)) {
                    StringBuilder msg =
                        new StringBuilder("Error processing file: "
                                          + getRecordFile() + "\n"
                                          + "Bad token count: expected: "
                                          + tokens.length + " got: "
                                          + toks.size() + "\n");
                    if (line.length() > 5000) {
                        line = line.substring(0, 4999) + "...";
                    }
                    msg.append("Line:" + line.replace("\n","_N") + "\n");
                    /*
                    msg.append("\nExpected:");
                    for (int i = 0; i < fields.size(); i++) {
                        RecordField field = fields.get(i);
                        if (i > 0) {
                            msg.append(", ");
                        }
                        msg.append(field.getName());
                    }
                    msg.append("\nGot:");
                    for (int i = 0; i < toks.size(); i++) {
                        if (i > 0) {
                            msg.append(", ");
                        }
                        msg.append(toks.get(i));
                        if (i > 50) {
                            msg.append(",...");

                            break;
                        }
                    }
                    */
                    msg.append("Fields:\n");
                    int max = Math.max(fields.size(), toks.size());
                    for (int i = 0; i < max; i++) {
                        if (i < fields.size()) {
                            msg.append(fields.get(i).getName());
                        } else {
                            msg.append("MISSING");
                        }
                        msg.append(":");
                        if (i < toks.size()) {
                            msg.append(toks.get(i));
                        } else {
                            msg.append("MISSING");
                        }
                        msg.append("\n");
                    }

		    //		    System.err.println(Utils.getStack(10));
                    throw new IllegalArgumentException(msg.toString());
                }
                int targetIdx = 0;
 
		for (int i = 0; (i < toks.size()) && (i < tokens.length); i++) {
                    if ((rawOK != null) && !rawOK[i]) {
                        //                        System.err.println(" raw not ok");
                        continue;
                    }
                    tokens[targetIdx++] = toks.get(i);
                }
            }

	    //	    System.out.println("row:");    for(int i=0;i<tokens.length;i++) {System.out.println("\t" + i + " " + tokens[i]);}

            TextFile textFile = (TextFile) getRecordFile();
            String   tok      = null;
            int      tokenCnt = 0;
            for (fieldCnt = 0; fieldCnt < fields.size(); fieldCnt++) {
                RecordField field = fields.get(fieldCnt);
                if (skip[fieldCnt]) {
                    continue;
                }
                if (hasDefault[fieldCnt]) {
                    if (field.isTypeString()) {
                        objectValues[fieldCnt] =
                            field.getDefaultStringValue();
                    } else if (field.isTypeDate()) {
                        String dttm = field.getDefaultStringValue();
                        objectValues[fieldCnt] = parseDate(field, dttm);
                    } else {
                        values[fieldCnt] = field.getDefaultDoubleValue();
                    }

                    continue;
                }

                //                System.err.println ("field:" + field +" " + tok);
                if (synthetic[fieldCnt]) {
                    continue;
                }


                if (indices[tokenCnt] >= 0) {
                    tok = tokens[indices[tokenCnt]];
                } else {
                    tok = tokens[tokenCnt];
                }
                tokenCnt++;

                if (field.isTypeString()) {
                    objectValues[fieldCnt] = tok;
                    continue;
                }

                if (field.isTypeDate()) {
                    tok = tok.replaceAll("\"", "");
                    Date date = null;
		    try {
			date = parseDate(field, tok);
		    } catch(Exception exc) {
			if(dateErrorCnt++<10)
			    System.err.println("bad date:" + tok);
		    }
                    if (date == null) {
                        objectValues[fieldCnt] = tok;
                    } else {
                        objectValues[fieldCnt] = date;
                    }
                    continue;
                }
                if (tok == null) {
                    System.err.println("tok null: " + tokenCnt + " " + line);
                }

                if (isMissingValue(field, tok)) {
                    values[fieldCnt] = Double.NaN;
                } else {
                    double dValue;
                    if ((idxX == fieldCnt) || (idxY == fieldCnt)) {
                        dValue = GeoUtils.decodeLatLon(tok);
                    } else {
                        try {
                            dValue = textFile.parseValue(this, field, tok);
                        } catch (Exception exc) {
			    if(doubleErrorCnt++<5)
				System.err.println("Error parsing value:" + tok + " line:" + line);
			    dValue=Double.NaN;
			    objectValues[fieldCnt] = tok;
			    //			    continue;
			    //                            throw exc;
                        }
                    }


                    if (isMissingValue(field, dValue)) {
                        dValue = Double.NaN;
                    } else {
                        dValue = field.convertValue(dValue);
                    }
                    values[fieldCnt] = dValue;
                }
            }

            if ((idxX >= 0) && (idxY >= 0)) {
                setLocation(values[idxX], values[idxY], ((idxZ >= 0)
                        ? values[idxZ]
                        : Double.NaN), dataHasLocation);
                convertedXYZToLatLonAlt = true;
            }

            if (idxTime >= 0) {
                setRecordTime(getRecordTime());
            }

            return ReadStatus.OK;
        } catch (Exception exc) {
            throw new WrapperException("Error line:" + line, exc);
        }

    }




    /**
     * _more_
     *
     * @param field _more_
     * @param tok _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Date parseDate(RecordField field, String tok) throws Exception {
	boolean debug = debugDate;
        tok = tok.trim();
	if(debug) System.err.println("parseDate:" + tok);
        if (tok.equals("") || tok.equals("null")) {
	    if(debug) System.err.println("\tno tok");
            return null;
        }
        //This is where the tok is, e.g.,  hh:mm:ss  and we prepend a base date on it
        if (field.getIsDateOffset()) {
            if (baseDateString != null) {
                tok = baseDateString + " " + tok;
            } else {
                //The field has an offset but there isn't a base date
                return new Date();
            }
        }
        String unit = field.getUnit();
        String sfmt = field.getSDateFormat();
        if ( !Utils.stringDefined(sfmt) && (unit != null)
                && unit.equals("s since 1970-01-01 00:00:00.000 UTC")) {
            sfmt = "sss";
        }


        if (sfmt != null) {
            if (sfmt.equals("SSS")) {
                if (tok.indexOf("E") >= 0) {
                    long l = ((long) Double.parseDouble(tok)) * 1000;
                    return new Date(l);
                } else {
                    long l = Long.parseLong(tok);

                    return new Date(l);
                }
            } else if (sfmt.equals("sss")) {
                if (tok.indexOf("E") >= 0) {
                    double l = Double.parseDouble(tok) * 1000;

                    return new Date((long) l);
                } else {
                    long l = Long.parseLong(tok) * 1000;

                    return new Date(l);
                }
            } else if (sfmt.equals("yyyy")) {
                //              System.out.println("tok:" + tok + " dttm:" + yearFormat.parse(tok + "-06"));
                return yearFormat.parse(tok + "-06");
                //
            }
        }

        Date date   = null;
        int  offset = field.getUtcOffset();
	if(sdf!=null) {
	    try {
		date = sdf.parse(tok);
	    } catch(Exception exc) {
	    }
	}

	if(date!=null) return date;

        try {
            date = getDateFormat(field).parse(tok);
	    if(debug) System.err.println("\tgot:" + date);
        } catch (java.text.ParseException ignore) {
	    if(debug) System.err.println("\terror:" + ignore);
            //Try to guess
	    sdf = Utils.findDateFormat(tok);
	    if(sdf!=null) {
		return sdf.parse(tok);
	    }
            date = Utils.extractDate(tok);
	    if(debug)System.err.println("\textract:" + date);
            if (date == null) {
                //Check for year
                if (tok.length() == 4) {
                    date = Utils.parseDate(tok);
		    if(debug)System.err.println("\tyear:" + date);
                }
                if (date == null) {
                    //Try tacking on UTC
                    try {
			if(debug)System.err.println("\ttacking on UTC");
                        date = getDateFormat(field).parse(tok + " UTC");
                    } catch (java.text.ParseException ignoreThisOne) {
                        throw ignore;
                    }
                }
            }
        }

	if(debug)System.err.println("\tfinal:" + date);
        if (offset != 0) {
            long millis = date.getTime();
            millis += (-offset * 1000 * 3600);
            date = new Date(millis);
        }
        return date;
    }



    /**
     * _more_
     *
     * @param field _more_
     *
     * @return _more_
     */
    private SimpleDateFormat getDateFormat(RecordField field) {
        SimpleDateFormat sdf = field.getDateFormat();
	if(debugDate)
	    System.err.println("TextRecord: date format:" + field.getSDateFormat());
        if (sdf == null) {
            field.setDateFormat(sdf =
                getRecordFile().makeDateFormat(TextFile.DFLT_DATE_FORMAT));
        }

        return sdf;
    }



    /**
     * _more_
     *
     * @param station _more_
     */
    public void setLocation(Station station) {
        this.setLocation(station.getLongitude(), station.getLatitude(),
                         station.getElevation());
        if (idxX >= 0) {
            this.setValue(idxX, station.getLongitude());
        }
        if (idxY >= 0) {
            this.setValue(idxY, station.getLatitude());
        }
        if (idxZ >= 0) {
            this.setValue(idxZ, station.getElevation());
        }

    }



    /**
     * _more_
     *
     * @param line _more_
     *
     * @return _more_
     */
    public boolean lineOk(String line) {
        if ((line.length() == 0) || line.startsWith("#")) {
            return false;
        }

        return true;
    }



    /** _more_ */
    boolean testing = false;

    /**
     * _more_
     *
     *
     * @param recordIO _more_
     * @param sourceString _more_
     * @param fields _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    int xcnt = 0;

    /**
     * _more_
     *
     * @param recordIO _more_
     * @param sourceString _more_
     * @param fields _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean split(RecordIO recordIO, String sourceString,
                         List<RecordField> fields)
            throws Exception {


        if (tokens == null) {
            testing = true;
            tokens  = new String[10];
        }
        int delimLength   = 1;
        int fullTokenCnt  = 0;
        int numTokensRead = 0;
        int fromIndex     = 0;
        int sourceLength  = sourceString.length();
        //      xcnt++;
        boolean debug = xcnt < 5;
        debug = false;
        //      if(debug)
        //          System.err.println (delimiterIsSpaces +" line:" + sourceString );
        boolean inQuotes = sourceString.startsWith("\"");

        /*
        //            10,"text column",20,"another text column"
        0
        */

        if (fixedWidth != null) {
            int lastIdx = 0;
            for (int i = 0; i < fixedWidth.length; i++) {
                String theString = sourceString.substring(lastIdx,
                                       lastIdx + fixedWidth[i]);
                tokens[numTokensRead++] = theString;
                lastIdx                 += fixedWidth[i];
            }
        } else if (delimiterIsCommasOrSpaces) {
            int cnt = 0;
            if (debug) {
                System.err.println("split:" + tokens.length);
            }
            while (true) {
                List<String> toks = Utils.split(sourceString,
                                        (sourceString.indexOf(",") >= 0)
                                        ? ","
                                        : " ", true, true);
                if (debug) {
                    System.err.println("\t" + toks.size() + " s:"
                                       + sourceString);
                }
                for (int i = 0; (i < toks.size()) && (cnt < tokens.length);
                        i++) {
                    tokens[cnt++] = toks.get(i);
                }
                if ( !lineWrap) {
                    break;
                }
                if (cnt >= tokens.length) {
                    break;
                }
                //              System.err.println("CNT:" + cnt +" " + tokens.length);
                sourceString = readNextLine(recordIO);
                if (sourceString == null) {
                    break;
                }
            }
            if (bePickyAboutTokens && (cnt != tokens.length)) {
                throw new IllegalArgumentException(
                    "Could not tokenize line: expected:" + tokens.length
                    + " rcvd: " + cnt + "\nLine: " + sourceString + "\n");
            }

            return true;

        } else if (delimiterIsSpaces) {
            int cnt = 0;
            while (true) {
                List<String> toks = Utils.split(sourceString, " ", true,
                                        true);
                for (int i = 0; (i < toks.size()) && (cnt < tokens.length);
                        i++) {
                    tokens[cnt++] = toks.get(i);
                }
                if ( !lineWrap) {
                    break;
                }
                if (cnt >= tokens.length) {
                    break;
                }
                //              System.err.println("CNT:" + cnt +" " + tokens.length);
                sourceString = readNextLine(recordIO);
                if (sourceString == null) {
                    break;
                }
            }
            if (bePickyAboutTokens && (cnt != tokens.length)) {
                //If there is an empty line then skip it
                if (cnt == 0) {
                    return false;
                }

                throw new IllegalArgumentException(
                    "Could not tokenize line: expected:" + tokens.length
                    + " rcvd: " + cnt + "\nLine: " + sourceString + "\n");
            }

            return true;
        } else {
            int idx;
            while (true) {
                if (inQuotes) {
                    idx = sourceString.indexOf("\"", fromIndex + 1);
                    int maxLines = 10;
                    while ((idx < 0) && (maxLines-- > 0)) {
                        String extraLine = readNextLine(recordIO);
                        if (extraLine == null) {
                            break;
                        }
                        sourceString = sourceString + extraLine;
                        idx = sourceString.indexOf("\"", fromIndex + 1);
                    }
                    idx++;
                } else {
                    idx = sourceString.indexOf(delimiter, fromIndex);
                }
                //                System.err.println("inquote:" + inQuotes + " from:" + fromIndex + " idx:" + idx);
                String theString;
                if (idx < 0) {
                    theString = sourceString.substring(fromIndex);
                } else {
                    theString = sourceString.substring(fromIndex, idx);
                    if (delimiterIsSpace) {
                        while ((sourceString.charAt(idx) == ' ')
                                && (idx < sourceLength)) {
                            idx++;
                        }
                        fromIndex = idx;
                    } else {
                        fromIndex = idx + delimLength;
                    }
                }

                theString = theString.trim();
                if (inQuotes) {
                    theString = theString.substring(1,
                            theString.length() - 1);
                }
                //              if(debug)
                //                  System.err.println("\ttokens[" + numTokensRead + "] = "+ theString);
                tokens[numTokensRead++] = theString;

                if ((idx < 0) || (numTokensRead == tokens.length)) {
                    break;
                }
                if (fromIndex >= sourceLength) {
                    if ((fromIndex == sourceLength)
                            && sourceString.endsWith(delimiter)) {
                        //pad out
                        tokens[numTokensRead++] = "";
                    }

                    break;
                }
                if (fromIndex < sourceLength) {
                    //                System.err.println("C:" + sourceString.charAt(fromIndex));
                    inQuotes = sourceString.charAt(fromIndex) == '\"';
                }
            }
        }

        if (testing) {
            return true;
        }


        if (bePickyAboutTokens && (numTokensRead != tokens.length)) {
            badCnt++;
            //Handle the goofy point cloud text file that occasionally has a single number
            if ((badCnt > 5) || (numTokensRead != 1)) {
                System.err.println("bad token cnt: expected:" + tokens.length
                                   + " read:" + numTokensRead + " delimiter:"
                                   + delimiter + ": is space:"
                                   + delimiterIsSpace + "\nLine:"
                                   + currentLine);

                throw new IllegalArgumentException(
                    "Could not tokenize line:\n" + currentLine + "\n");
            }

            return false;
        }
        badCnt = 0;

        return true;


    }


    /** _more_ */
    private boolean convertedXYZToLatLonAlt = false;

    /**
     * _more_
     */
    public void convertXYZToLatLonAlt() {
        convertedXYZToLatLonAlt = true;
        if (idxX >= 0) {
            values[idxX] = getLongitude();
        }
        if (idxY >= 0) {
            values[idxY] = getLatitude();
        }
        if (idxZ >= 0) {
            values[idxZ] = getAltitude();
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
        TextRecord record = new TextRecord();
        record.setDelimiter(",");
        record.testing = true;
        for (String line : args) {
            record.split(null, line, null);
        }
    }


    /**
     * _more_
     *
     * @param visitInfo _more_
     * @param pw _more_
     *
     * @return _more_
     */
    public int doPrintCsvHeader(VisitInfo visitInfo, PrintWriter pw) {
        int superCnt = super.doPrintCsvHeader(visitInfo, pw);
        int myCnt    = 0;
        if (superCnt > 0) {
            pw.print(',');
        }
        for (int i = 0; i < fields.size(); i++) {
            int         cnt         = 0;
            RecordField recordField = fields.get(i);
            if (recordField.getSkip()) {
                continue;
            }
            if (cnt > 0) {
                pw.print(',');
            }
            cnt++;
            if (convertedXYZToLatLonAlt) {
                if (i == idxX) {
                    pw.append("longitude[unit=\"degrees\"]");

                    continue;
                }
                if (i == idxY) {
                    pw.append("latitude[unit=\"degrees\"]");

                    continue;
                }
                if (i == idxZ) {
                    pw.append("altitude[unit=\"m\"]");

                    continue;
                }
            }
            fields.get(i).printCsvHeader(visitInfo, pw);
        }

        return fields.size() + superCnt;
    }



    /**
     * _more_
     *
     * @param buff _more_
     *
     * @throws Exception _more_
     */
    public void print(Appendable buff) throws Exception {
        super.print(buff);
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getSkip()) {
                continue;
            }
            System.out.println(fields.get(i).getName() + ": value:"
                               + values[i] + " ");
        }
    }


    /**
     * _more_
     *
     * @param picky _more_
     */
    public void setBePickyAboutTokens(boolean picky) {
        bePickyAboutTokens = picky;
    }

    /**
     * _more_
     *
     * @param b _more_
     */
    public void setLineWrap(boolean b) {
        lineWrap = b;
    }

    /**
     * _more_
     *
     * @param line _more_
     */
    public void setFirstDataLine(String line) {
        firstDataLine = line;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setMatchUpColumns(boolean v) {
        matchUpColumns = v;
    }


    /**
     * Set the BaseDate property.
     *
     * @param value The new value for BaseDate
     */
    public void setBaseDate(Date value) {
        baseDate = value;
        if (baseDate != null) {
            baseDateString = new SimpleDateFormat("yyyy-MM-dd").format(value);
        }
    }



    /**
     * Get the BaseDate property.
     *
     * @return The BaseDate
     */
    public Date getBaseDate() {
        return baseDate;
    }




}

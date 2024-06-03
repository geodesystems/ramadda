/**
Copyright (c) 2008-2023 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;


import org.ramadda.util.IO;
import org.ramadda.util.NamedChannel;


import org.ramadda.util.NamedInputStream;
import org.ramadda.util.Utils;

import org.ramadda.util.geo.Bounds;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.io.File;

import java.nio.*;
import java.nio.channels.*;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;


/**
 *
 * @author Jeff McWhirter
 */


@SuppressWarnings("unchecked")
public class TextReader implements Cloneable {


    /** _more_ */
    private static char NEWLINE = '\n';

    /** _more_ */
    private static char CARRIAGE_RETURN = '\r';

    /** _more_ */
    private static int UNDEF = -1;

    /** _more_ */
    private static final char QUOTE_DOUBLE = '"';

    /** _more_ */
    private List<String> files = new ArrayList<String>();

    /**  */
    private Hashtable<String, String> fieldAliases = new Hashtable<String,
                                                         String>();

    /** _more_ */
    private PrintWriter writer;

    /** _more_ */
    private File destDir = new File(".");

    /** _more_ */
    private NamedInputStream input;

    private String encoding;

    /** _more_ */
    private String inputFile;

    /** _more_ */
    private BufferedReader prependReader;

    /**  */
    private ReadableByteChannel channel;

    /**  */
    private ByteBuffer buff;


    /** _more_ */
    private String prepend;

    /** _more_ */
    private String commentChar;

    /** _more_ */
    private BufferedReader reader;


    /** _more_ */
    private OutputStream output;

    /** _more_ */
    private File outputFile;

    /** _more_ */
    private int nextChar = UNDEF;


    /**  */
    private boolean hasInput = true;

    private boolean uniqueHeader  =false;

    
    /** _more_ */
    private String delimiter = ",";

    private boolean delimiterGuess  =false;

    /**  */
    private boolean quotesNotSpecial = false;

    /** _more_ */
    private boolean splitOnSpaces = false;


    /** _more_ */
    private List<Integer> widths;

    /** _more_ */
    private String inputComment = "#";

    private List<String> comments = new ArrayList<String>();

    private String outputPrefix;

    /** _more_ */
    private String outputDelimiter = ",";

    /**  */
    private List<String> lineFilters;

    private String _startPattern;
    private Pattern startPattern;
    private boolean seenStartPattern  =false;

    /**  */
    private int skipLines = 0;


    /** _more_ */
    private int skipRows = 0;



    /** _more_ */
    private int visitedRows = 0;

    /** _more_ */
    private int maxRows = -1;

    /** _more_ */
    private int pruneBytes = 0;

    /** _more_ */
    private boolean positionStart = false;

    /** _more_ */
    private int maxLineLength = -1;

    /** _more_ */
    private int row = 0;

    /** _more_ */
    private List<String> skipStrings;

    /** _more_ */
    private List<String> changeStrings;


    /**  */
    private Processor firstProcessor;

    /**  */
    private Processor lastProcessor;

    /** _more_ */
    private DecimalFormat format;

    /** _more_ */
    private List<String> headerLines = new ArrayList<String>();

    /** _more_ */
    private Row firstRow;

    /** _more_ */
    private Row extraRow;

    /** _more_ */
    private List header;


    /** _more_ */
    private List props = new ArrayList();

    /** _more_ */
    private HashSet<Integer> sheetsToShow;

    /** _more_ */
    private List<SearchField> searchFields = new ArrayList<SearchField>();

    /** _more_ */
    private List<String> searchExpressions = new ArrayList<String>();

    /** _more_ */
    private StringBuilder lb = new StringBuilder();

    /** _more_ */
    private boolean okToRun = true;

    /** _more_ */
    private boolean allData = false;

    /** _more_ */
    private List<Row> rows;


    /** _more_ */
    private SeesvOperator currentOperator;


    /** _more_ */
    private boolean debug = false;

    private boolean debugInput = false;    

    /** _more_ */
    private StringBuilder debugSB = new StringBuilder();

    /** _more_ */
    private String lastDebugType;

    /** _more_ */
    private int debugCnt = 0;

    /** _more_ */
    private int debugLimit = 6;

    /** _more_ */
    private boolean printFields = false;

    /** _more_ */
    private List<DataProvider> providers;


    /** _more_ */
    private Hashtable properties = new Hashtable();

    /** _more_ */
    private Filter.FilterGroup filterToAddTo;

    /** _more_ */
    private List<String> changeFrom = new ArrayList<String>();

    /** _more_ */
    private List<String> changeTo = new ArrayList<String>();


    /** _more_ */
    public boolean verbose = false;

    /**  */
    public boolean cleanInput = false;

    /**  */
    private Bounds bounds;


    private Seesv.Dater  inDater;

    private Seesv.Dater  outDater;


    /**
     * _more_
     */
    public TextReader() {
    }

    /**
     * _more_
     *
     * @param dir _more_
     * @param outputFile _more_
     * @param output _more_
     */
    public TextReader(File dir, File outputFile, OutputStream output) {
        this.destDir    = dir;
        this.outputFile = outputFile;
        this.output     = output;
        if (this.outputFile != null) {
            this.output = null;
        }
    }

    /**
     * _more_
     *
     * @param reader _more_
     */
    public TextReader(BufferedReader reader) {
        this.reader = reader;
    }


    /**
     *  @return _more_
     */
    public String toString() {
        return "TextReader input:" + this.input;
    }

    /**
     *
     * @param tok _more_
     *
     * @return _more_
     */
    public String getFieldAlias(String tok) {
        return fieldAliases.get(tok);
    }

    /**
     *
     * @param name _more_
     * @param alias _more_
     */
    public void putFieldAlias(String name, String alias) {
        fieldAliases.put(name, alias);
        fieldAliases.put(alias, name);
    }



    /**
     *
     * @param seesv _more_
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Row processRow(Seesv seesv, Row row) throws Exception {
        if (firstProcessor != null) {
            row = firstProcessor.handleRow(this, row);
        }
        return row;
    }

    /**
     *
     * @throws Exception _more_
     */
    public void finishProcessing() throws Exception {
        if (firstProcessor != null) {
            firstProcessor.finish(this);
        }
    }

    /**
     */
    public void resetProcessors(boolean force) {
	seenStartPattern = false;
        if (firstProcessor != null) {
            firstProcessor.reset(force);
        }
    }

    /**
     *
     * @param processor _more_
     */
    public void addProcessor(Processor processor) {
        if (firstProcessor == null) {
            lastProcessor = firstProcessor = processor;
        } else {
            lastProcessor.setNextProcessor(processor);
            lastProcessor = processor;
        }
    }


    /**
      * @return _more_
     */
    public Processor getLastProcessor() {
        return lastProcessor;
    }

    /**
     *  Set the Verbose property.
     *
     *  @param value The new value for Verbose
     */
    public void setVerbose(boolean value) {
        verbose = value;
    }

    /**
     *  Get the Verbose property.
     *
     *  @return The Verbose
     */
    public boolean getVerbose() {
        return verbose;
    }

    /*
      print out the message if verbose=true
     */
    public void logMessage(String msg) {
	if(verbose) System.err.println(msg);
    }

    public void setInDater(Seesv.Dater dater) {
	this.inDater = dater;
    }

    public void setOutDater(Seesv.Dater dater) {
	this.outDater = dater;
    }    
    
    public Date parseDate(String d) {
	return inDater.parseDate(d);
    }

    public String formatDate(Date d) {
	return outDater.formatDate(d);
    }
    
    public TimeZone getTimeZone() {
	return inDater.getTimeZone();
    }




    /**
     * Set the Providers property.
     *
     * @param value The new value for Providers
     */
    public void setProviders(List<DataProvider> value) {
        providers = value;
    }

    /**
     * Get the Providers property.
     *
     * @return The Providers
     */
    public List<DataProvider> getProviders() {
        if (providers == null) {
            providers = new ArrayList<DataProvider>();
        }

        return providers;
    }


    /**
     * Set the Property property.
     *
     *
     * @param key _more_
     * @param value The new value for Property
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * Get the Property property.
     *
     *
     * @param key _more_
     * @return The Property
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }


    /**
     * Set the FilterToAddTo property.
     *
     * @param value The new value for FilterToAddTo
     */
    public void setFilterToAddTo(Filter.FilterGroup value) {
        filterToAddTo = value;
    }

    /**
     * Get the FilterToAddTo property.
     *
     * @return The FilterToAddTo
     */
    public Filter.FilterGroup getFilterToAddTo() {
        return filterToAddTo;
    }



    /**
     * Set the CurrentOperator property.
     *
     * @param value The new value for CurrentOperator
     */
    public void setCurrentOperator(SeesvOperator value) {
        currentOperator = value;
    }

    /**
     * Get the CurrentOperator property.
     *
     * @return The CurrentOperator
     */
    public SeesvOperator getCurrentOperator() {
        return currentOperator;
    }


    /**
     * Set the PrintFields property.
     *
     * @param value The new value for PrintFields
     */
    public void setPrintFields(boolean value) {
        printFields = value;
    }

    /**
     * Get the PrintFields property.
     *
     * @return The PrintFields
     */
    public boolean getPrintFields() {
        return printFields;
    }



    /**
     * _more_
     *
     * @param from _more_
     * @param to _more_
     */
    public void addChangeFromTo(String from, String to) {
        changeFrom.add(from);
        changeTo.add(to);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String convertContents(String s) {
        for (int i = 0; i < changeFrom.size(); i++) {
            s = s.replaceAll(changeFrom.get(i), changeTo.get(i));
        }

        return s;
    }

    /**
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String readContents() throws Exception {
	return IO.readInputStream(input.getInputStream());
    }

    /**
     * Get the ChangeFrom property.
     *
     * @return The ChangeFrom
     */
    public List<String> getChangeFrom() {
        return changeFrom;
    }


    /**
     * Get the ChangeTo property.
     *
     * @return The ChangeTo
     */
    public List<String> getChangeTo() {
        return changeTo;
    }


    /**
     * Set the AllData property.
     *
     * @param value The new value for AllData
     */
    public void setAllData(boolean value) {
        allData = value;
    }

    /**
     * Get the AllData property.
     *
     * @return The AllData
     */
    public boolean getAllData() {
        return allData;
    }


    /**
     * Set the CommentChar property.
     *
     * @param value The new value for CommentChar
     */
    public void setCommentChar(String value) {
        commentChar = value;
    }

    /**
     * Get the CommentChar property.
     *
     * @return The CommentChar
     */
    public String getCommentChar() {
        return commentChar;
    }



    /**
     * Set the VisitedRows property.
     *
     * @param value The new value for VisitedRows
     */
    public void setVisitedRows(int value) {
        visitedRows = value;
    }

    /**
     * Get the VisitedRows property.
     *
     * @return The VisitedRows
     */
    public int getVisitedRows() {
        return visitedRows;
    }

    /**
     * Set the ExtraRow property.
     *
     * @param value The new value for ExtraRow
     */
    public void setExtraRow(Row value) {
        extraRow = value;
    }

    /**
     * Get the ExtraRow property.
     *
     * @return The ExtraRow
     */
    public Row getExtraRow() {
        return extraRow;
    }



    /**
     * Set the FirstRow property.
     *
     * @param value The new value for FirstRow
     */
    public void setFirstRow(Row value) {
        firstRow = value;
    }

    /**
     * Get the FirstRow property.
     *
     * @return The FirstRow
     */
    public Row getFirstRow() {
	return  firstRow;
    }



    /**
     * Set the Rows property.
     *
     * @param value The new value for Rows
     */
    public void setRows(List<Row> value) {
        rows = value;
    }

    /**
     * Get the Rows property.
     *
     * @return The Rows
     */
    public List<Row> getRows() {
        return rows;
    }



    /**
     * _more_
     */
    public void stopRunning() {
        okToRun = false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getOkToRun() {
        return okToRun;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException _more_
     */
    public Object clone() throws CloneNotSupportedException {
        Object that = super.clone();

        return that;
    }

    /**
     * _more_
     *
     * @param input _more_
     * @param outputFile _more_
     * @param output _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException _more_
     */
    public TextReader cloneMe(NamedInputStream input, File outputFile,
                              OutputStream output)
            throws CloneNotSupportedException {
        TextReader that = (TextReader) super.clone();
	that.fieldAliases = this.fieldAliases;
	that.uniqueHeader = this.uniqueHeader;
	that.comments = this.comments;
	that.outputPrefix = this.outputPrefix;
        that.cleanInput = this.cleanInput;
        that.debug      = this.debug;
        that.bounds     = this.bounds;
        that.skipStrings   = this.skipStrings;
        that.changeStrings = this.changeStrings;
        that.setPrepend(this.prepend);
        that.allData = this.allData;
        that.input      = input;
        that.output     = output;
        that.outputFile = outputFile;
        that.writer     = null;

        if (debug) {
            that.debugSB = this.debugSB;
            if ((that.output != null) && (that.debugSB != null)
                    && (that.debugSB.length() > 0)) {
                that.getWriter().print(that.debugSB);
                that.debugSB = new StringBuilder();
            }
        }
        if (that.outputFile != null) {
            that.output = null;
        }

        return that;
    }




    /**
       Set the OutputPrefix property.

       @param value The new value for OutputPrefix
    **/
    public void setOutputPrefix (String value) {
	outputPrefix = value;
    }

    /**
       Get the OutputPrefix property.

       @return The OutputPrefix
    **/
    public String getOutputPrefix () {
	return outputPrefix;
    }



    public void addComment(String comment) {
	comments.add(comment);
    }

    public List<String> getComments() {
	return comments;
    }
	    
    public void setInputComment(String c) {
	inputComment = c;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getInputFile() {
        if (inputFile != null) {
            return inputFile;
        }
        if (input != null) {
            return input.getName();
        }
        return null;
    }

    /**
     * Set the DestDir property.
     *
     * @param value The new value for DestDir
     */
    public void setDestDir(File value) {
        destDir = value;
    }

    /**
     * Get the DestDir property.
     *
     * @return The DestDir
     */
    public File getDestDir() {
        destDir.mkdir();

        return destDir;
    }


    /**
     *
     * @param clean _more_
     */
    public void setCleanInput(boolean clean) {
        this.cleanInput = clean;
    }

    /**
     * _more_
     *
     * @param p _more_
     */
    public void setSkipPattern(String p) {
        if (skipStrings == null) {
            skipStrings = new ArrayList<String>();
        }
        skipStrings.add(p);
    }

    /**
     * _more_
     *
     * @param p _more_
     * @param to _more_
     */
    public void setChangeString(String p, String to) {
        if (changeStrings == null) {
            changeStrings = new ArrayList<String>();
        }
        changeStrings.add(p);
        changeStrings.add(to);
    }







    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int readChar() throws Exception {
        int c =  readCharOld();
        if (c == UNDEF) {
            hasInput = false;
        }
        return c;
    }


    /**
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private int readCharOld() throws Exception {
        int cnt = 0;
        while (true) {
            int c = -1;
            if (prependReader != null) {
                c = prependReader.read();
                if (c == -1) {
                    this.prependReader = null;
                    c                  = getReader().read();
                }
            } else {
                while (pruneBytes > 0) {
                    getReader().read();
                    pruneBytes--;
                }
                c = getReader().read();
            }
            if (c != 0x00) {
                return c;
            }
            cnt++;
        }
    }







    /**
     *
     * @param f _more_
     */
    public void setLineFilters(List<String> f) {
        lineFilters = f;
    }
    public void setStartPattern(String start) {
	if(StringUtil.containsRegExp(start)) {
	    startPattern = Pattern.compile(start);
	} else {
	    _startPattern =start;
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
        if ((inputComment != null) && inputComment.length()>0 && line.startsWith(inputComment)) {
            return false;
        }
	//	System.err.println("\tlineOk: " +line +" seen:" + seenStartPattern);
	if(!seenStartPattern && (startPattern!=null || _startPattern!=null)) {
	    if (startPattern !=null && !startPattern.matcher(line).find()) {
		addHeaderLine(line);
		return false;
	    }
	    if (_startPattern !=null && !line.startsWith(_startPattern)) {
		addHeaderLine(line);
		return false;
	    }	    
	    seenStartPattern = true;
	}



        if (lineFilters != null) {
            for (String f : lineFilters) {
                if (line.indexOf(f) >= 0) {
		    addHeaderLine(line);
                    return false;
                }
            }
        }

        return true;
    }




    /**
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int countLines() throws Exception {
        int numLines = 0;
        while (readLine() != null) {
            numLines++;
        }

        return numLines;
    }


    /**
     * _more_
     *
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String readLine() throws Exception {
        if ( !hasInput) {
            return null;
        }

        if (cleanInput) {
            if (prependReader != null) {
                String line = prependReader.readLine();
                if (line == null) {
                    prependReader = null;
                } else {
                    return line;
                }
            }
            return getReader().readLine();
        }


        lb.setLength(0);
        int           c;
        boolean       inQuote = false;
        StringBuilder sb      = null;
        boolean       debug   = false;
        boolean       debug2  = false;
        if (debug || debug2) {
            sb = new StringBuilder();
        }
        while (true) {
            if (debug2 && (lb.length() > 750)) {
                System.err.println("***** Whoa:" + lb);
                System.err.println("***" + sb);
            }
            if (nextChar != UNDEF) {
                c        = nextChar;
                nextChar = UNDEF;
            } else {
                c = readChar();
            }
            if (c == UNDEF) {
                String result = lb.toString();
                if (result.length() == 0) {
                    return null;
                }

                return result;
            }

            if (c == NEWLINE) {
                if (debug2) {
                    sb.append("\tnew line:" + inQuote + "\n");
                    System.err.print("new line:" + inQuote + "\n");
                }
                if (changeStrings != null) {
                    String line  = lb.toString();
                    String line2 = line;
                    for (int i = 0; i < changeStrings.size(); i += 2) {
                        String from = changeStrings.get(i);
                        String to   = changeStrings.get(i + 1);
                        line = line.replaceAll(from, to);
                    }
                    if ( !line2.equals(line)) {
                        return line;
                    }
                }

                if (skipStrings != null) {
                    String  line = lb.toString();
                    boolean ok   = true;
                    for (String skipString : skipStrings) {
                        if (line.indexOf(skipString) >= 0) {
                            lb.setLength(0);
                            inQuote  = false;
                            nextChar = UNDEF;
                            ok       = false;

                            break;
                        }
                    }
                    if ( !ok) {
                        continue;
                    }
                }
                if ( !inQuote) {
                    break;
                }
            } else if (c == CARRIAGE_RETURN) {
                if (debug2) {
                    sb.append("\tcr:" + inQuote + "\n");
                }
                if ( !inQuote) {
                    nextChar = readChar();
                    if (debug) {
                        sb.append("\tread next char:" + (char) nextChar);
                    }
                    if (nextChar == UNDEF) {
                        break;
                    }
                    if (nextChar == NEWLINE) {
                        nextChar = UNDEF;
                    }

                    break;
                }
            }
            lb.append((char) c);
            if (debug) {
                sb.append("\tchar:" + (char) c + "  inQuote: " + inQuote);
            }
            if ( !quotesNotSpecial && (c == QUOTE_DOUBLE)) {
                if (debug2) {
                    sb.append("\tquote: " + inQuote + "\n");
                }
                if ( !inQuote) {
                    if (debug) {
                        sb.append("\tinto quote\n");
                    }
                    inQuote = true;
                } else {
                    nextChar = readChar();
                    if (nextChar == UNDEF) {
                        break;
                    }
                    if (debug) {
                        sb.append("peek:" + (char) nextChar);
                    }
                    if (nextChar != QUOTE_DOUBLE) {
                        if (debug) {
                            sb.append("\tout quote\n");
                        }
                        inQuote = false;
                        if (nextChar == NEWLINE) {
                            nextChar = UNDEF;
                            if (debug) {
                                sb.append("\tnew line:" + inQuote + "\n");
                            }

                            break;
                        }
                        if (nextChar == CARRIAGE_RETURN) {
                            nextChar = readChar();
                            if (nextChar == UNDEF) {
                                break;
                            }
                            if (nextChar == NEWLINE) {
                                nextChar = UNDEF;
                            }

                            break;
                        }
                    }
                    if (nextChar != UNDEF) {
                        if (debug) {
                            sb.append("\tnext char:" + (char) nextChar
                                      + "\n");
                        }
                        lb.append((char) nextChar);
                    }
                    nextChar = UNDEF;
                }
            }
        }
        if (debug) {
            System.out.println(sb);
        }

        return lb.toString();


    }

    /**
     * _more_
     *
     * @param sheetIdx _more_
     *
     * @return _more_
     */
    public boolean okToShowSheet(int sheetIdx) {
        if ((sheetsToShow != null) && !sheetsToShow.contains(sheetIdx)) {
            return false;
        }

        return true;
    }






    /**
     * _more_
     */
    public void incrRow() {
        row++;
    }


    /**
     *  Get the Row property.
     *
     *  @return The Row
     */
    public int getRow() {
        return row;
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param s _more_
     */
    public void printDebug(String type, String s) {
        if ( !debug) {
            return;
        }
        if (Misc.equals(type, lastDebugType)) {
            debugCnt++;
            if (debugCnt == debugLimit) {
                printDebug("\t" + s);
                printDebug("\t...");

                return;
            } else if (debugCnt > debugLimit) {
                return;
            }
        } else {
            lastDebugType = type;
            debugCnt      = 0;
            printDebug(type);
        }
        printDebug("\t" + s);
    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public void printDebug(String s) {
        if ( !debug) {
            return;
        }
        if (writer != null) {
            writer.println(s);
        } else {
            debugSB.append(s);
            debugSB.append("\n");
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getDebug() {
        return debug;
    }

    /**
     * _more_
     *
     * @param debug _more_
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
       Set the DebugInput property.

       @param value The new value for DebugInput
    **/
    public void setDebugInput (boolean value) {
	debugInput = value;
    }

    /**
       Get the DebugInput property.

       @return The DebugInput
    **/
    public boolean getDebugInput () {
	return debugInput;
    }




    public void print(Object o) {
	getWriter().print(o);
    }

    public void println(Object o) {
	getWriter().println(o);
    }    


    /**
     * _more_
     *
     * @return _more_
     */
    public PrintWriter getWriter() {

        try {
            if (writer == null) {
                OutputStream os = this.getOutput();
		//		System.err.println("TextReader.getWriter");
                writer = new PrintWriter(os);
                if (getDebug()) {
                    if (debugSB.length() > 0) {
                        writer.print(debugSB);
                        debugSB = new StringBuilder();
                    }
                }
            }

            return writer;
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }

    }


    /**
     * _more_
     *
     * @param writer _more_
     */
    public void setWriter(PrintWriter writer) {
	System.err.println("setWriter");
        this.writer = writer;
    }

    /**
     * _more_
     *
     * @param filename _more_
     *
     * @return _more_
     */
    public String getFilepath(String filename) {
        String file = IOUtil.joinDir(getDestDir(), filename);
        files.add(file);

        return file;
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public void addFile(String file) {
        files.add(file);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void print(String s) {
        if (getWriter() != null) {
            getWriter().print(s);
        }
    }


    /**
     * _more_
     */
    public void flush() {
        if (writer != null) {
            writer.flush();
        }
    }

    /**
     * _more_
     */
    public void close() {
        if (output != null) {
            IOUtil.close(output);
        }
    }

    /**
     * _more_
     *
     * @param header _more_
     */
    public void setHeader(List header) {
        this.header = header;
    }

    /**
     * _more_
     *
     * @param row _more_
     */
    public void initRow(Row row) {
        if (this.header == null) {
            this.header = row.getValues();
        }
        visitedRows++;
    }


    /**
     * _more_
     *
     * @param line _more_
     */
    public void addHeaderLine(String line) {
        headerLines.add(line);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getHeaderLines() {
        return headerLines;
    }

    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String formatValue(double value) {
        if (format != null) {
            return format.format(value);
        }

        return "" + value;
    }


    /**
     * Set the Format property.
     *
     * @param value The new value for Format
     */
    public void setFormat(String value) {
        if (value == null) {
            format = null;
        } else {
            format = new DecimalFormat(value);
        }
    }



    public void setEncoding(String encoding) {
	this.encoding = encoding;
    }
    /**
     * _more_
     *
     * @param value _more_
     */
    public void setInput(InputStream value) {
        this.setInput(new NamedInputStream("input", value));
    }


    /**
     * Set the Input property.
     *
     * @param value The new value for Input
     */
    public void setInput(NamedInputStream value) {
        input = value;
	reader = null;
	hasInput = true;
    }

    /**
     * Get the Input property.
     *
     * @return The Input
     */
    public NamedInputStream getInput() {
        return input;
    }

    public String getCurrentFilename() {
	if(input!=null) return input.getName();
	return "";
    }

    public String applyMacros(String s) {
	String source = getCurrentFilename();
	String name  = IOUtil.stripExtension(source);
	String id  = Utils.makeID(name);
	return s.replace("${file_name}",source).replace("${file_shortname}",name).replace("${file_id}",id);
    }	



    /**
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream getInputStream() throws Exception {
        if (input != null) {
            return input.getInputStream();
        } else {
            return null;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public BufferedReader getReader() throws Exception {
        if (reader == null) {
            NamedInputStream input = getInput();
            InputStreamReader isr;
	    if(encoding!=null) {
		//		System.err.println("TextReader.getReader encoding:" + encoding);
                isr = new InputStreamReader(
					    input.getInputStream(),
					    encoding);
	    } else {
		//		System.err.println("UTF8");
                isr = new InputStreamReader(
					    input.getInputStream(),
					    java.nio.charset.StandardCharsets.UTF_8);
	    }
            reader = new BufferedReader(isr);
        }

        return reader;
    }


    /**
     * _more_
     *
     * @param text _more_
     */
    public void setPrepend(String text) {
        this.prepend = text;
        if (text != null) {
            this.prependReader = new BufferedReader(
						    new InputStreamReader(
									  new ByteArrayInputStream(text.getBytes())));
        } else {
            this.prependReader = null;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPrepend() {
        return prepend;
    }

    /**
     * _more_
     *
     * @param reader _more_
     */
    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * Set the Output property.
     *
     * @param value The new value for Output
     */
    public void setOutput(OutputStream value) {
        output = value;
    }

    /**
     * Get the Output property.
     *
     * @return The Output
     *
     * @throws Exception _more_
     */
    public OutputStream getOutput() throws Exception {
        if ((output == null) && (outputFile != null)) {
            //This forces the mkdir
            getDestDir();
            files.add(outputFile.toString());
            output = new FileOutputStream(outputFile);
        }

        return output;
    }

    /**
     * _more_
     *
     * @param file _more_
     */
    public void setOutputFile(File file) {
        outputFile = file;
	output = null;
	writer=null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File getOutputFile() {
        //This forces the mkdir
        if (outputFile != null) {
            getDestDir();
        }

        return outputFile;
    }


    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    public File setOutputFileSuffix(String suffix) {
        if (outputFile == null) {
            return null;
        }
        getDestDir();
        outputFile = new File(IOUtil.stripExtension(outputFile.toString())
                              + "." + suffix);

        return outputFile;
    }



    /**
     * Set the MaxRows property.
     *
     * @param value The new value for MaxRows
     */
    public void setMaxRows(int value) {
        maxRows = value;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPruneBytes(int value) {
        pruneBytes = value;
    }

    /**
     * Get the MaxRows property.
     *
     * @return The MaxRows
     */
    public int getMaxRows() {
        return maxRows;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setMaxLineLength(int value) {
        maxLineLength = value;
    }


    /**
     * Set the Skip property.
     *
     * @param value The new value for Skip
     */
    public void setSkipRows(int value) {
        skipRows = value;
    }

    /**
     * Get the Skip property.
     *
     * @return The Skip
     */
    public int getSkipRows() {
        return skipRows;
    }


    /**
     * Set the SkipLines property.
     *
     * @param value The new value for SkipLines
     */
    public void setSkipLines(int value) {
        skipLines = value;
    }

    /**
     * Get the SkipLines property.
     *
     * @return The SkipLines
     */
    public int getSkipLines() {
        return skipLines;
    }




    /**
     * Set the Widths property.
     *
     * @param value The new value for Widths
     */
    public void setWidths(List<Integer> value) {
        widths = value;
    }

    /**
     * Get the Widths property.
     *
     * @return The Widths
     */
    public List<Integer> getWidths() {
        return widths;
    }




    /**
     * Set the Delimiter property.
     *
     * @param value The new value for Delimiter
     */
    public void setDelimiter(String value) {
        delimiter = value;
        if (delimiter != null) {
            if (delimiter.equals("?")) {
		delimiterGuess=true;
		delimiter="";
	    } else  if (delimiter.equals("tab")) {
                delimiter = "\t";
            } else if (delimiter.equals("space")) {
                delimiter = " ";
            } else if (delimiter.equals("spaces")) {
                delimiter     = " ";
                splitOnSpaces = true;
            }
        }
    }

    /**
     *
     * @param v _more_
     */
    public void setQuotesNotSpecial(boolean v) {
        quotesNotSpecial = v;
    }


    /**
     * Get the Delimiter property.
     *
     * @return The Delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     */
    public void addTableProperty(String name, String value) {
        props.add(name);
        props.add(value);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTableProperties() {
        return props;
    }

    /**
     *  Set the SheetsToShow property.
     *
     *  @param value The new value for SheetsToShow
     */
    public void setSheetsToShow(HashSet<Integer> value) {
        sheetsToShow = value;
    }

    /**
     *  Get the SheetsToShow property.
     *
     *  @return The SheetsToShow
     */
    public HashSet<Integer> getSheetsToShow() {
        return sheetsToShow;
    }


    /**
     * Set the SearchFields property.
     *
     * @param value The new value for SearchFields
     */
    public void setSearchFields(List<SearchField> value) {
        searchFields = value;
    }

    /**
     * Get the SearchFields property.
     *
     * @return The SearchFields
     */
    public List<SearchField> getSearchFields() {
        return searchFields;
    }

    /**
     * Set the OutputDelimiter property.
     *
     * @param value The new value for OutputDelimiter
     */
    public void setOutputDelimiter(String value) {
        outputDelimiter = value;
    }

    /**
     * Get the OutputDelimiter property.
     *
     * @return The OutputDelimiter
     */
    public String getOutputDelimiter() {
        return outputDelimiter;
    }


    /**
     * _more_
     *
     * @param expr _more_
     */
    public void addSearchExpression(String expr) {
        searchExpressions.add(expr);
    }

    /**
     *  Get the SearchExpressions property.
     *
     *  @return The SearchExpressions
     */
    public List<String> getSearchExpressions() {
        return searchExpressions;
    }

    /**
     * Set the SplitSpaces property.
     *
     * @param value The new value for SplitSpaces
     */
    public void setSplitOnSpaces(boolean value) {
        splitOnSpaces = value;
    }

    /**
     * Get the SplitSpaces property.
     *
     * @return The SplitSpaces
     */
    public boolean getSplitOnSpaces() {
        return splitOnSpaces;
    }

    /**
     * Set the PositionStart property.
     *
     * @param value The new value for PositionStart
     */
    public void setPositionStart(boolean value) {
        positionStart = value;
    }

    /**
     * Get the PositionStart property.
     *
     * @return The PositionStart
     */
    public boolean getPositionStart() {
        return positionStart;
    }

    /**
     *  Set the Bounds property.
     *
     *  @param value The new value for Bounds
     */
    public void setBounds(Bounds value) {
        bounds = value;
    }

    /**
     *  Get the Bounds property.
     *
     *  @return The Bounds
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
       Set the UniqueHeader property.

       @param value The new value for UniqueHeader
    **/
    public void setUniqueHeader (boolean value) {
	uniqueHeader = value;
    }

    /**
       Get the UniqueHeader property.

       @return The UniqueHeader
    **/
    public boolean getUniqueHeader () {
	return uniqueHeader;
    }


    /**
       Set the DelimiterGuess property.

       @param value The new value for DelimiterGuess
    **/
    public void setDelimiterGuess (boolean value) {
	delimiterGuess = value;
    }

    /**
       Get the DelimiterGuess property.

       @return The DelimiterGuess
    **/
    public boolean getDelimiterGuess () {
	return delimiterGuess;
    }




    /**
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 5; i++) {
            TextReader          textReader = new TextReader();
	    textReader.setCleanInput(true);
            InputStream         fis        = new FileInputStream(args[0]);
	    textReader.setReader(new BufferedReader(new InputStreamReader(fis)));
            long t1  = System.currentTimeMillis();
            int  cnt = 0;
            while (true) {
                String line = textReader.readLine();
                if (line == null) {
                    break;
                }
                cnt++;
            }
            long t2 = System.currentTimeMillis();
            System.err.println("cnt:" + cnt + " time:" + (t2 - t1));
        }
        System.err.println("reader");
        for (int i = 0; i < 5; i++) {
            InputStream fis = new FileInputStream(args[0]);
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(fis));
            long t1  = System.currentTimeMillis();
            int  cnt = 0;
            while (true) {
                if (reader.readLine() == null) {
                    break;
                }
                cnt++;
            }
            long t2 = System.currentTimeMillis();
            System.err.println("cnt:" + cnt + " time:" + (t2 - t1));
        }


        Utils.exitTest(0);
    }


}

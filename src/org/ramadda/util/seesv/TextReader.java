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
    private static char NEWLINE = '\n';
    private static char CARRIAGE_RETURN = '\r';
    private static int UNDEF = -1;
    private static final char QUOTE_DOUBLE = '"';

    private List<String> files = new ArrayList<String>();
    private Hashtable<String, String> fieldAliases = new Hashtable<String,
                                                         String>();

    private PrintWriter writer;
    private File destDir = new File(".");
    private NamedInputStream input;
    private String encoding;
    private String inputFile;
    private BufferedReader prependReader;
    private ReadableByteChannel channel;
    private ByteBuffer buff;
    private String prepend;
    private String commentChar;
    private BufferedReader reader;
    private OutputStream output;
    private File outputFile;
    private int nextChar = UNDEF;
    private boolean hasInput = true;
    private boolean uniqueHeader  =false;
    private boolean trimLine = false;
    private String delimiter = ",";
    private List<String> delimiters;
    private boolean delimiterGuess  =false;
    private boolean quotesNotSpecial = false;
    private boolean splitOnSpaces = false;
    private List<Integer> widths;
    private String inputComment = "#";
    private List<String> comments = new ArrayList<String>();
    private String outputPrefix;
    private String outputDelimiter = ",";
    private List<String> lineFilters;
    private String _startPattern;
    private Pattern startPattern;
    private boolean seenStartPattern  =false;
    private int skipLines = 0;
    private int skipRows = 0;
    private int visitedRows = 0;
    private int maxRows = -1;
    private int pruneBytes = 0;
    private boolean positionStart = false;
    private int maxLineLength = -1;
    private int row = 0;
    private List<String> skipStrings;
    private List<String> changeStrings;
    private Processor firstProcessor;
    private Processor lastProcessor;
    private DecimalFormat format;
    private List<String> headerLines = new ArrayList<String>();
    private Row firstRow;
    private Row extraRow;
    private List header;
    private List props = new ArrayList();
    private HashSet<Integer> sheetsToShow;
    private List<SearchField> searchFields = new ArrayList<SearchField>();
    private List<String> searchExpressions = new ArrayList<String>();
    private StringBuilder lb = new StringBuilder();
    private boolean okToRun = true;
    private boolean allData = false;
    private List<Row> rows;
    private SeesvOperator currentOperator;
    private boolean debug = false;
    private boolean debugInput = false;    
    private StringBuilder debugSB = new StringBuilder();
    private String lastDebugType;
    private int debugCnt = 0;
    private int debugLimit = 6;
    private boolean printFields = false;
    private List<DataProvider> providers;
    private Hashtable properties = new Hashtable();
    private Filter.FilterGroup filterToAddTo;
    private List<String> changeFrom = new ArrayList<String>();
    private List<String> changeTo = new ArrayList<String>();
    public boolean verbose = false;
    public boolean cleanInput = false;
    private Bounds bounds;
    private Seesv.Dater  inDater;
    private Seesv.Dater  outDater;

    public TextReader() {
    }

    public TextReader(File dir, File outputFile, OutputStream output) {
        this.destDir    = dir;
        this.outputFile = outputFile;
        this.output     = output;
        if (this.outputFile != null) {
            this.output = null;
        }
    }

    public TextReader(BufferedReader reader) {
        this.reader = reader;
    }

    public String toString() {
        return "TextReader input:" + this.input;
    }

    public String getFieldAlias(String tok) {
        return fieldAliases.get(tok);
    }

    public void putFieldAlias(String name, String alias) {
        fieldAliases.put(name, alias);
        fieldAliases.put(alias, name);
    }

    public Row processRow(Seesv seesv, Row row) throws Exception {
        if (firstProcessor != null) {
            row = firstProcessor.handleRow(this, row);
        }
        return row;
    }

    public void finishProcessing() throws Exception {
        if (firstProcessor != null) {
            firstProcessor.finish(this);
        }
    }

    public void resetProcessors(boolean force) {
	seenStartPattern = false;
        if (firstProcessor != null) {
            firstProcessor.reset(force);
        }
    }

    public void addProcessor(Processor processor) {
        if (firstProcessor == null) {
            lastProcessor = firstProcessor = processor;
        } else {
            lastProcessor.setNextProcessor(processor);
            lastProcessor = processor;
        }
    }

    public Processor getLastProcessor() {
        return lastProcessor;
    }

    public void setVerbose(boolean value) {
        verbose = value;
    }

    public boolean getVerbose() {
        return verbose;
    }

    public void setTrimLine (boolean value) {
	trimLine = value;
    }

    public boolean getTrimLine () {
	return trimLine;
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

    public void setProviders(List<DataProvider> value) {
        providers = value;
    }

    public List<DataProvider> getProviders() {
        if (providers == null) {
            providers = new ArrayList<DataProvider>();
        }

        return providers;
    }

    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public void setFilterToAddTo(Filter.FilterGroup value) {
        filterToAddTo = value;
    }

    public Filter.FilterGroup getFilterToAddTo() {
        return filterToAddTo;
    }

    public void setCurrentOperator(SeesvOperator value) {
        currentOperator = value;
    }

    public SeesvOperator getCurrentOperator() {
        return currentOperator;
    }

    public void setPrintFields(boolean value) {
        printFields = value;
    }

    public boolean getPrintFields() {
        return printFields;
    }

    public void addChangeFromTo(String from, String to) {
        changeFrom.add(from);
        changeTo.add(to);
    }

    public String convertContents(String s) {
        for (int i = 0; i < changeFrom.size(); i++) {
            s = s.replaceAll(changeFrom.get(i), changeTo.get(i));
        }

        return s;
    }

    public String readContents() throws Exception {
	return IO.readInputStream(input.getInputStream());
    }

    public List<String> getChangeFrom() {
        return changeFrom;
    }

    public List<String> getChangeTo() {
        return changeTo;
    }

    public void setAllData(boolean value) {
        allData = value;
    }

    public boolean getAllData() {
        return allData;
    }

    public void setCommentChar(String value) {
        commentChar = value;
    }

    public String getCommentChar() {
        return commentChar;
    }

    public void setVisitedRows(int value) {
        visitedRows = value;
    }

    public int getVisitedRows() {
        return visitedRows;
    }

    public void setExtraRow(Row value) {
        extraRow = value;
    }

    public Row getExtraRow() {
        return extraRow;
    }

    public void setFirstRow(Row value) {
        firstRow = value;
    }

    public Row getFirstRow() {
	return  firstRow;
    }

    public void setRows(List<Row> value) {
        rows = value;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void stopRunning() {
        okToRun = false;
    }

    public boolean getOkToRun() {
        return okToRun;
    }

    public Object clone() throws CloneNotSupportedException {
        Object that = super.clone();

        return that;
    }

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

    public void setOutputPrefix (String value) {
	outputPrefix = value;
    }

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

    public String getInputFile() {
        if (inputFile != null) {
            return inputFile;
        }
        if (input != null) {
            return input.getName();
        }
        return null;
    }

    public void setDestDir(File value) {
        destDir = value;
    }

    public File getDestDir() {
        destDir.mkdir();

        return destDir;
    }

    public void setCleanInput(boolean clean) {
        this.cleanInput = clean;
    }

    public void setSkipPattern(String p) {
        if (skipStrings == null) {
            skipStrings = new ArrayList<String>();
        }
        skipStrings.add(p);
    }

    public void setChangeString(String p, String to) {
        if (changeStrings == null) {
            changeStrings = new ArrayList<String>();
        }
        changeStrings.add(p);
        changeStrings.add(to);
    }

    private int readChar() throws Exception {
        int c =  readCharOld();
        if (c == UNDEF) {
            hasInput = false;
        }
        return c;
    }

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

    public void setLineFilters(List<String> f) {
        lineFilters = f;
    }
    public void setStartPattern(String start) {
	start=start.replace("\\t","\t").replace("_tab_","\t");
	if(StringUtil.containsRegExp(start)) {
	    startPattern = Pattern.compile(start);
	} else {
	    _startPattern =start;
	}
    }

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

    public int countLines() throws Exception {
        int numLines = 0;
        while (readLine() != null) {
            numLines++;
        }

        return numLines;
    }

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
	    String line =  getReader().readLine();
	    if(trimLine && line!=null) line= line.trim();
	    return line;
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

	String line =  lb.toString();
	if(trimLine && line!=null) line= line.trim();
	return line;
    }

    public boolean okToShowSheet(int sheetIdx) {
        if ((sheetsToShow != null) && !sheetsToShow.contains(sheetIdx)) {
            return false;
        }

        return true;
    }

    public void incrRow() {
        row++;
    }

    public int getRow() {
        return row;
    }

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

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDebugInput (boolean value) {
	debugInput = value;
    }

    public boolean getDebugInput () {
	return debugInput;
    }

    public void print(Object o) {
	getWriter().print(o);
    }

    public void println(Object o) {
	getWriter().println(o);
    }    

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

    public void setWriter(PrintWriter writer) {
	System.err.println("setWriter");
        this.writer = writer;
    }

    public String getFilepath(String filename) {
        String file = IOUtil.joinDir(getDestDir(), filename);
        files.add(file);

        return file;
    }

    public void addFile(String file) {
        files.add(file);
    }

    public List<String> getFiles() {
        return files;
    }

    public void print(String s) {
        if (getWriter() != null) {
            getWriter().print(s);
        }
    }

    public void flush() {
        if (writer != null) {
            writer.flush();
        }
    }

    public void close() {
        if (output != null) {
            IOUtil.close(output);
        }
    }

    public void setHeader(List header) {
        this.header = header;
    }

    public void initRow(Row row) {
        if (this.header == null) {
            this.header = row.getValues();
        }
        visitedRows++;
    }

    public void addHeaderLine(String line) {
        headerLines.add(line);
    }

    public List<String> getHeaderLines() {
        return headerLines;
    }

    public String formatValue(double value) {
        if (format != null) {
            return format.format(value);
        }

        return "" + value;
    }

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

    public void setInput(InputStream value) {
        this.setInput(new NamedInputStream("input", value));
    }

    public void setInput(NamedInputStream value) {
        input = value;
	reader = null;
	hasInput = true;
    }

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

    public InputStream getInputStream() throws Exception {
        if (input != null) {
            return input.getInputStream();
        } else {
            return null;
        }
    }

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

    public String getPrepend() {
        return prepend;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    public void setOutput(OutputStream value) {
        output = value;
    }

    public OutputStream getOutput() throws Exception {
        if ((output == null) && (outputFile != null)) {
            //This forces the mkdir
            getDestDir();
            files.add(outputFile.toString());
            output = new FileOutputStream(outputFile);
        }

        return output;
    }

    public void setOutputFile(File file) {
        outputFile = file;
	output = null;
	writer=null;
    }

    public File getOutputFile() {
        //This forces the mkdir
        if (outputFile != null) {
            getDestDir();
        }

        return outputFile;
    }

    public File setOutputFileSuffix(String suffix) {
        if (outputFile == null) {
            return null;
        }
        getDestDir();
        outputFile = new File(IOUtil.stripExtension(outputFile.toString())
                              + "." + suffix);

        return outputFile;
    }

    public void setMaxRows(int value) {
        maxRows = value;
    }

    public void setPruneBytes(int value) {
        pruneBytes = value;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxLineLength(int value) {
        maxLineLength = value;
    }

    public void setSkipRows(int value) {
        skipRows = value;
    }

    public int getSkipRows() {
        return skipRows;
    }

    public void setSkipLines(int value) {
        skipLines = value;
    }

    public int getSkipLines() {
        return skipLines;
    }

    public void setWidths(List<Integer> value) {
        widths = value;
    }

    public List<Integer> getWidths() {
        return widths;
    }

    public void setDelimiter(String value) {
        delimiter = value;
        if (delimiter != null) {
            if (delimiter.indexOf(">")>=0) {
		delimiters= new ArrayList<String>();
		for(String tok: Utils.split(delimiter,">",false,false)) {
		    if(tok.equals("tab")) tok = "\t";
		    else  if(tok.equals("space")) tok = " ";
		    delimiters.add(tok);
		}
	    } else if (delimiter.equals("?")) {
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

    public void setQuotesNotSpecial(boolean v) {
        quotesNotSpecial = v;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public List<String> getDelimiters() {
	return delimiters;
    }

    public void addTableProperty(String name, String value) {
        props.add(name);
        props.add(value);
    }

    public List getTableProperties() {
        return props;
    }

    public void setSheetsToShow(HashSet<Integer> value) {
        sheetsToShow = value;
    }

    public HashSet<Integer> getSheetsToShow() {
        return sheetsToShow;
    }

    public void setSearchFields(List<SearchField> value) {
        searchFields = value;
    }

    public List<SearchField> getSearchFields() {
        return searchFields;
    }

    public void setOutputDelimiter(String value) {
        outputDelimiter = value;
    }

    public String getOutputDelimiter() {
        return outputDelimiter;
    }

    public void addSearchExpression(String expr) {
        searchExpressions.add(expr);
    }

    public List<String> getSearchExpressions() {
        return searchExpressions;
    }

    public void setSplitOnSpaces(boolean value) {
        splitOnSpaces = value;
    }

    public boolean getSplitOnSpaces() {
        return splitOnSpaces;
    }

    public void setPositionStart(boolean value) {
        positionStart = value;
    }

    public boolean getPositionStart() {
        return positionStart;
    }

    public void setBounds(Bounds value) {
        bounds = value;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public void setUniqueHeader (boolean value) {
	uniqueHeader = value;
    }

    public boolean getUniqueHeader () {
	return uniqueHeader;
    }

    public void setDelimiterGuess (boolean value) {
	delimiterGuess = value;
    }

    public boolean getDelimiterGuess () {
	return delimiterGuess;
    }

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

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

package org.ramadda.util.text;


import org.ramadda.util.Utils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.io.File;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

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

    /** _more_ */
    private PrintWriter writer;

    /** _more_ */
    private File destDir = new File(".");

    /** _more_ */
    private InputStream input;

    /** _more_ */
    private String inputFile;

    /** _more_ */
    private BufferedReader prependReader;

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


    /** _more_ */
    private String delimiter = ",";

    /** _more_ */
    private boolean splitOnSpaces = false;


    /** _more_ */
    private List<Integer> widths;

    /** _more_ */
    private String comment = "#";

    /** _more_ */
    private String outputDelimiter = ",";

    /** _more_ */
    private int skip = 0;

    /** _more_ */
    private int visitedRows = 0;

    /** _more_ */
    private int maxRows = -1;

    /** _more_ */
    private int pruneBytes = 0;

    /** _more_ */
    private int maxLineLength = -1;

    /** _more_ */
    private int row = 0;

    /** _more_ */
    private List<String> skipStrings;

    /** _more_ */
    private List<String> changeStrings;

    /** _more_ */
    private Converter.ColumnSelector selector;

    /** _more_ */
    private Filter.FilterGroup filter = new Filter.FilterGroup();


    /** _more_ */
    private Processor.ProcessorGroup processor =
        new Processor.ProcessorGroup();

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
    private CsvOperator currentOperator;


    /**
     * _more_
     */
    public TextReader() {}

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
     * Set the CurrentOperator property.
     *
     * @param value The new value for CurrentOperator
     */
    public void setCurrentOperator(CsvOperator value) {
        currentOperator = value;
    }

    /**
     * Get the CurrentOperator property.
     *
     * @return The CurrentOperator
     */
    public CsvOperator getCurrentOperator() {
        return currentOperator;
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
     * Set the Comment property.
     *
     * @param value The new value for Comment
     */
    public void setComment(String value) {
        comment = value;
    }

    /**
     * Get the Comment property.
     *
     * @return The Comment
     */
    public String getComment() {
        return comment;
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
        return firstRow;
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
     * @param inputFile _more_
     * @param outputFile _more_
     * @param output _more_
     *
     * @return _more_
     *
     * @throws CloneNotSupportedException _more_
     */
    public TextReader cloneMe(InputStream input, String inputFile,
                              File outputFile, OutputStream output)
            throws CloneNotSupportedException {
        TextReader that = (TextReader) super.clone();
        that.input         = input;
        that.inputFile     = inputFile;
        that.output        = output;
        that.outputFile    = outputFile;
        that.writer        = null;
        that.skipStrings   = skipStrings;
        that.changeStrings = changeStrings;
        that.setPrepend(this.prepend);
        this.allData = this.allData;
        if (that.outputFile != null) {
            that.output = null;
        }

        return that;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getInputFile() {
        return inputFile;
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
        int cnt = 0;
        while (true) {
            int c;
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
                if (cnt > 0) {
                    //                    System.err.println("Skipped " + cnt + " null chars");
                }

                return c;
            }
            cnt++;
        }
    }

    /**
     * _more_
     *
     * @param info _more_
     * @param line _more_
     *
     * @return _more_
     */
    public boolean lineOk(TextReader info, String line) {
        if ((comment != null) && line.startsWith(comment)) {
            return false;
        }

        return true;
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

        lb.setLength(0);
        int           c;
        boolean       inQuote = false;
        StringBuilder sb      = new StringBuilder();
        boolean       debug   = true;
        debug = false;
        while (true) {
            if (debug && (lb.length() > 750)) {
                System.err.println("***** Whoa:" + lb);
                System.err.println("***" + sb);
            }

            if (nextChar >= 0) {
                c        = nextChar;
                nextChar = UNDEF;
                if (debug) {
                    sb.append("\tread from before:" + (char) c + "\n");
                }

            } else {
                c = readChar();
                if (debug) {
                    sb.append("\tread:" + (char) c + "\n");
                }
            }
            if (c == UNDEF) {
                String result = lb.toString();
                if (result.length() == 0) {
                    return null;
                }

                return result;
                //                break;
            }

            if (c == NEWLINE) {
                if (debug) {
                    sb.append("\tnew line:" + inQuote + "\n");
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
                            System.err.println("SKIPPPED:" + line);
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
                if (debug) {
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

            } else {}
            lb.append((char) c);
            //            sb.append("lb:" + lb+"\n");
            if (debug) {
                sb.append("\tchar:" + (char) c + "  inQuote: " + inQuote);
            }
            //"PMT2005-01570","3250 O""NEAL CR B","0079339","N&V","Residential","Single Family Attached Dwelling, Repair","Building","8450","0","05/02/2005","\

            if (c == QUOTE_DOUBLE) {
                if (debug) {
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
                    } else {
                        //we have ""
                        //                        nextChar = UNDEF;
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

        String line = lb.toString();

        //        if (line.length() == 0) {
        //            return null;
        //        }
        return line;
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
     *  Set the Selector property.
     *
     *  @param value The new value for Selector
     */
    public void setSelector(Converter.ColumnSelector value) {
        selector = value;
    }

    /**
     *  Get the Selector property.
     *
     *  @return The Selector
     */
    public Converter.ColumnSelector getSelector() {
        return selector;
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
     * @return _more_
     */
    public PrintWriter getWriter() {
        try {
            if (writer == null) {
                writer = new PrintWriter(this.getOutput());
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




    /**
     * Set the Input property.
     *
     * @param value The new value for Input
     */
    public void setInput(InputStream value) {
        input = value;
    }

    /**
     * Get the Input property.
     *
     * @return The Input
     */
    public InputStream getInput() {
        return input;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public BufferedReader getReader() {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(getInput()));
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
    public void setSkip(int value) {
        skip = value;
    }

    /**
     * Get the Skip property.
     *
     * @return The Skip
     */
    public int getSkip() {
        return skip;
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
            if (delimiter.equals("tab")) {
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
     * @return _more_
     */
    public Filter.FilterGroup getFilter() {
        return filter;
    }


    /**
     * Set the Processor property.
     *
     * @param value The new value for Processor
     */
    public void setProcessor(Processor.ProcessorGroup value) {
        processor = value;
    }

    /**
     * Get the Processor property.
     *
     * @return The Processor
     */
    public Processor.ProcessorGroup getProcessor() {
        return processor;
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



}

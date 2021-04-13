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

import org.json.*;

import org.ramadda.util.IO;
import org.ramadda.util.Json;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.NamedChannel;
import org.ramadda.util.MapProvider;
import org.ramadda.util.PropertyProvider;
import org.ramadda.util.Utils;
import org.ramadda.util.XlsUtil;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


import java.util.function.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.*;


/**
 *
 * @author Jeff McWhirter
 */
public class CsvUtil {

    /** _more_          */
    private static boolean debugFiles = false;

    /** _more_          */
    private static boolean debugArgs = false;

    private boolean interactive = false;
    
    /** _more_ */
    private List<String> args;

    /** _more_ */
    private OutputStream outputStream = System.out;

    /** _more_ */
    private InputStream inputStream;

    private ReadableByteChannel channel;
    

    /** _more_ */
    private File destDir = new File(".");

    /** _more_ */
    private TextReader myTextReader;

    /** _more_ */
    private String currentArg;

    /** _more_ */
    private Row currentRow;

    /** _more_ */
    private String errorDescription;

    /** _more_ */
    private File outputFile;

    /** _more_ */
    private Processor.DbXml dbXml;

    /** _more_ */
    private boolean okToRun = true;

    /** _more_ */
    private int rawLines = 0;

    /** _more_ */
    private String delimiter;

    /** _more_ */
    private String comment;

    private String dateFormatString = "yyyy-MM-dd HH:mm";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private String timezone="";
    
    /** _more_ */
    private List<String> changeTo = new ArrayList<String>();

    /** _more_ */
    private StringBuilder js = new StringBuilder();


    private PropertyProvider propertyProvider;
    private MapProvider mapProvider;    
    private List<String> inputFiles;
    private CsvContext csvContext;
    private List<DataSink> sinks;


    private boolean hasSink = false;

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(String[] args) throws Exception {
        this.args = new ArrayList<String>();
        for (String arg : args) {
            this.args.add(arg);
        }
        if (debugArgs) {
            System.out.println("Initial args");
            for (String arg : this.args) {
                System.out.println("Arg:" + arg);
            }
        }
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(List<String> args) throws Exception {
        this.args = args;
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(List<String> args, File destDir) throws Exception {
        this(args);
        this.destDir = destDir;
    }


    /**
     * _more_
     *
     * @param args _more_
     * @param out _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(List<String> args, OutputStream out, File destDir)
	throws Exception {
        this(args, destDir);
        this.outputStream = out;
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param out _more_
     * @param destDir _more_
     *
     * @throws Exception _more_
     */
    public CsvUtil(String[] args, OutputStream out, File destDir)
	throws Exception {
        this(args);
        this.destDir      = destDir;
        this.outputStream = out;
    }


    /**
     * _more_
     *
     * @param csvUtil _more_
     */
    public void initWith(CsvUtil csvUtil) {
        this.comment = csvUtil.comment;
        this.js      = csvUtil.js;
	this.sinks = csvUtil.sinks;
        //        this.delimiter = csvUtil.delimiter;
    }

    public List<String> getInputFiles() {
	return inputFiles;
    }


    /**
       Set the Context property.

       @param value The new value for Context
    **/
    public void setCsvContext (CsvContext value) {
	csvContext = value;
	sinks = new ArrayList<DataSink>();
	try {
	    for(Class c: csvContext.getClasses()) {
		if (DataSink.class.isAssignableFrom(c)) {
		    DataSink sink = (DataSink) c.newInstance();
		    sinks.add(sink);
		}

	    }
	} catch(Exception exc) {
	}
    }

    /**
       Get the Context property.

       @return The Context
    **/
    public CsvContext getCsvContext () {
	return csvContext;
    }





    /**
       Set the PropertyProvider property.
       @param value The new value for PropertyProvider
    **/
    public void setPropertyProvider (PropertyProvider value) {
	propertyProvider = value;
    }

    /**
       Get the PropertyProvider property.
       @return The PropertyProvider
    **/
    public PropertyProvider getPropertyProvider () {
	return propertyProvider;
    }

    public MapProvider getMapProvider () {
	return mapProvider;
    }


    public void setMapProvider (MapProvider mp) {
	mapProvider = mp;
    }
    
    public String getProperty(String name, String dflt) {
	String v = getProperty(name);
	if(v==null) return dflt;
	return v;
    }


    public String getProperty(String name) {
	if(propertyProvider!=null) {
	    String value =  propertyProvider.getProperty(name,null);
	    if(value!=null) return value; 
	} else {
	    System.err.println("CsvUtil.getProperty: no PropertyProvider set");
	}
	return System.getenv(name);
    }

    /**
       Set the Interactive property.

       @param value The new value for Interactive
    **/
    public void setInteractive (boolean value) {
	interactive = value;
    }

    /**
       Get the Interactive property.

       @return The Interactive
    **/
    public boolean getInteractive () {
	return interactive;
    }


    /**
     * _more_
     *
     * @param inputStream _more_
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setChannel(ReadableByteChannel channel)  {
        this.channel = channel;
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
     */
    public void stopRunning() {
        okToRun = false;
        if (myTextReader != null) {
            myTextReader.stopRunning();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getErrorDescription() {
        return errorDescription;
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
     * @param file _more_
     */
    public void setOutputFile(File file) {
        this.outputFile = file;
        if (file != null) {
            this.outputStream = null;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputStream getOutputStream() throws Exception {
        if (this.outputStream == null) {
            if (myTextReader != null) {
                myTextReader.getDestDir();
                myTextReader.addFile(outputFile.toString());
            }
            this.outputStream = makeOutputStream(outputFile.toString());
        }

        return this.outputStream;
    }


    /**
     * _more_
     *
     * @param out _more_
     */
    public void setOutputStream(OutputStream out) {
        this.outputStream = out;
    }

    /**
     * _more_
     *
     * @param files _more_
     *
     * @throws Exception _more_
     */
    public void run(List<String> files) throws Exception {
        try {
            runInner(files);
        } catch (Exception exc) {
            CsvOperator op = (myTextReader == null)
		? null
		: myTextReader.getCurrentOperator();
            if (op != null) {
                errorDescription = "Error processing text with operator: "
		    + op.getDescription();
            } else if (currentArg != null) {
                errorDescription = "Error processing argument:" + currentArg;
            }
            if (currentRow != null) {
                errorDescription += "\nRow:" + currentRow.getValues() + "\n";

            }
            throw exc;
        }
    }

    /**
     * _more_
     *
     * @param files _more_
     *
     * @throws Exception _more_
     */
    private void runInner(List<String> files) throws Exception {

        if (files == null) {
            files = new ArrayList<String>();
        }
	this.inputFiles = files;
        boolean      doConcat      = false;
        boolean      doHeader      = false;
        boolean      doRaw         = false;
        int          rawCut        = 0;
        Hashtable    dbProps       = new Hashtable<String, String>();
        boolean      doPoint       = false;
        String       iterateColumn = null;
        List<String> iterateValues = new ArrayList<String>();

        String       prepend       = null;
        myTextReader = new TextReader(destDir, outputFile, outputStream);

        boolean      printArgs = false;
        List<String> extra     = new ArrayList<String>();
	boolean doArgs = false;
	int                doArgsCnt     = 0;
	int                doArgsIndex   = 1;
	PrintWriter pw        = null;
	//	System.err.println("args:" + args);
	//Check for the -args command
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
	    if(arg.equals("-args")) {
		doArgs = true;
		continue;
	    }
	}

	//	System.err.println("args:" + args);
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
	    if(arg.equals("-args")) {
		doArgs = true;
		continue;
	    }
	    if (doArgs) {
		if (pw == null) {
		    pw = new PrintWriter(getOutputStream());
		    pw.print("csvcommands1=");
		} else {
		    doArgsCnt++;
		    if (doArgsCnt > 4) {
			pw.print("\n");
			doArgsCnt = 0;
			doArgsIndex++;
			pw.print("csvcommands" + doArgsIndex + "=");
		    } else {
			pw.print(",");
		    }
		}
		arg = arg.replaceAll(",", "\\\\,");
		//		System.err.println("arg:" + arg);
		pw.print(arg);
		continue;
	    }



            //      System.out.println("ARG:" + arg);
            //      if(true) continue;
            if (arg.equals("-printargs")) {
                System.out.print("java  org.ramadda.util.text.CsvUtil ");
                printArgs = true;

                continue;
            }
            if (printArgs) {
                if ( !arg.startsWith("\"") && !arg.startsWith("-")) {
                    System.out.print("\"" + arg + "\" ");
                } else {
                    System.out.print(arg + " ");
                }
            }
            if (arg.equals("-help")) {
                usage("", null);
                return;
            }
            if (arg.equals("-genhelp")) {
                genHelp();
                return;
            }
            if (arg.equals("-helpraw")) {
                usage("", null, "-raw", "true");
                return;
            }
            if (arg.equals("-helpjson")) {
                usage("", null, "-json", "true");
                return;
            }
            if (arg.startsWith("-help:")) {
                usage("", arg.substring("-help:".length()));

                return;
            }
            if (arg.equals("-alldata")) {
                myTextReader.setAllData(true);
                continue;
            }

            if (arg.equals("-verbose")) {
                myTextReader.setVerbose(true);
                continue;
            }


            if (arg.equals("-cat")) {
                doConcat = true;
                continue;
            }

            if (arg.equals("-raw")) {
                doRaw = true;

                continue;
            }

            if (arg.equals("-commentChar")) {
                myTextReader.setCommentChar(args.get(++i));
                continue;
            }

            if (arg.startsWith("-header")) {
                myTextReader.setFirstRow(
					 new Row(Utils.split(args.get(++i), ",")));
                continue;
            }

            if (arg.startsWith("-iter")) {
                iterateColumn = args.get(++i);
                iterateValues = Utils.split(args.get(++i), ",");

                continue;
            }
            extra.add(arg);
        }


	if (doArgs) {
	    if (pw != null) {
		pw.print("\n");
	    }
	    pw.close();
	    return;
	}



        if ( !parseArgs(extra, myTextReader, files)) {
            currentArg = null;
            return;
        }
        currentArg = null;
        if (printArgs) {
            for (String f : files) {
                System.out.print(f + " ");
            }
            if (files.size() == 0) {
                System.out.print(" ${1} ");
            }
            System.out.println("");
        }

        if (doConcat) {
            IO.concat(files, getOutputStream());
        } else if (doHeader) {
            header(files, myTextReader, doPoint);
        } else if (doRaw) {
            raw(files, myTextReader);
        } else {
	    List<DataProvider> providers = myTextReader.getProviders();
            if (providers.size() == 0) {
                providers.add(new DataProvider.CsvDataProvider(myTextReader, rawLines));
            }
            Filter.PatternFilter iteratePattern = null;
            if (iterateColumn == null) {
                iterateValues.add("dummy");
            } else {
                iteratePattern = new Filter.PatternFilter(iterateColumn, "");
                myTextReader.getProcessor().addProcessor(iteratePattern);
            }
            for (int i = 0; i < iterateValues.size(); i++) {
                String pattern = iterateValues.get(i);
                if (iteratePattern != null) {
                    iteratePattern.setPattern(pattern);
                }
		boolean newWay = true;
		//		newWay=false;
                for (DataProvider provider : providers) {
		    if(newWay) {
			for (NamedChannel input : getChannels(files)) {
			    myTextReader.getProcessor().reset();
			    TextReader clone = myTextReader.cloneMe(input,
								    outputFile, outputStream);
			    process(clone, provider);
			    input.close();
			    provider.finish();
			}
		    } else {
			for (NamedInputStream input : getStreams(files)) {
			    myTextReader.getProcessor().reset();
			    TextReader clone = myTextReader.cloneMe(input,
								    outputFile, outputStream);
			    process(clone, provider);
			    input.close();
			    provider.finish();
			}
		    }
                }
            }
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void process(TextReader ctx) throws Exception {
        DataProvider.CsvDataProvider provider =
            new DataProvider.CsvDataProvider(0);
        process(ctx, provider);
    }


    /**
     * Run through the csv file in the TextReader
     *
     * @param ctx Holds input, output, skip, delimiter, etc
     * @param provider _more_
     *
     * @throws Exception On badness
     */
    public void process(TextReader ctx, DataProvider provider)
	throws Exception {
	provider.initialize(this, ctx);
        try {
            errorDescription = null;
            processInner(ctx, provider);
        } catch (Exception exc) {
            CsvOperator op = (ctx == null)
		? null
		: ctx.getCurrentOperator();
            System.err.println("error:" + op);
            if (op != null) {
                errorDescription = "Error processing text with operator: "
		    + op.getDescription();
            }
            throw exc;
        }
    }


    /**
     * _more_
     *
     * @param provider _more_
     *
     * @throws Exception _more_
     */
    private void processInner(TextReader ctx, DataProvider provider)
	throws Exception {
        int rowCnt   = 0;
        Row firstRow = ctx.getFirstRow();
        ctx.setFirstRow(null);
        if (firstRow != null) {
            processRow(ctx, firstRow);
            rowCnt++;
        }
        Row row;
        while ((row = provider.readRow()) != null) {
	    if(row==null) break;
            rowCnt++;
            if (rowCnt <= ctx.getSkip()) {
                continue;
            }
            if ( !processRow(ctx, row)) {
                break;
            }
        }
        if (okToRun) {
            if (ctx.getProcessor() != null) {
                ctx.getProcessor().finish(ctx, null);
            }
        }
        ctx.flush();
        ctx.close();
    }

    /**
     * _more_
     *
     * @param row _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean processRow(TextReader ctx, Row row)
	throws Exception {
        if ((ctx.getFilter() != null)) {
            if ( !ctx.getFilter().rowOk(ctx, row)) {
                return true;
            }
        }

        ctx.initRow(row);
        if ((ctx.getMaxRows() >= 0)
	    && (ctx.getVisitedRows() > ctx.getMaxRows())) {
            return false;
        }
        if (ctx.getProcessor() != null) {
            ctx.setCurrentOperator(null);
            currentRow = row;
            row        = ctx.getProcessor().processRow(ctx,
						       row);
            currentRow = null;
            if ( !ctx.getOkToRun()) {
                return false;
            }
            if (ctx.getExtraRow() != null) {
                row = ctx.getProcessor().processRow(ctx,
						    ctx.getExtraRow());
                ctx.setExtraRow(null);
            }
            if ( !ctx.getOkToRun()) {
                return false;
            }
        } else {
            ctx.getWriter().println(columnsToString(row.getValues(),
						    ctx.getOutputDelimiter()));
            ctx.getWriter().flush();
        }
        ctx.incrRow();
        return true;
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
    private List<NamedInputStream> getStreams(List<String> files)
	throws Exception {
        if (debugFiles) {
            System.err.println("getStreams:" + files);
        }
        ArrayList<NamedInputStream> streams =
            new ArrayList<NamedInputStream>();
        for (String file : files) {
            streams.add(new NamedInputStream(file, makeInputStream(file)));
        }
        if (inputStream != null) {
            streams.add(new NamedInputStream("input", inputStream));
        }
        if (streams.size() == 0) {
            streams.add(new NamedInputStream("stdin", System.in));
        }
        return streams;
    }


    private List<NamedChannel> getChannels(List<String> files)
	throws Exception {
        if (debugFiles) {
            System.err.println("getChannels:" + files);
        }
        ArrayList<NamedChannel> channels=
            new ArrayList<NamedChannel>();
        for (String file : files) {
	    if (file.toLowerCase().endsWith(".xls")) {
		String csv = XlsUtil.xlsToCsv(file);
		InputStream bais=  new ByteArrayInputStream(csv.getBytes());
		ReadableByteChannel in = Channels.newChannel(bais);
		channels.add(new NamedChannel(file, in));
		continue;
	    }
	    if (file.toLowerCase().endsWith(".xlsx")) {
		String csv = XlsUtil.xlsxToCsv(file);
		InputStream bais=  new ByteArrayInputStream(csv.getBytes());
		ReadableByteChannel in = Channels.newChannel(bais);
		channels.add(new NamedChannel(file, in));
		continue;
	    }

	    if (file.toLowerCase().endsWith(".gz") || file.toLowerCase().endsWith(".gzip")) {
		InputStream is = new GZIPInputStream(new FileInputStream(file));
		ReadableByteChannel in = Channels.newChannel(is);
		channels.add(new NamedChannel(file, in));
		continue;
	    }

            channels.add(new NamedChannel(file, new RandomAccessFile(file,"r").getChannel()));
        }
        if (inputStream != null) {
	    ReadableByteChannel in = Channels.newChannel(inputStream);
	    channels.add(new NamedChannel("input", in));
        }
        if (channel != null) {
            channels.add(new NamedChannel("input", channel));
	    
        }	
        if (channels.size() == 0) {
	    FileInputStream stdin = new FileInputStream(FileDescriptor.in);
	    FileChannel stdinChannel = stdin.getChannel();
            channels.add(new NamedChannel("stdin", stdinChannel));
        }
        return channels;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getNewFiles() {
        return myTextReader.getFiles();
    }

    public TextReader getContext() {
	return myTextReader;
    }



    /**
     *  Check if this is an OK path to write to
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputStream makeOutputStream(String file) throws Exception {
	if(!IO.okToWriteTo(file)) {
	    throw new IllegalArgumentException("Cannot write to file:"
					       + file);
	}
        return new FileOutputStream(file);
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public InputStream makeInputStream(String file) throws Exception {
	if(!IO.okToReadFrom(file)) {
	    throw new IllegalArgumentException("Cannot read file:"
					       + file);
        }

        if (file.endsWith(".xls")) {
            String csv = XlsUtil.xlsToCsv(file);
            return new BufferedInputStream(
					   new ByteArrayInputStream(csv.getBytes()));
	} else if (file.endsWith(".xlsx")) {
            String csv = XlsUtil.xlsxToCsv(file);
            return new BufferedInputStream(
					   new ByteArrayInputStream(csv.getBytes()));
        } else if (file.toLowerCase().endsWith(".zip")) {
            InputStream    fis = IO.getInputStream(file.toString());
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
                if (p.endsWith("export.xml")) {
                    return zin;
                }
            }
        } else {
            if (new File(file).exists()) {
                try {
                    return new BufferedInputStream(new FileInputStream(file));
                } catch (Exception exc) {
                    System.err.println("Error opening file:" + file);
                    throw exc;
                }
            }
            return IO.getInputStream(file);
        }
        return null;
    }


    /**
     * _more_
     *
     * @param files _more_
     * @param info _more_
     * @param asPoint _more_
     *
     * @throws Exception _more_
     */
    public void header(List<String> files, TextReader info, boolean asPoint)
	throws Exception {
        PrintWriter   writer    = info.getWriter();
        List<Integer> widths    = info.getWidths();
        String        delimiter = info.getDelimiter();
        if ((widths == null) && (delimiter == null)) {
            delimiter = ",";
        }
        List<BufferedReader> readers = new ArrayList<BufferedReader>();
        for (String file : files) {
            readers.add(
			new BufferedReader(
					   new InputStreamReader(makeInputStream(file))));
        }
        for (BufferedReader br : readers) {
            String line = new TextReader(br).readLine();
            if (line == null) {
                continue;
            }
            List<String> cols = (widths != null)
		? Utils.tokenizeColumns(line, widths)
		: myTextReader.getSplitOnSpaces()
		? Utils.split(line, " ", true, true)
		: Utils.tokenizeColumns(line, delimiter);
            if (asPoint) {
                writer.println("skiplines=1");
                writer.print("fields=");
            }
            for (int i = 0; i < cols.size(); i++) {
                String col = cols.get(i).trim();
                col = col.replaceAll("\n", " ");
                if (asPoint) {
                    if (i > 0) {
                        writer.print(", ");
                    }
                    String label =
                        Utils.makeLabel(col.replaceAll("\\([^\\)]+\\)", ""));
                    String unit = StringUtil.findPattern(col,
							 ".*?\\(([^\\)]+)\\).*");
                    //                    System.err.println ("COL:" + col +" unit: " + unit);
                    StringBuffer attrs = new StringBuffer();
                    attrs.append("label=\"" + label + "\" ");
                    if (unit != null) {
                        attrs.append("unit=\"" + unit + "\" ");

                    }
                    String id = label.replaceAll(
						 "\\([^\\)]+\\)", "").replaceAll(
										 "-", "_").trim().toLowerCase().replaceAll(
															   " ", "_").replaceAll(":", "_");
                    id = id.replaceAll("/+", "_");
                    id = id.replaceAll("\\.", "_");
                    id = id.replaceAll("_+_", "_");
                    id = id.replaceAll("_+$", "");
                    id = id.replaceAll("^_+", "");
                    if (id.indexOf("date") >= 0) {
                        attrs.append("type=\"date\" format=\"\" ");
                    }

                    writer.print(id + "[" + attrs + "] ");
                } else {
                    writer.println("#" + i + " " + col);
                }
            }
            if (asPoint) {
                writer.println("");
            }
        }
        System.err.println("CsvUtil.done");
        writer.flush();
        writer.close();
    }

    /**
     *     _more_
     *
     *     @param files _more_
     *     @param info _more_
     *
     *     @throws Exception _more_
     */
    public void raw(List<String> files, TextReader ctx) throws Exception {
        int         numLines    = ctx.getMaxRows();
        PrintWriter writer      = ctx.getWriter();
        String      prepend     = ctx.getPrepend();
        int         chars       = 0;
        int         LINE_LIMIT  = 2000;
        int         CHARS_LIMIT = 3000000;
        for (String file : files) {
            int                  lineCnt = 0;
            List<BufferedReader> readers = new ArrayList<BufferedReader>();
            if (ctx.getPrepend() != null) {
                readers.add(
			    new BufferedReader(
					       new InputStreamReader(
								     new ByteArrayInputStream(
											      ctx.getPrepend().getBytes()))));
            }

            readers.add(
			new BufferedReader(
					   new InputStreamReader(makeInputStream(file))));
            for (BufferedReader br : readers) {
                while ((lineCnt < numLines) && (chars < CHARS_LIMIT)) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
		    //                    System.err.println("line:" + line.length());
                    while (line.length() > LINE_LIMIT) {
                        String tmp = line.substring(0, LINE_LIMIT - 1);
                        line = line.substring(LINE_LIMIT - 1);
                        lineCnt++;
                        writer.println(tmp);
                        chars += tmp.length();
                        if (chars > CHARS_LIMIT) {
                            break;
                        }
                    }
                    if (chars > CHARS_LIMIT) {
                        break;
                    }
                    writer.println(line);
                    lineCnt++;
                    chars += line.length();
		    //                    System.err.println("chars:" + chars + " lines:" + lineCnt + " max lines:" + numLines);
                }
                br.close();
            }

        }
        writer.flush();
        writer.close();
    }






    /**
     * _more_
     *
     * @param props _more_
     * @param colId _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getDbProp(Hashtable<String, String> props,
                                   String colId, String prop, String dflt) {
        String key   = (colId == null)
	    ? prop
	    : colId + "." + prop;
        String value = props.get("-" + key);
        if (value == null) {
            value = props.get(key);
        }
        if (value != null) {
            return value;
        }

        return dflt;
    }


    /**
     * _more_
     *
     * @param props _more_
     * @param colId _more_
     * @param index _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getDbProp(Hashtable<String, String> props,
                                   String colId, int index, String prop,
                                   String dflt) {
        String value = getDbProp(props, colId, prop, null);
        if (value != null) {
            return value;
        }

        return getDbProp(props, index + "", prop, dflt);
    }


    /**
     * _more_
     *
     * @param props _more_
     * @param colId _more_
     * @param prop _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static boolean getDbProp(Hashtable<String, String> props,
                                    String colId, String prop, boolean dflt) {
        String value = getDbProp(props, colId, prop, (String) null);
        if (value == null) {
            return dflt;
        }
        return value.equals("true");
    }


    /**
     * _more_
     *
     * @param cols _more_
     * @param delimiter _more_
     *
     * @return _more_
     */
    public static String columnsToString(List cols, String delimiter) {
        return columnsToString(cols, delimiter, false);
    }

    /**
     * _more_
     *
     * @param cols _more_
     * @param delimiter _more_
     * @param addNewLine _more_
     *
     * @return _more_
     */
    public static String columnsToString(List cols, String delimiter,
                                         boolean addNewLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            Object o = cols.get(i);
            String s = ((o == null)
                        ? ""
                        : o.toString());
            if (i > 0) {
                sb.append(delimiter);
            }
            boolean needToQuote = false;
            if (s.indexOf("\n") >= 0) {
                needToQuote = true;
            } else if (s.indexOf(delimiter) >= 0) {
                needToQuote = true;
            }

            if (s.indexOf("\"") >= 0) {
                s           = s.replaceAll("\"", "\"\"");
                needToQuote = true;
            }

            if (needToQuote) {
                sb.append('"');
                sb.append(s);
                sb.append('"');
            } else {
                sb.append(s);
            }
        }
        if (addNewLine) {
            sb.append("\n");
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String cleanColumnValue(String s) {
        return cleanColumnValue(s, ",");
    }

    /**
     * _more_
     *
     * @param s _more_
     * @param delimiter _more_
     *
     * @return _more_
     */
    public static String cleanColumnValue(String s, String delimiter) {
        boolean needToQuote = false;
        if (s.indexOf("\n") >= 0) {
            needToQuote = true;
        } else if (s.indexOf(delimiter) >= 0) {
            needToQuote = true;
        }

        if (s.indexOf("\"") >= 0) {
            s           = s.replaceAll("\"", "\"\"");
            needToQuote = true;
        }

        if (needToQuote) {
            return '"' + s + '"';
        }

        return s;
    }

    /**
     * Class description
     *
     *
     * @version        $version$, Tue, Mar 24, '20
     * @author         Enter your name here...
     */
    public static class Arg {

        /** _more_ */
        String id;

        /** _more_ */
        String desc;

        /** _more_ */
        String[] props;

        /**
         * _more_
         *
         * @param id _more_
         */
        public Arg(String id) {
            this(id, "");
        }

        /**
         * _more_
         *
         * @param id _more_
         * @param desc _more_
         * @param props _more_
         */
        public Arg(String id, String desc, String... props) {
            if ((desc.length() == 0) && id.equals("columns")) {
                desc = "Column indices, one per line.<br>Can include ranges, e.g. 0-5";
            }
            if ((desc.length() == 0) && id.equals("rows")) {
                desc = "Row indices, one per line.<br>Can include ranges, e.g. 0-5";
            }
            this.id    = id;
            this.desc  = desc;
            this.props = props;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Sun, Apr 5, '20
     * @author         Enter your name here...
     */
    public static class Label {

        /** _more_ */
        String label;

        /**
         * _more_
         *
         * @param l _more_
         */
        public Label(String l) {
            this.label = l;
        }
    }


    /**
     * Class description
     *
     *
     * @version        $version$, Wed, Feb 20, '19
     * @author         Enter your name here...
     */
    public static class Cmd {

        /** _more_ */
        boolean category;

        /** _more_ */
        String cmd;

        /** _more_ */
        String label;

        /** _more_ */
        List<Arg> args = new ArrayList<Arg>();

        /** _more_ */
        String desc;

        /**
         * _more_
         *
         * @param isCat _more_
         * @param category _more_
         */
        public Cmd(boolean isCat, String category) {
            this.category = isCat;
            this.desc     = category;
        }


        /**
         * _more_
         *
         * @param cmd _more_
         * @param args _more_
         * @param desc _more_
         */
        public Cmd(String cmd, String desc, Object... args) {
            this(cmd, (Label) null, desc, args);
        }

        /**
         * _more_
         *
         * @param cmd _more_
         * @param label _more_
         * @param desc _more_
         * @param args _more_
         */
        public Cmd(String cmd, Label label, String desc, Object... args) {
            if (label != null) {
                this.label = label.label;
            }
            this.cmd  = cmd;
            this.desc = desc;
            for (Object obj : args) {
                if ( !(obj instanceof Arg)) {
                    obj = new Arg(obj.toString(), "");
                }
                this.args.add((Arg) obj);
            }
        }

        /**
         * _more_
         *
         * @param s _more_
         *
         * @return _more_
         */
        public boolean match(String s) {
            return cmd.indexOf(s) >= 0;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getLine() {
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (Arg arg : args) {
                    sb.append("<" + arg.id + ((arg.desc.length() > 0)
					      ? " " + arg.desc
					      : "") + "> ");
                }
            }

            return cmd + " " + sb + " (" + desc + ")";
        }
    }


    /** _more_ */
    private static final Cmd[] commands = {
        new Cmd("-help", "print this help)"),
        new Cmd("-help:<topic search string>",
                "print help that matches topic"),

        /** * Input   * */
        new Cmd(true, "Input"),
        new Cmd("-delimiter", "Specify a delimiter",
                new Arg("delimiter", "Use 'space' for space, 'tab' for tab",
                        "size", "5")),
        new Cmd("-tab", "Use tabs"),
        new Cmd("-widths", "Columns are fixed widths",
                new Arg("widths", "w1,w2,...,wN")),
        new Cmd("-header", "Raw header",
                new Arg("header", "Column names", "type", "list")),
        new Cmd("-html", "Parse the table in the input html file",
                new Arg("skip", "Number of tables to skip", "type",
			"number"), new Arg("pattern", "Pattern to skip to",
					   "type", "pattern", "size",
					   "40"), new Arg("properties",
							  "Other attributes - <br>&nbsp;&nbsp;removeEntity false removePattern pattern",
							  "rows", "6", "size", "40")),
        new Cmd("-htmlpattern", new Label("Extract from html"),
                "Parse the input html file",
                new Arg("columns", "Column names", "type", "columns"),
                new Arg("startPattern", "", "type", "pattern"),
                new Arg("endPattern", "", "type", "pattern"),
                new Arg("pattern", "Row pattern. Use (...) to match columns",
                        "type", "pattern")),
        new Cmd("-json", "Parse the input as json",
                new Arg("arrayPath",
			"Path to the array e.g., obj1.arr[2].obj2", "size", "30",
			"label", "Array path"), new Arg("objectPaths",
							"One or more paths to the objects e.g. geometry,features",
							"size", "30", "label", "Object paths", "type",
							"list", "size", "30")),
        new Cmd("-lines", "Parse the input as text lines"),
        new Cmd("-xml", "Parse the input as xml",
                new Arg("path", "Path to the elements", "size", "60")),
        new Cmd("-shapefile", "Parse the input shapefile",
                new Arg("props", "addPoints true addShapes false")),	
        new Cmd("-text", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("chunk pattern", "", "type", "pattern"),
                new Arg("token pattern", "", "type", "pattern")),
        new Cmd("-text2", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("chunk pattern", "", "type", "pattern"),
                new Arg("token pattern", "", "type", "pattern")),
        new Cmd("-text3", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("token pattern", "", "type", "pattern")),
        new Cmd("-tokenize", "Tokenize the input from the pattern",
                new Arg("header", "header1,header2..."),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-sql", "Connect to the given database",
                new Arg("db", "The database id (defined in the environment)","type","enumeration","values","property:csv_dbs"),
		new Arg("table", "Comma separate list of tables to select from","size","60"),
		new Arg("columns", "Comma separated list of columns to select"),
		new Arg("where", "column1 expr value\ncolumn2 expr value\n...\nWhere expr is: =|<|>|<>|like|notlike","type","rows","delimiter",";","size","60"),				
		new Arg("properties", "name space value properties. e.g., join col1,col2")),
        new Cmd("-prune", "Prune out the first N bytes",
                new Arg("bytes", "Number of leading bytes to remove", "type",
                        "number")),
        new Cmd("-deheader", new Label("Remove the  header"),
		"Strip off the point header"),
        new Cmd("-cat", "One or more csv files", "*.csv"),


        /** *  Slice and dice * */
        new Cmd(true, "Slice and Dice"),
        new Cmd("-columns", new Label("Select columns"),
                "Only include the given columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-notcolumns", new Label("Deselect columns"),
                "Don't include given columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-delete", new Label("Delete columns"), "Remove the columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-cut", new Label("Drop rows"), "",
                new Arg("rows",
                        "One or more rows. -1 to the end. e.g., 0-3,5,10,-1",
                        "type", "rows")),
        new Cmd("-include", new Label("Include rows"),
                "Only include specified rows",
                new Arg("rows", "one or more rows, -1 to the end", "type",
                        "rows")),
        new Cmd("-skip", "Skip number of rows",
                new Arg("rows", "How many rows to skip", "type", "number")),
        new Cmd("-copy", new Label("Copy column"), "",
                new Arg("column", "", "type", "column"), "name"),
        new Cmd(
		"-insert", new Label("Insert column"),
		"Insert new column values",
		new Arg("column", "Column to insert after", "type", "column"),
		new Arg(
			"values",
			"Single value or comma separated for multiple rows", "type",
			"list")),
        new Cmd("-concat", "Create a new column from the given columns",
                new Arg("columns", "", "type", "columns"), "delimiter"),
        new Cmd("-split", "Split the column",
                new Arg("column", "", "type", "column"),
                new Arg("delimiter", "What to split on"),
                new Arg("names", "Comma separated new column names", "type",
                        "list")),
        new Cmd("-splat",
                "Create a new column from the values in the given column",
                "key col", new Arg("column", "", "type", "column"),
                "delimiter", new Arg("name", "new column name")),
        new Cmd("-shift", new Label("Shift columns"),
                "Shift columns over by count for given rows",
                new Arg("rows", "Rows to apply to", "type", "rows"),
                new Arg("column", "Column to start at", "type", "column"),
                new Arg("count")),
        new Cmd("-addcell", new Label("Add cell"),
                "Add a new cell at row/column", new Arg("row"),
                new Arg("column", "", "type", "column"), "value"),
        new Cmd("-deletecell", new Label("Delete cell"),
                "Delete cell at row/column", new Arg("row"),
                new Arg("column", "", "type", "column")),
        new Cmd("-mergerows", new Label("Merge rows"), "",
                new Arg("rows", "2 or more rows", "type", "rows"),
                new Arg("delimiter"), new Arg("close")),
        new Cmd("-rowop", new Label("Row Operator"),
                "Apply an operator to columns and merge rows",
                new Arg("keys", "Key columns", "type", "columns"),
                new Arg("values", "Value columns", "type", "columns"),
                new Arg("operator", "Operator", "values",
                        "average,min,max,count")),
        new Cmd("-rotate", "Rotate the data"),
        new Cmd("-flip", "Reverse the order of the rows except the header"),
        new Cmd("-unfurl", "Make columns from data values",
                new Arg("column", "column to get new column header#", "type",
			"column"), new Arg("value columns",
					   "Columns to get values from", "type",
					   "columns"), new Arg("unique column",
							       "The unique value, e.g. date", "type",
							       "column"), new Arg("other columns",
										  "Other columns to include", "type",
										  "columns")),
        new Cmd("-furl", "Use values in header to make new row",
                new Arg("columns", "", "type", "columns"), "header label",
                "value label"),
        new Cmd("-explode", "Make separate files based on value of column",
                new Arg("column", "", "type", "column")),
        new Cmd("-join", "Join the 2 files together",
                new Arg("key columns", "", "type", "columns"),
                new Arg("value_columns", "value columns"),
                new Arg("file", "File to join with", "type", "file"),
                new Arg("source_columns", "source key columns")),
        new Cmd("-dissect", "Make fields based on patterns",
                new Arg("column", "", "type", "column"), new Arg("pattern","e.g., \"(field1:.*) (field2:.*) ...\"","type","pattern","size","80")),
        new Cmd("-keyvalue", "Make fields from key/value pairs, e.g. name1=value1 name2=value2 ...",
                new Arg("column", "", "type", "column")),
        /** *  Filter * */
        new Cmd(true, "Filter"),
        new Cmd("-start", "Start at pattern in source file",
                new Arg("start pattern", "", "type", "pattern")),
        new Cmd("-stop", "End at pattern in source file",
                new Arg("stop pattern", "", "type", "pattern")),
        new Cmd("-rawlines", "",
                new Arg("lines",
                        "How many lines to pass through unprocesed")),
        new Cmd(
		"-min",
		"Only pass thorough lines that have at least this number of columns",
		new Arg("min # columns", "", "type", "number")),
        new Cmd(
		"-max",
		"Only pass through lines that have no more than this number of columns",
		new Arg("max # columns", "", "type", "number")),
        new Cmd("-pattern", "Pass through rows that match the pattern",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-notpattern",
                "Pass through rows that don't match the pattern",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-unique", "Pass through unique values", new Arg("columns")),
        new Cmd("-dups", new Label("Duplicate values"),
                "Pass through duplicate values", new Arg("columns")),
        new Cmd("-maxvalue", new Label("Max value"), "", "key column",
                "value column"),
        new Cmd("-eq", new Label("Equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-ne", new Label("Not equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-gt", new Label("Greater than"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-ge", new Label("Greater than/equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-lt", new Label("Less than"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-le", new Label("Less than/equals"),
                "Extract rows that pass the expression",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-groupfilter", new Label("Group filter"),
                "One row in each group has to match",
                new Arg("column", "", "type", "column"),
                new Arg("value_column", "Value column", "type", "column"),
                new Arg("operator", "", "values", "=,!=,~,<,<=,>,>="),
                new Arg("value")),
        new Cmd("-before", new Label("Before date"), "",
                new Arg("column", "", "type", "column"), new Arg("format"),
                new Arg("date"), new Arg("format2")),
        new Cmd("-after", new Label("After date"), "",
                new Arg("column", "", "type", "column"), new Arg("format"),
                new Arg("date"), new Arg("format2")),
        new Cmd("-latest", new Label("Latest date"), "",
                new Arg("columns", "Key columns", "type", "columns"),
                new Arg("column", "Date column", "type", "column"),
                new Arg("format")),
        new Cmd("-countvalue", new Label("Max unique values"),
                "No more than count unique values",
                new Arg("column", "", "type", "column"), new Arg("count")),
        new Cmd("-decimate", "only include every <skip factor> row",
                new Arg("rows", "# of start rows to include"),
                new Arg("skip", "skip factor")),
        new Cmd("-skipline", "Skip any line that matches the pattern",
                new Arg("pattern", "", "type", "pattern")),

        /** *  Change values * */
        new Cmd(true, "Change"),
        new Cmd("-change", "Change columns",
                new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("substitution string",
                        "use $1, $2, etc for pattern (...) matches")),
        new Cmd("-changerow", "Change the values in the row/cols",
                new Arg("rows"), new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("substitution string")),
        new Cmd("-set", "Write the value into the cells",
                new Arg("columns", "", "type", "columns"),
                new Arg("rows", "", "type", "list"), new Arg("value")),
        new Cmd(
		"-macro",
		"Look for the pattern in the header and apply the template to make a new column, template: '{1} {2} ...', use 'none' for column name for no header",
		new Arg("pattern", "", "type", "pattern"), new Arg("template"),
		new Arg("column label")),
        new Cmd(
		"-setcol",
		"Write the value into the write col for rows that match the pattern",
		new Arg("column", "match col #", "type", "column"),
		new Arg("pattern", "", "type", "pattern"),
		new Arg("write column", "", "type", "column"), new Arg("value")),
        new Cmd(
		"-priorprefix",
		"Append prefix from the previous element to rows that match pattern",
		new Arg("column", "", "type", "column"),
		new Arg("pattern", "", "type", "pattern"), new Arg("delimiter")),
        new Cmd("-case", "Change case of column",
                new Arg("type", "", "values",
                        "lower,upper,proper,capitalize"), new Arg("column",
								 "", "type", "column")),
        new Cmd("-width", "Limit the string size of the columns",
                new Arg("columns", "", "type", "columns"), new Arg("size")),
        new Cmd(
		"-prepend",
		"Add the text to the beginning of the file. use _nl_ to insert newlines",
		new Arg("text")),
        new Cmd("-pad", "Add or remove columns to achieve the count",
                new Arg("count"), new Arg("pad string")),
        new Cmd("-prefix", "Add prefix to column",
                new Arg("column", "", "type", "column"), new Arg("prefix")),
        new Cmd("-suffix", "Add suffix to column",
                new Arg("column", "", "type", "column"), "suffix"),
        new Cmd(
		"-js",
		"Define Javascript (e.g., functions) to use later in the -func call",
		new Arg("javascript", "", "rows", "6")),
        new Cmd("-func",
                "Apply the javascript function. Use _colname or _col#",
                new Arg("names", "New column names", "type", "list"),
                new Arg("javascript", "javascript expression", "size", "60")),
        new Cmd("-endswith", "Ensure that each column ends with the string",
                new Arg("column", "", "type", "column"), new Arg("string")),
        new Cmd("-trim", "Trim the string values",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-truncate", "", new Arg("column", "", "type", "columns"),
                "max length", "suffix"),
        new Cmd("-extract", "Extract text from column and make a new column",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("replace with", "use 'none' for no replacement"),
                "new column name"),
        new Cmd("-map", "Change values in column to new values",
                new Arg("column", "", "type", "columns"), "new columns name",
                "value newvalue ..."),
        new Cmd("-combine",
                "Combine columns with the delimiter. deleting columns",
                new Arg("column", "", "type", "columns"), "delimiter",
                "new column name"),
        new Cmd("-combineinplace", new Label("Combine in place"),
                "Combine columns with the delimiter",
                new Arg("column", "", "type", "columns"), "delimiter",
                "new column name"),
        new Cmd("-format", "", new Arg("columns", "", "type", "columns"),
                new Arg("format", "Decimal format  e.g. '##0.00'")),
        new Cmd(
		"-denormalize",
		"Read the id,value from file and substitute the value in the dest file col idx",
		new Arg("file", "From csv file", "type", "file"), "from id idx",
		"from value idx", "to idx", "new col name", "mode replace add"),
        new Cmd("-break", "Break apart column values and make new rows",
                "label1", "label2", "columns"),

        new Cmd("-makeids", "Turn the header row into IDs (lowercase, no space, a-z0-9_"),

        /** *  Add values * */
        new Cmd(true, "Add Values"),
        new Cmd("-md", "Make a message digest of the column values",
                new Arg("columns", "", "type", "columns"),
                new Arg("type", "", "values", "MD5,SHA-1,SHA-256")),
        new Cmd("-uuid", "Add a UUID field"),
        new Cmd("-number", "Add 1,2,3... as column"),
        new Cmd("-letter", "Add 'A','B', ... as column"),
	//        new Cmd(true, "Lookup"),
        new Cmd("-wikidesc", "Add a description from wikipedia",
                new Arg("column", "", "type", "columns"), "suffix"),
        new Cmd("-image", "Search for an image",
                new Arg("column", "", "type", "columns"), "suffix"),
        new Cmd(
		"-imagefill",
		"Search for an image with the query column text if the given image column is blank. Add the given suffix to the search. ",
		new Arg("querycolumn", "", "type", "columns"), "suffix",
		new Arg("imagecolumn", "", "type", "column")),
        new Cmd("-gender", "Figure out the gender of the name in the column",
                new Arg("column", "", "type", "columns")),


        /** *  Dates * */
        new Cmd(true, "Dates"),
        new Cmd("-dateformat", new Label("Specify date format for later use"),  "",
                new Arg("format", "e.g. yyyy-MM-dd HH:mm:ss"),
		new Arg("timezone", "")),

        new Cmd("-convertdate", new Label("Convert date"), "",
                new Arg("column", "", "type", "columns"),
                new Arg("destformat", "date format")),

        new Cmd(
		"-extractdate", new Label("Extract date"), "",
		new Arg("date column", "", "type", "column"),
		new Arg(
			"what", "What to extract, e.g., year, month, day_of_week, etc", "values",
			"era,year,month,day_of_month,day_of_week,week_of_month,day_of_week_in_month,am_pm,hour,hour_of_day,minute,second,millisecond")),

        new Cmd("-formatdate", new Label("Format date"), "",
                new Arg("columns"), "target date format"),

        new Cmd("-elapsed", new Label("Calculate elapsed time between rows"), "Writes milliseconds",
                new Arg("column")),


        /** *  Numeric * */
        new Cmd(true, "Numeric"),
        new Cmd("-scale", "Set value={value+delta1}*scale+delta2",
                new Arg("column", "", "type", "columns"), "delta1", "scale",
                "delta2"),
        new Cmd("-generate", "Add row values", "label", "start", "step"),
        new Cmd("-decimals", "", new Arg("column", "", "type", "columns"),
                "how many decimals to round to"),
        new Cmd("-delta",
                "Add column that is the delta from the previous step",
                new Arg("key columns"), new Arg("columns")),
        new Cmd("-operator",
                "Apply the operator to the given columns and create new one",
                new Arg("columns"), "new col name", "operator +,-,*,/"),
        new Cmd("-round", "round the values", "columns"),
        new Cmd("-abs", "make absolute values", "columns"),
        new Cmd("-rand", "make random value"),		
        new Cmd(
		"-sum",
		"Sum values keying on name column value. If no value columns specified then do a count",
		"key columns", "value columns", "carry over columns"),
        new Cmd("-percent", "", "columns to add"),
        new Cmd("-increase", "Calculate percent increase",
                new Arg("column", "", "type", "columns"), "how far back"),
        new Cmd("-average", "Calculate a moving average", "columns",
                "period", "label"),
        new Cmd("-bytes", "Convert suffixed values (e.g., 2 MB) into the number",
                new Arg("unit", "", "type", "enumeration","values","binary,metric"),
                new Arg("column", "", "type", "columns")),
        /** * Geocode  * */
        new Cmd(true, "Geocode"),
        new Cmd("-geocode", "", new Arg("columns", "", "type", "columns"),
                new Arg("prefix", "e.g., state: or county:"),
                new Arg("suffix")),
        new Cmd("-geocodeaddressdb", new Label("Geocode address for DB"), "",
                new Arg("columns"), "prefix", "suffix"),
        new Cmd("-geocodejoin", new Label("Geocode with file"),
                "Geocode with file",
                new Arg("column", "", "type", "columns"),
                new Arg("csv file", "File to get lat/lon from", "type",
                        "file"), "name idx", "lat idx", "lon idx"),
        new Cmd("-statename", "Add state name from state ID",
                new Arg("column")),
	new Cmd("-geoname", "Look up location name",
                new Arg("lookup","('counties' or 'states' or 'countries' or 'timezones')"),
                new Arg("lat", "Latitude column", "type", "column"),
                new Arg("lon", "Longitude column", "type", "column")),	
	new Cmd("-elevation", "Look up elevation(using 1/3 arc-second DEM)",
                new Arg("lat", "Latitude column", "type", "column"),
                new Arg("lon", "Longitude column", "type", "column")),	

        new Cmd("-mercator", "Convert x/y to lon/lat", new Arg("columns")),
        new Cmd("-region", "Add the state's region",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-population", "Add in population from address",
                new Arg("columns", "", "type", "columns"),
                new Arg("prefix", "e.g., state: or county:"), "suffix"),

        /** * Other  * */
        new Cmd(true, "Misc."),
        new Cmd("-sort", "",
                new Arg("column", "Column to sort on", "type", "column")),
        new Cmd("-descsort", "",
                new Arg("column", "Column to descending sort on", "type",
                        "column")),
        new Cmd("-count", "Show count"),
        new Cmd("-maxrows", "", "Max rows to print"),
        new Cmd("-changeline", new Label("Change line"), "Change the line",
                "from", "to"),
        new Cmd("-changeraw", new Label("Change input"), "Change input text",
                "from", "to"),
        new Cmd("-crop", new Label("Crop string"),
                "Crop last part of string after any of the patterns",
                "columns", "pattern1,pattern2"),
        new Cmd(
		"-strict",
		"Be strict on columns. any rows that are not the size of the other rows are dropped"),
        new Cmd(
		"-flag",
		"Be strict on columns. any rows that are not the size of the other rows are shown"),
        new Cmd("-verify",
                "Throw error if a row has a different number of columns",
                new Arg("# columns", "", "type", "number")),
        new Cmd("-prop", "Set a property",
                new Arg("property", "", "values", "position"),
                new Arg("value", "start, end, etc")),
        new Cmd("-comment", "", "string"),
        new Cmd("-verify",
                "Verify that all of the rows have the same # of columns"),


        /*  Output   */
        new Cmd(true, "Output"), 
	new Cmd("-print", "Text output"),
        new Cmd("-printheader", "Print header"),
        new Cmd("-raw", "Print the file raw"),
        new Cmd("-stats", "Print stats"),	
        new Cmd("-record", "Print records"),
        new Cmd("-toxml", "Generate XML", new Arg("tag1"),new Arg("tag2")),
        new Cmd("-tojson", "Generate JSON"),	
        new Cmd("-template", "Apply the template to make the output",
                new Arg("prefix", "", "size", "40"),
                new Arg("template", "Use ${0},${1}, etc for values", "rows",
                        "6"), new Arg("delimiter", "Output between rows",
                                      "size", "40"), new Arg("suffix", "",
							     "size", "40")),
        new Cmd("-addheader", new Label("Add header"),
                "Add the RAMADDA point properties",
                new Arg("properties", "name1 value1 ... nameN valueN",
                        "rows", "6")),
        new Cmd(
		"-db", "Generate the RAMADDA db xml from the header",
		new Arg(
			"props",
			"Name value pairs:\n\t\ttable.id <new id> table.name <new name> table.cansearch <true|false> table.canlist <true|false> table.icon <icon, e.g., /db/database.png>\n\t\t<column name>.id <new id for column> <column name>.label <new label>\n\t\t<column name>.type <string|enumeration|double|int|date>\n\t\t<column name>.format <yyyy MM dd HH mm ss format for dates>\n\t\t<column name>.canlist <true|false> <column name>.cansearch <true|false>\n\t\tinstall <true|false install the new db table>\n\t\tnukedb <true|false careful! this deletes any prior created dbs", "rows", "6")),
        new Cmd("-run", "", "Name of process directory"),
        new Cmd("-dots", "", "Print a dot every count row",
		new Arg("every", "Dot every")),

	new Cmd("-script", "Generate the script to call"),
        new Cmd("-args", "Generate the CSV file commands"),
        new Cmd("-pointheader", "Generate the RAMADDA point properties"),
	//        new Cmd("-args2", "Print out the args"),
    };



    /**
     * _more_
     *
     *
     * @param msg _more_
     * @param match _more_
     * @param args _more_
     * @throws Exception _more_
     */
    public void usage(String msg, String match, String... args)
	throws Exception {
        boolean exact = false;
        boolean raw   = false;
        boolean json  = false;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-exact")) {
                exact = args[i + 1].equals("true");
            } else if (args[i].equals("-raw")) {
                raw = args[i + 1].equals("true");
            } else if (args[i].equals("-json")) {
                json = args[i + 1].equals("true");
            }
        }
        PrintWriter pw = new PrintWriter(getOutputStream());
        if (msg.length() > 0) {
            pw.println(msg);
        }
        if ( !json) {
            pw.println("Usage:");
        } else {
            pw.println("{\"commands\":[");
        }
        int     cnt             = 0;
        String  pad             = "\t";
        boolean matchedCategory = false;
        for (Cmd c : commands) {
            String cmd = c.getLine();
            if (match != null) {
                String text  = c.cmd;
                String desc  = null;
                String label = null;
                if (c.category) {
                    matchedCategory = false;
                    text            = c.desc;
                } else {
                    text  = c.cmd;
                    desc  = c.desc;
                    label = c.label;
                    if (desc != null) {
                        desc = desc.toLowerCase();
                    }
                    if (label != null) {
                        label = label.toLowerCase();
                    }
                }
                boolean ok = true;
                text = text.toLowerCase();
                if (exact && !text.equals(match)) {
                    ok = false;
                } else if ( !exact) {
                    if (text.indexOf(match) < 0) {
                        ok = false;
                    }
                    if ( !ok && (label != null)) {
                        ok = label.indexOf(match) >= 0;
                    }
                    if ( !ok && (desc != null)) {
                        ok = desc.indexOf(match) >= 0;
                    }
                }

                if (c.category) {
                    matchedCategory = ok;

                    continue;
                } else {
                    ok = ok || matchedCategory;
                }
                if ( !ok) {
                    continue;
                }
            }
            if ( !raw) {
                cmd = cmd.replaceAll("_nl_", "\n").replaceAll("_tab_", "\n");
            }
            if (json) {
                if (cnt > 0) {
                    pw.println(",");
                }
                if (c.category) {
                    pw.println(Json.mapAndQuote("isCategory", "true",
						"description", c.desc));
                } else {
                    String argList = "[]";
                    if (c.args != null) {
                        List tmp = new ArrayList();
                        for (Arg arg : c.args) {
                            List<String> attrs = new ArrayList<String>();
                            attrs.add("id");
                            attrs.add(Json.quote(arg.id));
                            attrs.add("description");
                            attrs.add(Json.quote(arg.desc));
                            if (arg.props != null) {
                                for (int i = 0; i < arg.props.length;
				     i += 2) {
                                    attrs.add(arg.props[i]);
				    String v = arg.props[i + 1];
				    if(v.startsWith("property:")) {
					v = (String)getProperty(v.substring("property:".length()));
				    }
				    if(v==null) v="";
				    attrs.add(Json.quote(v));
                                }
                            }
                            tmp.add(Json.map(attrs));

                        }
                        argList = Json.list(tmp);
                    }
                    pw.println(Json.map("command", Json.quote(c.cmd),
                                        "label", (c.label != null)
					? Json.quote(c.label)
					: "null", "args", argList, "description",
					Json.quote(c.desc)));
                }
            } else {
                if (c.category) {
                    pw.println(c.desc);
                } else {
                    pw.println(pad + cmd);
                }
            }
            if (raw && cmd.startsWith("-db")) {
                break;
            }
            cnt++;
        }
        if (json) {
            pw.println("]}");
        }
        pw.flush();
        pw.close();

    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void genHelp() throws Exception {
        PrintWriter pw = new PrintWriter(getOutputStream());
        for (Cmd c : commands) {
            pw.println("[etl {" + c.cmd + "} {" + c.args + "} {" + c.desc
                       + "}]");
        }
        pw.flush();
    }



    /**
     * _more_
     *
     * @param args _more_
     * @param i _more_
     * @param cnt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean ensureArg(List args, int i, int cnt) throws Exception {
        if (args.size() <= (i + cnt)) {
            String arg = (String) args.get(i);
            usage("Bad argument count for:" + arg, arg, "-exact", "true");
            return false;
        }
        return true;
    }

    private interface CsvFunction {
	int method(TextReader ctx, List<String> args,int index); 

    }

    public static class MessageException extends RuntimeException {
	String msg;
	public MessageException(String s) {
	    super(s);
	    msg = s;
	}

	public String getMessage() {
	    return msg;
	}

    }


    private static int SKIP_INDEX = -999;
    private static class CsvFunctionHolder {
	private CsvUtil csvUtil;
	private String  name;
	private int numargs;
	private CsvFunction func;
	CsvFunctionHolder(CsvUtil csvUtil, String name,int numargs,CsvFunction func) {
	    this.csvUtil = csvUtil;
	    this.name = name;
	    this.numargs = numargs;
	    this.func = func;
	}
	public int run(TextReader ctx, List<String> args, int index) throws Exception {
	    if ( !csvUtil.ensureArg(args, index, numargs)) {
		return -1;
	    }
	    return this.func.method(ctx,args,index);
	}
    }

    private Hashtable<String,CsvFunctionHolder> functions;

    private void defineFunction(String[] names, int args, CsvFunction func) {
	for(String  name: names)
	    defineFunction(name,args,func);
    }


    private CsvFunctionHolder defineFunction(String name, int args, CsvFunction func) {
	CsvFunctionHolder csvFunction = new CsvFunctionHolder(this,name,args,func);
	if(functions==null)
	    functions= new Hashtable<String,CsvFunctionHolder>();
	functions.put(name,csvFunction);
	return csvFunction;
    }

    private void makeFunctions() {
	defineFunction("-skip",1,(ctx,args,i) -> {
		ctx.setSkip(Integer.parseInt(args.get(++i)));
		return i;
	    });

	defineFunction("-skipline",1,(ctx,args,i) -> {
		ctx.setSkipPattern(args.get(++i));
		return i;
	    });

	defineFunction("-pass",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Pass());
		return i;
	    });

	defineFunction("-changeline",2,(ctx,args,i) -> {
		ctx.setChangeString(args.get(++i), args.get(++i));
		return i;
	    });

	defineFunction("-image",2, (ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ImageSearch(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-columns",1,(ctx,args,i) -> {
		ctx.setSelector(new Converter.ColumnSelector(getCols(args.get(++i))));
		ctx.getProcessor().addProcessor(ctx.getSelector());
		return i;
	    });

	defineFunction("-notcolumns",1,(ctx,args,i) -> {
		List<String> cols = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(
						new Converter.ColumnNotSelector(cols));

		return i;
	    });

	defineFunction("-number",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Number());
		return i;
	    });

	defineFunction("-letter",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Letter());
		return i;
	    });

	defineFunction("-uuid",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.UUID());
		return i;
	    });

	defineFunction("-start",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Filter.Start(args.get(++i)));
		return i;
	    });

	defineFunction("-stop",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Filter.Stop(args.get(++i)));

		return i;
	    });

	defineFunction("-min",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Filter.MinColumns(new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-max",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Filter.MaxColumns(new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-decimate",2,(ctx,args,i) -> {
		int start = Integer.parseInt(args.get(++i));
		int skip  = Integer.parseInt(args.get(++i));
		if (skip > 0) {
		    ctx.getProcessor().addProcessor(
						    new Filter.Decimate(start, skip));
		}
		return i;
	    });

	defineFunction("-db",1,(ctx,args,i) -> {
		Hashtable<String, String> props =  parseProps(args.get(++i));
		ctx.putProperty("installPlugin", ""+(Utils.equals(props.get("-install"),"true") || Utils.equals(props.get("install"),
														"true")));
		ctx.putProperty("nukeDb", ""+(Utils.equals(props.get("-nukedb"), "true")
					      || Utils.equals(props.get("nukedb"),
							      "true")));
		ctx.getProcessor().addProcessor(dbXml =  new Processor.DbXml(props));
		ctx.setMaxRows(30);
		return i;
	    });

	defineFunction("-unfurl",4,(ctx,args,i) -> {
		String       mainCol   = args.get(++i);
		List<String> valueCols = getCols(args.get(++i));
		String       uniqueCol = args.get(++i);
		List<String> extraCols = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Processor.Unfurler(
								       mainCol, valueCols, uniqueCol, extraCols));

		return i;
	    });

	defineFunction("-furl",3,(ctx,args,i) -> {
		List<String> valueCols = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Processor.Furler(
								     valueCols, args.get(++i), args.get(++i)));

		return i;
	    });

	defineFunction("-break",3,(ctx,args,i) -> {
		String       label1 = args.get(++i);
		String       label2 = args.get(++i);
		List<String> cols   = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Processor.Breaker(label1, label2, cols));

		return i;
	    });

	defineFunction("-sort",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Sorter(args.get(++i), true));

		return i;
	    });

	defineFunction("-descsort",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Sorter(args.get(++i), false));
		return i;
	    });

	defineFunction("-join",4,(ctx,args,i) -> {
		List<String> keys1   = getCols(args.get(++i));
		List<String> values1 = getCols(args.get(++i));
		String       file    = args.get(++i);
		List<String> keys2   = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Processor.Joiner(keys1, values1, file, keys2));
		return i;
	    });

	defineFunction("-sum",3,(ctx,args,i) -> {
		List<String> keys   = getCols(args.get(++i));
		List<String> values = getCols(args.get(++i));
		List<String> extra  = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Processor.Summer(keys, values, extra));
		return i;
	    });

	defineFunction("-unique",1,(ctx,args,i) -> {
		List<String> toks = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Filter.Unique(toks));
		return i;
	    });

	defineFunction("-dups",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Dups(getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-verify",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Verifier());
		return i;
	    });

	defineFunction("-count",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Counter());
		return i;
	    });


	defineFunction("-log",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Logger());
		return i;
	    });

	defineFunction("-strict",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Counter(true));
		return i;
	    });

	defineFunction("-flag",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Counter(true, true));
		return i;
	    });


	defineFunction("-rotate",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Rotator());
		return i;
	    });

	defineFunction("-flip",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Flipper());
		return i;
	    });



	defineFunction("-rawlines",1,(ctx,args,i) -> {
		rawLines = Integer.parseInt(args.get(++i));
		return i;
	    });


	defineFunction("-tab",0,(ctx,args,i) -> {
		ctx.setDelimiter(delimiter = "tab");
		return i;
	    });

	defineFunction("-delimiter",1,(ctx,args,i) -> {
		ctx.setDelimiter(delimiter = args.get(++i));
		return i;
	    });

	defineFunction("-widths",1,(ctx,args,i) -> {
		List<Integer> widths = new ArrayList<Integer>();
		for (String tok : Utils.split(args.get(++i), ",", true,
					      true)) {
		    widths.add(Integer.parseInt(tok));
		}
		ctx.setWidths(widths);
		return i;
	    });

	defineFunction("-comment",1,(ctx,args,i) -> {
		ctx.setComment(comment = args.get(++i));
		return i;
	    });

	defineFunction("-outputdelimiter",1,(ctx,args,i) -> {
		String s = args.get(++i);
		if (s.equals("tab")) {
		    s = "\t";
		}
		ctx.setOutputDelimiter(s);
		return i;
	    });

	defineFunction("-cut",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Filter.RowCutter(Utils.getNumbers(args.get(++i)), true));
		return i;
	    });


	defineFunction("-include",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Filter.RowCutter(Utils.getNumbers(args.get(++i)), false));
		return i;
	    });


	
	defineFunction("-prop",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Propper(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-rowop",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.RowOperator(getCols(args.get(++i)),getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-fields",0,(ctx,args,i) -> {
		ctx.setPrintFields(true);
		return i;
	    });

	
	defineFunction("-percent",  1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnPercenter(getCols(args.get(++i))));
		return i;
	    });


	defineFunction("-average",3,(ctx,args,i) -> {
		List<String> cols   = getCols(args.get(++i));
		int          period = Integer.parseInt(args.get(++i));
		String       label  = args.get(++i);
		ctx.getProcessor().addProcessor(
						new Converter.ColumnAverage(
									    Converter.ColumnAverage.MA, cols, period, label));

		return i;
	    });

	defineFunction("-increase",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnIncrease(args.get(++i), Integer.parseInt(args.get(++i))));
		return i;
	    });



	defineFunction("-sumrow",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnOperator());

		return i;
	    });

	defineFunction("-pad",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Padder(new Integer(args.get(++i)).intValue(), args.get(++i)));
		return i;
	    });

	defineFunction("-prefix",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Prefixer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-suffix",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Suffixer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction("-explode",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Exploder(args.get(++i)));
		return i;
	    });
	defineFunction("-dissect",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Dissector(args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction("-keyvalue",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.KeyValue(args.get(++i)));
		return i;
	    });		

	defineFunction("-gender",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Genderizer(args.get(++i)));
		return i;
	    });

	defineFunction("-ximage",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ImageSearch(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-imagefill",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ImageSearch(getCols(args.get(++i)), args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction("-wikidesc",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.WikiDescSearch(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-statename",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.StateNamer(args.get(++i)));
		return i;
	    });
	defineFunction("-geoname",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.GeoNamer(args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction("-elevation",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Elevation(args.get(++i),args.get(++i)));
		return i;
	    });		

	defineFunction("-geocode",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim()));
		return i;
	    });

	defineFunction("-geocodejoin",5,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Geocoder(args.get(++i),args.get(++i), Integer.parseInt(args.get(++i)),
								       Integer.parseInt(args.get(++i)),
								       Integer.parseInt(args.get(++i)), false));
		return i;
	    });



	defineFunction("-geocodeaddressdb",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim(), true));
		return i;
	    });

	defineFunction("-geocodedb",5,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Geocoder(args.get(++i),args.get(++i), Integer.parseInt(args.get(++i)),
								       Integer.parseInt(args.get(++i)),
								       Integer.parseInt(args.get(++i)), true));
		return i;
	    });


	defineFunction("-population",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Populator(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim()));
		return i;
	    });

	defineFunction("-region",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Regionator(getCols(args.get(++i))));
		return i;
	    });


	defineFunction("-crop",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Cropper(getCols(args.get(++i)), Utils.split(args.get(++i), ",", true, true)));
		return i;
	    });

	defineFunction("-change",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnChanger(getCols(args.get(++i)),Utils.convertPattern(args.get(++i)),  args.get(++i)));
		return i;
	    });


	defineFunction("-endswith",2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnEndsWith(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-trim",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnTrimmer(getCols(args.get(++i))));
		return i;
	    });


	defineFunction("-extract",4,(ctx,args,i) -> {
		int    col     = new Integer(args.get(++i));
		String pattern = args.get(++i);
		String replace = args.get(++i);
		String name    = args.get(++i);
		pattern = Utils.convertPattern(pattern);
		ctx.getProcessor().addProcessor(
						new Converter.ColumnExtracter(
									      col, pattern, replace, name));

		return i;
	    });


	defineFunction("-truncate",3,(ctx,args,i) -> {
		int    col    = new Integer(args.get(++i));
		int    length = new Integer(args.get(++i));
		String suffix = args.get(++i);
		ctx.getProcessor().addProcessor(
						new Converter.Truncater(col, length, suffix));

		return i;
	    });


	defineFunction("-changerow",4,(ctx,args,i) -> {
		List<Integer> rows    = Utils.getNumbers(args.get(++i));
		List<String>  cols    = getCols(args.get(++i));
		String        pattern = args.get(++i);
		pattern = Utils.convertPattern(pattern);
		ctx.getProcessor().addProcessor(
						new Converter.RowChanger(
									 rows, cols, pattern, args.get(++i)));

		return i;
	    });

	defineFunction("-formatdate", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.DateFormatter(getCols(args.get(++i)), dateFormat, args.get(++i)));
		return i;
	    });

	defineFunction("-elapsed", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Elapsed(args.get(++i), dateFormat));
		return i;
	    });


	defineFunction("-dateformat", 2,(ctx,args,i) -> {
		this.dateFormat = new SimpleDateFormat(dateFormatString = args.get(++i));
		this.timezone = args.get(++i);
		return i;
	    });

	defineFunction("-convertdate",2,(ctx,args,i) -> {
		String col  = args.get(++i);
		String sdf2 = args.get(++i);
		ctx.getProcessor().addProcessor(
						new Converter.DateConverter(
									    col, dateFormat,
									    new SimpleDateFormat(sdf2)));
		return i;
	    });

	defineFunction("-extractdate",2,(ctx,args,i) -> {
		String col  = args.get(++i);
		String what = args.get(++i);
		ctx.getProcessor().addProcessor(
						new Converter.DateExtracter(col, dateFormatString, timezone, what));

		return i;
	    });



	defineFunction("-before",4,(ctx,args,i) -> {
		try {
		    int    col  = Integer.parseInt(args.get(++i));
		    String sdf1 = args.get(++i);
		    String date = args.get(++i);
		    String sdf2 = args.get(++i);
		    Date   dttm = null;
		    if (date.equals("now")) {
			dttm = new Date();
		    } else if (sdf2.length() > 0) {
			SimpleDateFormat sdf = new SimpleDateFormat(sdf2);
			dttm = sdf.parse(date);
		    } else {
			dttm = Utils.parseDate(date);
		    }
		    ctx.getProcessor().addProcessor(
						    new Converter.DateBefore(
									     col, new SimpleDateFormat(sdf1), dttm));

		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });

	defineFunction("-after",4,(ctx,args,i) -> {
		try {
		    int    col  = Integer.parseInt(args.get(++i));
		    String sdf1 = args.get(++i);
		    String date = args.get(++i);
		    String sdf2 = args.get(++i);
		    Date   dttm = null;
		    if (date.equals("now")) {
			dttm = new Date();
		    } else if (sdf2.length() > 0) {
			SimpleDateFormat sdf = new SimpleDateFormat(sdf2);
			dttm = sdf.parse(date);
		    } else {
			dttm = Utils.parseDate(date);
		    }
		    ctx.getProcessor().addProcessor(
						    new Converter.DateAfter(
									    col, new SimpleDateFormat(sdf1), dttm));

		    return i;
	       	} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}

	    });

	defineFunction("-latest",3,(ctx,args,i) -> {
		List<String> cols = getCols(args.get(++i));
		String       col  = args.get(++i);
		String       sdf  = args.get(++i);
		ctx.getProcessor().addProcessor(
						new Converter.DateLatest(
									 cols, col, new SimpleDateFormat(sdf)));

		return i;
	    });






	defineFunction("-html",3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.HtmlDataProvider(args.get(++i), args.get(++i),
									 parseProps(args.get(++i))));

		return i;
	    });
	defineFunction("-htmlpattern",4,(ctx,args,i) -> {
		ctx.getProviders().add(
				       new DataProvider.HtmlPatternDataProvider(args.get(++i), args.get(++i),
										args.get(++i), args.get(++i)));

		return i;
	    });

	defineFunction("-text",3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.TextDataProvider(args.get(++i), args.get(++i), args.get(++i)));

		return i;
	    });
	defineFunction("-text2",3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Pattern2DataProvider(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-text3",2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Pattern3DataProvider(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-tokenize",2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.PatternDataProvider(Utils.split(args.get(++i), ","),
									    args.get(++i)));
		return i;
	    });

	defineFunction("-json",2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.JsonDataProvider(args.get(++i), args.get(++i)));

		return i;
	    });

	defineFunction("-sql",4,(ctx,args,i) -> {
		//-sql db table cols "col1 value col2 value"
		ctx.getProviders().add(new DataProvider.SqlDataProvider(args.get(++i),
									args.get(++i),
									args.get(++i),
									args.get(++i),																	
									parseProps(args.get(++i))));
		return i;
	    });

	defineFunction("-xml",1,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.XmlDataProvider(args.get(++i)));
		return i;
	    });
	defineFunction("-lines",0,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Lines());
		return i;
	    });


	defineFunction("-shapefile",1,(ctx,args,i) -> {
		ctx.getProviders().add(new ShapefileProvider(parseProps(args.get(++i))));
		return i;
	    });
	
	defineFunction("-kml",0,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.KmlDataProvider());	
	return i;
	    });
	
	defineFunction("-changeraw",2,(ctx,args,i) -> {
		ctx.addChangeFromTo(args.get(++i),args.get(++i));
		return i;
	    });

	defineFunction("-maxrows",1,(ctx,args,i) -> {
		ctx.setMaxRows(Integer.parseInt(args.get(++i)));

		return i;
	    });

	defineFunction("-prune",1,(ctx,args,i) -> {
		ctx.setPruneBytes(Integer.parseInt(args.get(++i)));
		return i;
	    });


	defineFunction("-mergerows",3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.RowMerger(Utils.getNumbers(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-prepend",1,(ctx,args,i) -> {
		String text = args.get(++i);
		text = text.replaceAll("_nl_", "\n");
		ctx.setPrepend(text);
		return i;
	    });

	defineFunction("-columndebug", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnDebugger(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	
	defineFunction("-map", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnMapper(getCols(args.get(++i)), args.get(++i),
									   Utils.parseCommandLine(args.get(++i))));
		return i;
	    });


	defineFunction("-split", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnSplitter(
									     args.get(++i), args.get(++i),
									     Utils.split(args.get(++i), ",")));
		return i;
	    });

	defineFunction("-delete", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnDeleter(getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-insert", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnInserter(args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction("-shift", 3,(ctx,args,i) -> {
		List<Integer> rows  = Utils.getNumbers(args.get(++i));
		int           col   = Integer.parseInt(args.get(++i));
		int           count = Integer.parseInt(args.get(++i));
		ctx.getProcessor().addProcessor(new Converter.Shifter(rows, col, count));
		return i;
	    });


	defineFunction("-generate", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Generator(args.get(++i), Double.parseDouble(args.get(++i)),
									Double.parseDouble(args.get(++i))));
		return i;
	    });



	defineFunction("-macro", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnMacro(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction("-format", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnFormatter(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction("-scale", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnScaler(getCols(args.get(++i)), Double.parseDouble(args.get(++i)),
									   Double.parseDouble(args.get(++i)),
									   Double.parseDouble(args.get(++i))));

		return i;
	    });


	defineFunction("-decimals", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Decimals(getCols(args.get(++i)), new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-copy", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnCopier(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-concat", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnNewer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-splat", 4,(ctx,args,i) -> {
		String key       = args.get(++i);
		String value     = args.get(++i);
		String delimiter = args.get(++i);
		String name      = args.get(++i);
		ctx.getProcessor().addProcessor(new Processor.Splatter(key, value, delimiter, name));
		return i;
	    });
	defineFunction("-delta", 2,(ctx,args,i) -> {
		List<String> keyidxs = getCols(args.get(++i));
		List<String> idxs    = getCols(args.get(++i));
		ctx.getProcessor().addProcessor(new Converter.Delta(keyidxs, idxs));
		return i;
	    });



	defineFunction("-operator", 3,(ctx,args,i) -> {
		List<String> idxs = getCols(args.get(++i));
		String       name = args.get(++i);
		String       op   = args.get(++i);
		ctx.getProcessor().addProcessor(new Converter.ColumnMathOperator(idxs, name, op));
		return i;
	    });

	defineFunction("-js", 1,(ctx,args,i) -> {
		js.append(args.get(++i));
		js.append("\n");
		return i;
	    });

	defineFunction("-func", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnFunc(js.toString(), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-mercator", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Mercator(getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-round", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnRounder(getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-bytes", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Bytes(args.get(++i),getCols(args.get(++i))));
		return i;
	    });	
	defineFunction("-abs", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnAbs(getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-rand", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnRand());
		return i;
	    });		
	defineFunction("-md", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.MD(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });


	defineFunction("-striptags", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.StripTags(getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-decode", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Decoder(getCols(args.get(++i))));
		return i;
	    });	


	defineFunction("-case", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.Case(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });

	defineFunction("-addcell", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnNudger(Integer.parseInt(args.get(++i)),Integer.parseInt(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-deletecell", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnUnNudger(Integer.parseInt(args.get(++i)), getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-priorprefix", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.PriorPrefixer(Integer.parseInt(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-set", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnSetter(getCols(args.get(++i)),getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction("-makeids", 0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.MakeIds());
		return i;
	    });

	defineFunction("-setcol", 4,(ctx,args,i) -> {
		String col1    = args.get(++i);
		String pattern = args.get(++i);
		String col2    = args.get(++i);
		String what    = args.get(++i);
		ctx.getProcessor().addProcessor(new Converter.ColumnPatternSetter(col1, pattern, col2, what));
		return i;
	    });

	defineFunction("-width", 2,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnWidth(getCols(args.get(++i)), Integer.parseInt(args.get(++i))));
		return i;
	    });

	defineFunction("-combine", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnConcatter(getCols(args.get(++i)),args.get(++i),args.get(++i),false));
		return i;
	    });


	defineFunction("-combineinplace", 3,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.ColumnConcatter(getCols(args.get(++i)),args.get(++i),args.get(++i),true));
		return i;
	    });

	defineFunction("-denormalize", 6,(ctx,args,i) -> {
		String file = args.get(++i);
		int    col1 = Integer.parseInt(args.get(++i));
		int    col2 = Integer.parseInt(args.get(++i));
		int    col3 = Integer.parseInt(args.get(++i));
		String name = args.get(++i);
		String mode = args.get(++i);
		ctx.getProcessor().addProcessor(new Converter.Denormalizer(file, col1, col2, col3, name, mode));
		return i;
	    });


	defineFunction("-or",0,(ctx,args,i) -> {
		ctx.setFilterToAddTo(new Filter.FilterGroup(false));
		ctx.getProcessor().addProcessor(ctx.getFilterToAddTo());
		return i;
	    });

	defineFunction("-and",0,(ctx,args,i) -> {
		ctx.setFilterToAddTo(new Filter.FilterGroup(true));
		ctx.getProcessor().addProcessor(ctx.getFilterToAddTo());
		return i;
	    });

	defineFunction("-pattern", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.PatternFilter(args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction("-notpattern", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.PatternFilter(args.get(++i),args.get(++i), true));
		return i;
	    });

	defineFunction("-countvalue", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.CountValue(args.get(++i), Integer.parseInt(args.get(++i))));
		return i;
	    });

	defineFunction("-groupfilter", 4,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.GroupFilter(getCols(args.get(++i)), Integer.parseInt(args.get(++i)),
									  CsvOperator.getOperator(args.get(++i)),
									  args.get(++i)));
		return i;
	    });


	defineFunction("-eq", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(getCols(args.get(++i)),
						     Filter.ValueFilter.OP_EQUALS,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction("-ne", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(getCols(args.get(++i)),
						     Filter.ValueFilter.OP_NOTEQUALS,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });




	defineFunction("-lt", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(
						     getCols(args.get(++i)), Filter.ValueFilter.OP_LT,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction("-gt", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(
						     getCols(args.get(++i)), Filter.ValueFilter.OP_GT,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });


	defineFunction("-defined", 1,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(getCols(args.get(++i)),Filter.ValueFilter.OP_DEFINED, 0));
		return i;
	    });
	defineFunction("-maxvalue", 2,(ctx,args,i) -> {
		String key   = args.get(++i);
		String value = args.get(++i);
		ctx.getProcessor().addProcessor(
						new Processor.MaxValue(key, value));

		return i;
	    });


	defineFunction("-quit",0,(ctx,args,i) -> {
		String last = args.get(args.size() - 1);
		if (last.equals("-print") || last.equals("-p")) {
		    ctx.getProcessor().addProcessor(
						    new Processor.Printer(ctx.getPrintFields(), false));
		} else if (last.equals("-table")) {
		    ctx.getProcessor().addProcessor(new Processor.Html());
		}
		return -1;
	    });



	defineFunction("-dots",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Dots(new Integer(args.get(++i))));
		return i;
	    });



	defineFunction("-addheader",1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.HeaderMaker(parseProps(args.get(++i))));
		return i;
	    });

	defineFunction("-deheader",0,(ctx,args,i) -> {
		ctx.putProperty("deheader","true");
		return i;
	    });


	defineFunction("-output",1,(ctx,args,i) -> {
		try {
		    String out = args.get(++i);
		    this.outputStream = makeOutputStream(out);
		    ctx.setWriter(new PrintWriter(this.outputStream));
		    ctx.getProcessor().addProcessor(new Processor.Printer(ctx.getPrintFields(), false));
		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });


	defineFunction("-toxml",2,(ctx,args,i) -> {
		hasSink = true;
		ctx.getProcessor().addProcessor(new Processor.ToXml(args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction("-tojson",0,(ctx,args,i) -> {
		hasSink = true;
		ctx.getProcessor().addProcessor(new Processor.ToJson());
		return i;
	    });	


	defineFunction("-table",0, (ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.Html());
		return i;
	    });


	defineFunction("-dump",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(
						new Processor.Printer(ctx.getPrintFields(), false));
		return i;
	    });

	defineFunction("-record",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(
						new Processor.Prettifier());
		return i;
	    });

	defineFunction(new String[]{"-print","-p"}, 0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		if (ctx.getProperty("seenPrint")!=null) {
		    return i;
		}
		ctx.putProperty("seenPrint","true");
		ctx.getProcessor().addProcessor(new Processor.Printer(ctx.getPrintFields(), false));
		return i;
	    });

	defineFunction("-stats",0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		ctx.getProcessor().addProcessor(new Processor.Stats(this));
		return i;
	    });
	


	defineFunction("-template",4,(ctx,args,i) -> {
		try {
		    String prefix   = args.get(++i).replaceAll("_nl_", "\n");
		    String template = args.get(++i).replaceAll("_nl_", "\n");
		    String delim    = args.get(++i).replaceAll("_nl_", "\n");
		    String suffix   = args.get(++i).replaceAll("_nl_", "\n");
		    if (new File(template).exists()) {
			template = IO.readContents(new File(template));
		    }
		    ctx.getProcessor().addProcessor(
						    new Processor.Printer(
									  prefix, template, delim, suffix));

		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });



	defineFunction(new String[]{"-printheader","-ph"},0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.PrintHeader());
		return i;
	    });

	defineFunction("-pointheader",0,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Converter.PrintHeader(true));
		return i;
	    });



	defineFunction("-tcl", 1,(ctx,args,i) -> {
		ctx.getProcessor().addProcessor(new Processor.TclWrapper(args.get(++i)));
		return i;
	    });



	defineFunction("-dummy",0,(ctx,args,i) -> {return i;});

    }

    private CsvFunctionHolder getFunction(String id) {
	if(functions==null) {
	    makeFunctions();
	}
	return functions.get(id);
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param files _more_
     * @param providers _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public boolean parseArgs(List<String> args, TextReader ctx,    List<String> files)
	throws Exception {
	boolean            addFiles      = files.size() == 0;
	Filter.FilterGroup subFilter     = null;
	boolean            doArgs        = false;
	boolean            doArgs2       = false;
	int                doArgsCnt     = 0;
	int                doArgsIndex   = 1;
	if (comment != null) {
	    ctx.setComment(comment);
	}
	if (delimiter != null) {
	    ctx.setDelimiter(delimiter);
	}

	PrintWriter pw        = null;
	boolean     seenPrint = false;
	if (debugArgs) {
	    System.err.println("ParseArgs");
	}
	for (int i = 0; i < args.size(); i++) {
	    String arg = args.get(i);
	    if (debugArgs) {
		System.err.println("\targ[" + i + "]=" + arg);
	    }
	    currentArg = arg;
	    try {
		if (doArgs) {
		    if (pw == null) {
			pw = new PrintWriter(getOutputStream());
			pw.print("csvcommands1=");
		    } else {
			doArgsCnt++;
			if (doArgsCnt > 4) {
			    pw.print("\n");
			    doArgsCnt = 0;
			    doArgsIndex++;
			    pw.print("csvcommands" + doArgsIndex + "=");
			} else {
			    pw.print(",");
			}
		    }
		    arg = arg.replaceAll(",", "\\\\,");
		    pw.print(arg);

		    continue;
		}
		if (doArgs2) {
		    if (pw == null) {
			pw = new PrintWriter(getOutputStream());
		    }
		    if ( !arg.equals("-table")) {
			arg = arg.replaceAll("\"", "\\\\\"");
			pw.print("\"" + arg + "\",");
		    }
		    continue;
		}

		if(sinks!=null) {
		    boolean gotOne = false;
		    for(DataSink sink:sinks) {
			if(sink.canHandle(this,arg)) {
			    DataSink other = sink.cloneMe();
			    i = other.processArgs(this,args,i);
			    ctx.getProcessor().addProcessor(other);
			    gotOne = true;
			    hasSink = true;
			    break;
			}
		    }
		    if(gotOne) continue;
		}


		CsvFunctionHolder func = getFunction(arg);
		if(func!=null) {
		    int idx = func.run(ctx, args,i);
		    if(idx==SKIP_INDEX) {
			continue;
		    }
		    if(idx<0) return false;
		    i = idx;
		    continue;
		}

		if (arg.equals("-args")) {
		    doArgs = true;
		    continue;
		}
		if (arg.equals("-args2")) {
		    doArgs2 = true;
		    continue;
		}
		if (arg.equals("-script")) {
		    outputScript(args, ctx);
		    return false;
		}		
		if (arg.equals("-debug")) {
		    ctx.setDebug(true);
		    ctx.printDebug("arguments: ");
		    int argCnt = 0;
		    for(String a: this.args) {
			if(a.equals("-debug")) continue;
			argCnt++;
			ctx.printDebug("\t[" + argCnt+"]=" + a); 
		    }
		    System.err.println("CsvUtil args:" + this.args);
		    continue;
		}

		if (arg.startsWith("-")) {
		    throw new IllegalArgumentException("Unknown arg:" + arg);
		}




		int idx = arg.indexOf("!=");
		if (idx >= 0) {
		    handlePattern(ctx, ctx.getFilterToAddTo(),
				  new Filter.PatternFilter(arg.substring(0,
									 idx).trim(), arg.substring(idx
												    + 2).trim(), true));
		    continue;
		}

		idx = arg.indexOf("=~");
		if (idx >= 0) {
		    handlePattern(ctx, ctx.getFilterToAddTo(),
				  new Filter.PatternFilter(arg.substring(0,
									 idx).trim(), arg.substring(idx
												    + 2).trim()));
		    continue;
		}

		if (arg.length() == 0) {
		    throw new IllegalArgumentException("Unknown argument:"
						       + arg);
		}
		if (addFiles) {
		    if (debugFiles) {
			System.err.println("adding file:" + arg);
		    }
		    files.add(arg);
		} else {
		    //		    System.err.println("no files");
		    //                    throw new IllegalArgumentException("Unknown arg:" + arg);
		}
	    } catch (Exception exc) {
		System.err.println("Error processing arg:" + arg);
		throw exc;
	    }
	}

	if (doArgs) {
	    if (pw != null) {
		pw.print("\n");
	    }
	    pw.close();
	    return false;
	}

	if (doArgs2) {
	    if (pw != null) {
		pw.print("\"-print\"");
		pw.print("\n");
	    }
	    pw.close();
	    return false;
	}
	return true;
    }


    private void outputScript(List<String> args, TextReader ctx) throws Exception {
	PrintWriter pw        = new PrintWriter(getOutputStream());
	pw.println("#!/bin/sh");
	pw.println("#the CSVUTIL environment variable needs to point to RAMADDA's csv release");
	pw.println("#");
	pw.print("sh ${CSVUTIL}/csv.sh ");	
	boolean seenPrint = false;
	for (String arg: args) {
	    if(arg.equals("-script")) continue;
	    if(arg.equals("-print")) seenPrint = true;
	    arg = arg.replaceAll("\\$","\\\\\\$");
	    pw.print("\"" + arg+"\" ");
	}
	if(!seenPrint)   pw.print(" -print ");
	pw.print(" $1 ");
	pw.println("");
	pw.println("");
	pw.println("");
	pw.close();
    }



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private List<String> getCols(String s) {
	return Utils.split(s, ",", true, true);
    }

    /**
     * _more_
     *
     * @param info _more_
     * @param filterToAddTo _more_
     * @param converter _more_
     */
    private void handlePattern(TextReader ctx,
			       Filter.FilterGroup filterToAddTo,
			       Filter converter) {
	if (filterToAddTo != null) {
	    filterToAddTo.addFilter(converter);
	} else {
	    ctx.getProcessor().addProcessor(converter);
	}
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public  Hashtable<String, String> parseProps(String s) {
	s = s.replaceAll("_quote_", "\"");
	s = s.replaceAll("\n", " ");
	List<String> toks = Utils.parseCommandLine(s);
	//      System.err.println("s:" + s);
	//      System.err.println("toks:" + toks);
	Hashtable<String, String> props = new Hashtable<String, String>();
	for (int j = 0; j < toks.size(); j += 2) {
	    if (j >= toks.size() - 1) {
		StringBuilder err = new StringBuilder();
		for (int k = 0; k < toks.size(); k += 2) {
		    if (k >= toks.size() - 1) {
			err.append("\t" + toks.get(k) + "=NONE\n");
		    } else {
			err.append("\t" + toks.get(k) + "=" + toks.get(k + 1)
				   + "\n");
		    }
		}

		throw new IllegalArgumentException(
						   "Error: Odd number of arguments:\n" + err);
	    }
	    props.put(toks.get(j), toks.get(j + 1));
	}
	return props;
    }


    public static List<StringBuilder> tokenizeCommands(String commandString) {
        StringBuilder tmp = new StringBuilder();
        for (String line : StringUtil.split(commandString, "\n")) {
            String tline = line.trim();
            if (tline.startsWith("-quit")) {
                break;
            }
            if ( !tline.startsWith("#")) {
                tmp.append(line);
                tmp.append("\n");
            }
        }
        List<StringBuilder> toks =
            Utils.parseMultiLineCommandLine(tmp.toString());

        return toks;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getDbId() {
	return dbXml.getTableId();
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
	/*
	  FileInputStream stdin = new FileInputStream(FileDescriptor.in);
	  ReadableByteChannel inChannel = stdin.getChannel();
	  //	FileChannel inChannel = aFile.getChannel();
	  ByteBuffer buf = ByteBuffer.allocate(32000);
	  int bytesRead = inChannel.read(buf); //read into buffer.
	  int bc= 0;
	  while (bytesRead != -1) {
	  buf.flip(); 
	  while(buf.hasRemaining()) {
	  buf.get();
	  bc++;
	  }
	  buf.clear(); 
	  bytesRead = inChannel.read(buf);
	  }
	  System.err.println("bc:" +bc);
	  System.exit(0);
	*/

	CsvUtil csvUtil = new CsvUtil(args);
	csvUtil.run(null);
	System.exit(0);
    }

}

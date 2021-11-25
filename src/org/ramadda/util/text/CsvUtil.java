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
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.Json;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.NamedChannel;
import org.ramadda.util.MapProvider;
import org.ramadda.util.PatternProps;
import org.ramadda.util.PropertyProvider;
import org.ramadda.util.Utils;
import org.ramadda.util.XlsUtil;


import org.apache.commons.io.input.BOMInputStream;
import ucar.unidata.util.LogUtil;
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
import java.util.Properties;
import java.util.zip.*;


import java.sql.*;
import org.ramadda.util.sql.Clause;
import org.ramadda.util.sql.SqlUtil;


/**
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
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
    

    private Hashtable<String,String> macros = new Hashtable<String,String>();
    
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
    private SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    
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

    private boolean inputIsBom = false;


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
	this.macros.putAll(csvUtil.macros);
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
	    throw new RuntimeException(exc);
	}
    }

    /**
       Get the Context property.

       @return The Context
    **/
    public CsvContext getCsvContext () {
	return csvContext;
    }

    public File getTmpFile(String name) {
	if(csvContext!=null) return csvContext.getTmpFile(name);
	return null;
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
	    //	    System.err.println("CsvUtil.getProperty: no PropertyProvider set");
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


    public Connection getDbConnection(TextReader ctx, CsvOperator op, Hashtable<String,String> props, String db, String table) throws Exception {
	String dbs = getProperty("csv_dbs");
	if(!Utils.stringDefined(db)) {
	    op.fatal(ctx, "No db specified." + (dbs!=null?"Available dbs:" + dbs:""));

	}
	String jdbcUrl = getProperty("csv_db_" + db + "_url");
	Properties connectionProps = new Properties();
	if(jdbcUrl==null) {
	    op.fatal(ctx, "No csv_db_" + db + "_url environment variable specified. " + (dbs!=null?"Available dbs:" + dbs:""));
	}

	String     user            = props.get("db.user");
	String     password        = props.get("db.password");
	if(user==null) user = getProperty("csv_db_" + db +"_user");
	if(password==null) password = getProperty("csv_db_" + db +"_password");	

	Connection connection = null;
	try {
	    connection = SqlUtil.getConnection(jdbcUrl, user, password);
	} catch (Exception exc) {
	    op.fatal(ctx, "Error making JDBC connection:"+ jdbcUrl, exc);
	}

	if (props.get("help")!=null && props.get("help").equals("true")) {
	    throw new CsvUtil.MessageException("table:" + table
					       + "\ncolumns:\n"
					       + Utils.wrap(SqlUtil.getColumnNames(connection,
										   table, true), "\t", "\n"));
	}


	//Check tables whitelist
	String tables   = getProperty("csv_db_" + db + "_tables",  "");
	List   okTables = Utils.split(tables, ",", true, true);
	if ((okTables.size() == 1) && okTables.get(0).equals("*")) {
	    okTables = SqlUtil.getTableNames(connection);
	}


	List<String> tableList = Utils.split(table, ",", true, true);
	if (tableList.size() == 0) {
	    op.fatal(ctx, "No table specified"
		     + "\nAvailable tables:\n"
		     + Utils.wrap(okTables, "\t", "\n"));
	}
	
	for (String t : tableList) {
	    if ( !Utils.containsIgnoreCase(okTables, t)) {
		op.fatal(ctx, "Unknown table:" + t
			 + "\nAvailable tables:\n"
			 + Utils.wrap(okTables, "\t", "\n"));
            }
	}




	return connection;
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
        boolean      doAppend      = false;	
	int appendSkip = 1;
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

            if (arg.equals("-append")) {
                doAppend = true;
		appendSkip = Integer.parseInt(args.get(++i));
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

            if (arg.equals("-header")) {
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
	} else if (doAppend) {
            IO.append(files, getOutputStream(),appendSkip);	    
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
                iteratePattern = new Filter.PatternFilter(myTextReader, getCols(iterateColumn), "");
                myTextReader.addProcessor(iteratePattern);
            }
	    //For now do the old way so we handle utf-8 better
	    boolean newWay = false;
            for (int i = 0; i < iterateValues.size(); i++) {
                String pattern = iterateValues.get(i);
                if (iteratePattern != null) {
                    iteratePattern.setPattern(pattern);
                }
		for(String file: files) {
		    if(Utils.isUrl(file)) newWay=false;
		}
                for (DataProvider provider : providers) {
		    if(newWay) {
			for (NamedChannel input : getChannels(files)) {
			    myTextReader.resetProcessors();
			    TextReader clone = myTextReader.cloneMe(input,
								    outputFile, outputStream);
			    process(clone, provider);
			    input.close();
			    provider.finish();
			}
		    } else {
			for (NamedInputStream input : getStreams(files)) {
			    myTextReader.resetProcessors();
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
	long t1 = System.currentTimeMillis();
        Row row;
        while ((row = provider.readRow()) != null) {
	    if(row==null) break;
            rowCnt++;
	    //	    if((rowCnt%100000)==0) System.err.print(".");
            if (rowCnt <= ctx.getSkip()) {
                continue;
            }
	    if ( !processRow(ctx, row)) {
		break;
	    }
        }
	long t2 = System.currentTimeMillis();
	//	System.err.println("time:" + (t2-t1));
        if (okToRun) {
            ctx.finishProcessing();
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
        ctx.initRow(row);
        if ((ctx.getMaxRows() >= 0)
	    && (ctx.getVisitedRows() > ctx.getMaxRows())) {
            return false;
        }
	row        = ctx.processRow(this,row);
	if ( !ctx.getOkToRun()) {
	    return false;
	}
	if (ctx.getExtraRow() != null) {
	    row = ctx.processRow(this, ctx.getExtraRow());
	    ctx.setExtraRow(null);
	}
	if (!ctx.getOkToRun()) {
	    return false;
	}

        ctx.incrRow();
        return true;
    }


    private InputStream wrapInputStream(InputStream is) {
	if(inputIsBom) {
	    return new BOMInputStream(is);
	}
	return is;
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
            streams.add(new NamedInputStream(file, wrapInputStream(makeInputStream(file))));
        }
        if (inputStream != null) {
            streams.add(new NamedInputStream("input", wrapInputStream(inputStream)));
        }
        if (streams.size() == 0) {
            streams.add(new NamedInputStream("stdin", wrapInputStream(System.in)));
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
	    try {
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
	    } catch(FileNotFoundException fnfe) {
		throw new RuntimeException("Error missing file:" + file);
	    }
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
	if(!Utils.isUrl(file) && !IO.okToReadFrom(file)) {
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
                if (p.endsWith(".csv") || p.endsWith(".tsv")||p.endsWith(".txt")) {
                    return zin;
                }
                //Apple health
                if (p.endsWith("export.xml")) {
                    return zin;
                }
            }
	    throw new IllegalArgumentException("Could not find .csv, .tsv or .txt file in the zip file");

        } else {
            if (new File(file).exists()) {
                try {
		    FileInputStream fis = new FileInputStream(file);
		    //		    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		    //		    BufferedReader reader = new BufferedReader(isr)
                    return new BufferedInputStream(fis);
                } catch (Exception exc) {
                    System.err.println("Error opening file:" + file);
                    throw exc;
                }
            }
            return IO.getInputStream(file);
        }
    }


    /**
     * _more_
     *
     * @param files _more_
     * @param ctx _more_
     * @param asPoint _more_
     *
     * @throws Exception _more_
     */
    public void header(List<String> files, TextReader ctx, boolean asPoint)
	throws Exception {
        PrintWriter   writer    = ctx.getWriter();
        List<Integer> widths    = ctx.getWidths();
        String        delimiter = ctx.getDelimiter();
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
     *     @param ctx _more_
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
		    //		    System.err.println("line:" + line);
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

    public static String getDbProp(PatternProps props,
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

    public static String getDbProp(PatternProps props,
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
    public static boolean getDbProp(PatternProps props,
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
                desc = "Column indices. Can include ranges, e.g. 0-5";
            }
            if ((desc.length() == 0) && id.equals("rows")) {
                desc = "Row indices. Can include ranges, e.g. 0-5";
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
        public Cmd(boolean isCat, String category,String desc) {
            this.category = isCat;
	    this.cmd = category;
            this.desc     = desc;
        }


        public Cmd(boolean isCat, String category) {
	    this(isCat, category,"");
	}

        /**
         * _more_
         *
         * @param cmd _more_
         * @param args _more_
         * @param desc _more_
         */
        public Cmd(String cmd, String desc, Object... args) {
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
	    //	    if(true)
	    //		return cmd +  " " +desc;
		
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
        new Cmd(true, "Input","Specify the input. Default is assumed to be a CSV but can support HTML, JSON, XML, Shapefile, etc"),
        new Cmd("-delimiter", "Specify a delimiter",
                new Arg("delimiter", "Use 'space' for space, 'tab' for tab",
                        "size", "5")),
        new Cmd("-tab", "Use tabs"),
        new Cmd("-widths", "Columns are fixed widths",
                new Arg("widths", "w1,w2,...,wN")),
        new Cmd("-quotesnotspecial", "Don't treat quotes as special characters"),
        new Cmd("-cleaninput", "Input is one text line per row. i.e., no new lines in a data row. Setting this can improve performance on large files"),
        new Cmd("-bom", "Input has a leading byte order mark (BOM) that should be stripped out"),		
        new Cmd("-header", "Raw header",  new Arg("header", "Column names", "type", "list")),
        new Cmd("-htmltable", "Parse the table in the input html file",
                new Arg("skip", "Number of tables to skip", "type",
			"number"), new Arg("pattern", "Pattern to skip to",
					   "type", "pattern", "size",
					   "40"), new Arg("properties",
							  "Other attributes - <br>&nbsp;&nbsp;removeEntity false removePattern pattern exrtractUrls true column1.extractUrls true stripTags false column1.stripTags false",
							  "rows", "6", "size", "40")),
        new Cmd("-htmlpattern", "Parse the input html file",
                new Arg("columns", "Column names", "type", "columns"),
                new Arg("startPattern", "", "type", "pattern"),
                new Arg("endPattern", "", "type", "pattern"),
                new Arg("pattern", "Row pattern. Use (...) to match columns",
                        "type", "pattern")),
        new Cmd("-harvest", "Harvest links in web page. This results in a 2 column dataset with fields: label,url",
		new Arg("pattern","regexp to match")),
        new Cmd("-json", "Parse the input as json",
                new Arg("arrayPath",
			"Path to the array e.g., obj1.arr[2].obj2", "size", "30",
			"label", "Array path"), new Arg("objectPaths",
							"One or more paths to the objects e.g. geometry,features",
							"size", "30", "label", "Object paths", "type",
							"list", "size", "30")),
        new Cmd("-geojson", "Parse the input as geojson",new Arg("includePolygon","true|false Include polygon")),
        new Cmd("-pdf", "Read input from a PDF file."),	
        new Cmd("-xml", "Parse the input as xml",
                new Arg("path", "Path to the elements", "size", "60")),
        new Cmd("-shapefile", "Parse the input shapefile",
                new Arg("props", "\"addPoints true addShapes false\"")),	
        new Cmd("-lines", "Parse the input as text lines"),
        new Cmd("-text", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("chunk pattern", "", "type", "pattern"),
                new Arg("token pattern", "", "type", "pattern")),
	/*
        new Cmd("-text2", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("chunk pattern", "", "type", "pattern"),
                new Arg("token pattern", "", "type", "pattern")),
	*/
        new Cmd("-extractpattern", "Extract rows from the text",
                new Arg("comma separated header"),
                new Arg("token pattern", "", "type", "pattern")),
        new Cmd("-tokenize", "Tokenize the input from the pattern",
                new Arg("header", "header1,header2..."),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-sql", "Read data from the given database",
                new Arg("db", "The database id (defined in the environment)","type","enumeration","values","property:csv_dbs"),
		new Arg("table", "Comma separate list of tables to select from","size","60"),
		new Arg("columns", "Comma separated list of columns to select"),
		new Arg("where", "column1:expr:value;column2:expr:value;...\ne.g.: name:like:joe;age:>:60\nWhere expr is: =|<|>|<>|like|notlike","type","rows","delimiter",";","size","60"),				
		new Arg("properties", "name space value properties. e.g., join col1,col2")),
        new Cmd("-prune", "Prune out the first N bytes",
                new Arg("bytes", "Number of leading bytes to remove", "type",
                        "number")),
        new Cmd("-deheader", "Strip off the RAMADDA point header"),
        new Cmd("-headernames", "Clean up names"),
        new Cmd("-cat", "Concat the columns in one or more csv files", "*.csv"),
        new Cmd("-append", "Append the files, skipping the given rows in the latter files",
		new Arg("skip","Number of rows to skip"),
		new Arg("files","*.csv")),	
        /** *  Filter * */
        new Cmd(true, "Filter"),
        new Cmd("-skiplines", "Skip number of raw lines.",
                new Arg("lines", "How many raw lines to skip", "type", "number")),	
        new Cmd("-if", "Next N args specify a filter command followed by any change commands followed by an -endif."),
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
        new Cmd(
		"-numcolumns",
		"Remove or add values so each row has the number of columns",
		new Arg("number", "", "type", "number")),
        new Cmd("-pattern", "Pass through rows that the columns each match the pattern",
                new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-notpattern",
                "Pass through rows that don't match the pattern",
                new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-fuzzypattern", "Pass through rows that the columns each fuzzily match the pattern",
                new Arg("threshold", "Score threshold 0-100. Default:85. Higher number better match"),
                new Arg("columns", "", "type", "columns"),
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-same", "Pass through where the 2 columns have the same value",
                new Arg("column1", "", "type", "column"),
                new Arg("column2", "", "type", "column")),
        new Cmd("-notsame", "Pass through where the 2 columns don't have the same value",
                new Arg("column1", "", "type", "column"),
                new Arg("column2", "", "type", "column")),			
        new Cmd("-unique", "Pass through unique values", new Arg("columns")),
        new Cmd("-dups", "Pass through duplicate values", new Arg("columns")),
        new Cmd("-sample", "Pass through rows based on probablity",
                new Arg("probablity", "0-1 probability of passing through a row")),
        new Cmd("-maxvalue",  "Pass through the row that has the max value in the group of columns defined by the key column", new Arg("key column"),
                new Arg("value column")),
        new Cmd("-eq", "Pass through rows that the column value equals the given value",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-ne", "Pass through rows that the column value does not equal the given value",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-gt", "Pass through rows that the column value is greater than the given value",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-ge", "Pass through rows that the column value is greater than or equals the given value",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-lt", "Pass through rows that the column value is less than the given value",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-le", "Pass through rows that the column value is less than or equals the given value",
                new Arg("column", "", "type", "column"), new Arg("value")),
        new Cmd("-between", "Extract rows that are within the range",
                new Arg("column", "", "type", "column"), new Arg("min value"),new Arg("max value")),

	new Cmd("-notbetween","Extract rows that are not within the range",
                new Arg("column", "", "type", "column"), new Arg("min value"),new Arg("max value")),
        new Cmd("-groupfilter", "One row in each group has to match",
                new Arg("column", "key column", "type", "column"),
                new Arg("value_column", "Value column", "type", "column"),
                new Arg("operator", "", "values", "=,!=,~,<,<=,>,>="),
                new Arg("value")),
        new Cmd("-before", "Pass through rows whose date is before the given date",
                new Arg("column", "", "type", "column"),
		new Arg("format","Date Format, e.g. yyyy-MM-dd"),
                new Arg("date"),
		new Arg("format2","Date Format, e.g. yyyy-MM-dd")),
        new Cmd("-after", "Pass through rows whose date is after the given date",
                new Arg("column", "", "type", "column"), 
		new Arg("format","Date Format, e.g. yyyy-MM-dd"),
                new Arg("date"),
		new Arg("format2","Date Format, e.g. yyyy-MM-dd")),
        new Cmd("-latest", "Pass through rows whose date is the latest in the group of rows defined by the key column",
                new Arg("columns", "Key columns", "type", "columns"),
                new Arg("column", "Date column", "type", "column"),
                new Arg("format"),"Date Format, e.g. yyyy-MM-dd"),
        new Cmd("-countvalue", "No more than count unique values",
                new Arg("column", "", "type", "column"), new Arg("count")),
        new Cmd("-decimate", "only include every <skip factor> row",
                new Arg("rows", "# of start rows to include"),
                new Arg("skip", "skip factor")),

        new Cmd("-ifin", "Pass through rows that the columns with ID is in given file",
                new Arg("column", "Column in the file", "type", "column"),
                new Arg("file", "The file"),
                new Arg("column2", "Column in mainr file", "type", "column")),
        new Cmd("-ifnotin", "Pass through rows that the columns with ID is not in given file",
                new Arg("column", "Column in the file", "type", "column"),
                new Arg("file", "The file"),
                new Arg("column2", "Column in mainr file", "type", "column")),	


        new Cmd("-skippattern", "Skip any line that matches the pattern",
                new Arg("pattern", "", "type", "pattern")),
        new Cmd("-skip", "Skip number of processed rows.",
                new Arg("rows", "How many rows to skip", "type", "number")),

        /** *  Slice and dice * */
        new Cmd(true, "Slice and Dice","Add/remove columns, rows, restructure, etc"),
        new Cmd("-columns",  "Only include the given columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-notcolumns", "Don't include given columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-firstcolumns", "Move columns to beginning",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-columnsbefore", "Move columns before the given column",
                new Arg("column", "Column to move before", "type", "column"),
                new Arg("columns", "Columns to move", "type", "columns")),
        new Cmd("-columnsafter", "Move columns after given column",
                new Arg("column", "Column to move after", "type", "column"),
                new Arg("columns", "Columns to move", "type", "columns")),	
        new Cmd("-delete", "Remove the columns",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-cut", "Drop rows",
                new Arg("rows",
                        "One or more rows. -1 to the end. e.g., 0-3,5,10,-1",
                        "type", "rows")),
        new Cmd("-include", "Only include specified rows",
                new Arg("rows", "one or more rows, -1 to the end", "type",
                        "rows")),
	new Cmd("-rows_first", "Move rows to the top that match the pattern",
                new Arg("columns", "columns to match on", "type",  "columns"),
                new Arg("pattern", "Pattern")),
	new Cmd("-rows_last", "Move rows to the end of list that match the pattern",
                new Arg("columns", "columns to match on", "type",  "columns"),
                new Arg("pattern", "Pattern")),

        new Cmd("-copy", "Copy column",
                new Arg("column", "", "type", "column"), "name"),
        new Cmd("-insert","Insert new column values",
		new Arg("column", "Column to insert before", "type", "column"),
		new Arg("name", "Name of new column"),		
		new Arg(
			"values",
			"Single value or comma separated for multiple rows", "type",
			"list")),
        new Cmd("-concat", "Create a new column from the given columns",
                new Arg("columns", "", "type", "columns"), "delimiter",
		new Arg("name","Name of new colums")),
        new Cmd("-split", "Split the column",
                new Arg("column", "", "type", "column"),
                new Arg("delimiter", "What to split on"),
                new Arg("names", "Comma separated new column names", "type",
                        "list")),
        new Cmd("-splat",
                "Create a new column from the values in the given column",
                "key col", new Arg("column", "", "type", "column"),
                "delimiter", new Arg("name", "new column name")),
        new Cmd("-shift", "Shift columns over by count for given rows",
                new Arg("rows", "Rows to apply to", "type", "rows"),
                new Arg("column", "Column to start at", "type", "column"),
                new Arg("count")),
        new Cmd("-slice",
                "Slide columns down and over to append new rows to the bottom",
                new Arg("columns", "Columns to move", "type", "columns"),
                new Arg("dest", "Desc column to move to", "type", "column"),
                new Arg("fill", "Comma separated list of values to fill out the new row")),				
        new Cmd("-addcell", "Add a new cell at row/column", new Arg("row"),
                new Arg("column", "", "type", "column"), "value"),
        new Cmd("-deletecell",  "Delete cell at row/column", new Arg("row"),
                new Arg("column", "", "type", "column")),
        new Cmd("-mergerows", "Merge rows",
                new Arg("rows", "2 or more rows", "type", "rows"),
                new Arg("delimiter"), new Arg("close")),
        new Cmd("-rowop", "Apply an operator to columns and merge rows",
                new Arg("keys", "Key columns", "type", "columns"),
                new Arg("values", "Value columns", "type", "columns"),
                new Arg("operator", "Operator", "values",
                        "average,min,max,count")),
        new Cmd("-rotate", "Rotate the data"),
        new Cmd("-flip", "Reverse the order of the rows except the header"),
        new Cmd("-makefields", "Make new columns from data values",
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
                new Arg("key columns", "key columns the file to join with", "type", "columns"),
                new Arg("value_columns", "value columns"),
                new Arg("file", "File to join with", "type", "file"),
                new Arg("source_columns", "source key columns"),
		new Arg("default_value", "default value")),
        new Cmd("-fuzzyjoin", "Join the 2 files together using fuzzy matching logic",
                new Arg("threshold", "Score threshold 0-100. Default:85. Higher number better match"),
                new Arg("key columns", "Numeric column numbers of the file to join with", "type", "columns"),
                new Arg("value_columns", "numeric columns of the values to join"),
                new Arg("file", "File to join with", "type", "file"),
                new Arg("source_columns", "source key columns"),
		new Arg("default_value", "default value")),

        new Cmd("-normal", "Normalize the strings",
                new Arg("columns", "Columns", "type", "columns")
		),
        new Cmd(
		"-countunique",
		"Count number of unique values",
		new Arg("columns", "", "type", "columns")),
        new Cmd("-dissect", "Make fields based on patterns",
                new Arg("column", "", "type", "column"), new Arg("pattern","e.g., \"(field1:.*) (field2:.*) ...\"","type","pattern","size","80")),
        new Cmd("-keyvalue", "Make fields from key/value pairs, e.g. name1=value1 name2=value2 ...",
                new Arg("column", "", "type", "column")),
        new Cmd(
		"-firstchars",
		"Extract first N characters and create new column",
		new Arg("column", "", "type", "column"),
		new Arg("name", "New column name"),
		new Arg("number", "Number of characters")),
        new Cmd(
		"-lastchars",
		"Extract last N characters and create new column",
		new Arg("column", "", "type", "column"),
		new Arg("name", "New column name"),
		new Arg("number", "Number of characters")),
        new Cmd(
		"-between_indices",
		"Extract characters between the 2 indices",
		new Arg("column", "", "type", "column"),
		new Arg("name", "New column name"),
		new Arg("start", "Start index"),
		new Arg("end", "End index")),		
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
		"-copyif",
		"Copy column 2 to column 3 if all of the columns match the pattern",
		new Arg("columns", "", "type", "columns"),
		new Arg("pattern", ""),
		new Arg("column1", "", "type", "column"),
		new Arg("column2", "", "type", "column")),		
        new Cmd(
		"-copycolumns",
		"Copy columns 1  to columns 2",
		new Arg("columns1", "", "type", "columns"),
		new Arg("columns2", "", "type", "columns")),		

        new Cmd(
		"-filldown",
		"Fill down with last non-null value",
		new Arg("columns", "", "type", "columns")),
        new Cmd(
		"-priorprefix",
		"Append prefix from the previous element to rows that match pattern",
		new Arg("column", "", "type", "column"),
		new Arg("pattern", "", "type", "pattern"), new Arg("delimiter")),
        new Cmd("-case", "Change case of column - type:lower,upper,proper,capitalize",
		new Arg("column", "", "type", "column"),
                new Arg("type", "", "values", "lower,upper,proper,capitalize")),
        new Cmd("-padleft", "Pad left with given character",
                new Arg("columns", "", "type", "columns"),
                new Arg("character", "Character to pad to"),
                new Arg("length", "Length")),
        new Cmd("-padright", "Pad right with given character",
                new Arg("columns", "", "type", "columns"),
                new Arg("character", "Character to pad to"),
                new Arg("length", "Length")),	


        new Cmd("-trim", "Trim leading and trailing white space",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-trimquotes", "Trim leading and trailing quotes",
                new Arg("columns", "", "type", "columns")),	
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
        new Cmd("-ascii", "Convert non ascii characters",
                new Arg("columns", "", "type", "columns"),
                new Arg("substitution string", "")),

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
        new Cmd("-truncate", "", new Arg("column", "", "type", "columns"),
                "max length", "suffix"),
        new Cmd("-extract", "Extract text from column and make a new column",
                new Arg("column", "", "type", "column"),
                new Arg("pattern", "", "type", "pattern"),
                new Arg("replace with", "use 'none' for no replacement"),
                "new column name"),
        new Cmd("-urlarg", "Extract URL argument and make a new column",
                new Arg("column", "", "type", "column"),
                new Arg("argname", "URL arg name")),

        new Cmd("-map", "Change values in column to new values",
                new Arg("column", "", "type", "columns"), "new columns name",
                "value newvalue ..."),
        new Cmd("-combine",
                "Combine columns with the delimiter. deleting columns",
                new Arg("column", "", "type", "columns"), "delimiter",
                "new column name"),
        new Cmd("-combineinplace", "Combine columns with the delimiter",
                new Arg("column", "", "type", "columns"), "delimiter",
                "new column name"),
        new Cmd("-format", "Apply decimal format to the columns", 
		new Arg("columns", "", "type", "columns"),
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
        new Cmd("-soundex", "Generate a soundex code",
                new Arg("columns", "", "type", "columns")),
        new Cmd("-wikidesc", "Add a description from wikipedia",
                new Arg("column", "", "type", "columns"), "suffix"),
        new Cmd("-image", "Search for an image",
                new Arg("column", "", "type", "columns"), "suffix"),
        new Cmd("-embed", "Download the URL and embed the image contents",
                new Arg("url column")),
        new Cmd("-fetch", "Fetch the the URL and embed the contents",
                new Arg("name","Name of new column"),
                new Arg("url","URL template, e.g., https://foo.com/${column_name}")),	
        new Cmd(
		"-imagefill",
		"Search for an image with the query column text if the given image column is blank. Add the given suffix to the search. ",
		new Arg("querycolumn", "", "type", "columns"), "suffix",
		new Arg("imagecolumn", "", "type", "column")),
        new Cmd("-download", "Download the URL",
                new Arg("column", "", "type", "column"),
		new Arg("suffix", "File suffix")),
        new Cmd("-gender", "Figure out the gender of the name in the column",
                new Arg("column", "", "type", "columns")),


        /** *  Dates * */
        new Cmd(true, "Dates"),
        new Cmd("-dateformat", "Specify date format for later use",
                new Arg("format", "e.g. yyyy-MM-dd HH:mm:ss"),
		new Arg("timezone", "")),

        new Cmd("-convertdate", "Convert date", 
                new Arg("column", "", "type", "columns"),
                new Arg("destformat", "date format")),

        new Cmd("-extractdate", "Extract date",
		new Arg("date column", "", "type", "column"),
		new Arg("what", "What to extract, e.g., year, month, day_of_week, etc", "values",
			"era,year,month,day_of_month,day_of_week,week_of_month,\nday_of_week_in_month,am_pm,hour,hour_of_day,\nminute,second,millisecond")),

        new Cmd("-formatdate", "Format date",
                new Arg("columns"), "target date format"),

        new Cmd("-elapsed", "Calculate elapsed time (ms) between rows",
                new Arg("column")),

        /** *  Numeric * */
        new Cmd(true, "Numeric/Boolean"),
        new Cmd("-scale", "Set value={value+delta1}*scale+delta2",
                new Arg("column", "", "type", "columns"), "delta1", "scale",
                "delta2"),
        new Cmd("-generate", "Add row values", "label", "start", "step"),
        new Cmd("-decimals", "", new Arg("column", "", "type", "columns"),
                "how many decimals to round to"),
        new Cmd("-ceil", "Set the max value", new Arg("columns", "", "type", "columns"),
                new Arg("value","Value")),
        new Cmd("-floor", "Set the min value", new Arg("columns", "", "type", "columns"),
                new Arg("value","Value")),		
        new Cmd("-delta",
                "Add column that is the delta from the previous step",
                new Arg("key columns"), new Arg("columns")),
        new Cmd("-operator",
                "Apply the operator to the given columns and create new one",
                new Arg("columns","Columns","type","columns"), "new col name", "operator +,-,*,/,average"),
        new Cmd("-round", "round the values", new Arg("columns", "", "type", "columns")),
        new Cmd("-abs", "make absolute values", new Arg("columns", "", "type", "columns")),
	//TODO:
        new Cmd("-clip", "clip the number to within the range", new Arg("columns", "", "type", "columns"),new Arg("min"), new Arg("max")),
        new Cmd("-rand", "make random value",
		new Arg("column name"),
		new Arg("minrange","Minimum range (e.g. 0)"),
		new Arg("maxrange","Maximum range (e.g. 1)")),		
        new Cmd("-even", "Add true if the column starts with an even number",
		new Arg("columns", "", "type", "columns")),
        new Cmd(
		"-sum",
		"Sum values keying on key column value. If no value columns specified then do a count",
		"key columns", "value columns", "carry over columns"),
        new Cmd(
		"-summary",
		"count/sum/avg/min/max values keying on key column value. If no value columns specified then do a count",
		new Arg("key columns","Columns to key on","type","columns"), new Arg("value columns", "Columns to apply operators on","type","columns"),
		new Arg("carry over columns","Extra columns to include","type","columns"),
		new Arg("ops","any of count,sum,avg,min,max")),
        new Cmd(
		"-histogram",
		"Make a histogram with the given column and bins",
		new Arg("column","The column","type","column"),
		new Arg("bins","Comma separated set of bin values"),
		new Arg("value columns","Extra columns to sum up","type","columns"),
		new Arg("ops","ops to apply to extra columns - any of count,sum,avg,min,max")),		
        new Cmd("-percent", "", "columns to add"),
        new Cmd("-increase", "Calculate percent increase",
                new Arg("column", "", "type", "columns"), "how far back"),
        new Cmd("-average", "Calculate a moving average", "columns",
                "period", "label"),
        new Cmd("-ranges", "Create a new column with the (string) ranges where the value falls in",
                new Arg("column", "", "type", "columns"),
		new Arg("name", "New column name"),		
		new Arg("start", "Numeric start of range"),
		new Arg("size", "Numeric size of range")),		
        new Cmd("-bytes", "Convert suffixed values (e.g., 2 MB) into the number",
                new Arg("unit", "", "type", "enumeration","values","binary,metric"),
                new Arg("column", "", "type", "columns")),
        new Cmd("-column_and", "And values", new Arg("name","New column name"), new Arg("columns", "", "type", "columns")),
        new Cmd("-colum_nor", "Or values", new Arg("name","New column name"), new Arg("columns", "", "type", "columns")),
        new Cmd("-column_not", "Not value", new Arg("name","New column name"), new Arg("column", "", "type", "column")),		




        /** * Geocode  * */
        new Cmd(true, "Geospatial"),
        new Cmd("-geocode", 
		"Geocode using given columns", 
		new Arg("columns", "", "type", "columns"),
                new Arg("prefix", "optional prefix e.g., state: or county: or country:"),
                new Arg("suffix")),
        new Cmd("-geocodeaddressdb", "Geocode for import into RAMADDA's DB. The lat/lon is one semi-colon delimited column", 
                new Arg("columns"), "prefix", "suffix"),
        new Cmd("-geocodejoin", "Geocode with file",
                new Arg("column", "key column", "type", "columns"),
                new Arg("csv file", "File to get lat/lon from", "type",
                        "file"), "key idx", "lat idx", "lon idx"),
        new Cmd("-statename", "Add state name from state ID",
                new Arg("column")),
	new Cmd("-geoname", "Look up location name",
                new Arg("lookup","('counties' or 'states' or 'countries' or 'timezones')"),
                new Arg("lat", "Latitude column", "type", "column"),
                new Arg("lon", "Longitude column", "type", "column")),	
	new Cmd("-elevation", "Look up elevation(using 1/3 arc-second DEM)",
                new Arg("lat", "Latitude column", "type", "column"),
                new Arg("lon", "Longitude column", "type", "column")),	

        new Cmd("-mercator", "Convert x/y to lon/lat", new Arg("columns","x and y columns")),
        new Cmd("-region", "Add the state's region",
                new Arg("columns", "Columns with state name or abbrev.", "type", "columns")),
        new Cmd("-population", "Add in population from address",
                new Arg("columns", "", "type", "columns"),
                new Arg("prefix", "e.g., state: or county:"), "suffix"),
	new Cmd("-neighborhood", "Look up neighborhood for a given location",
                new Arg("lat", "Latitude column", "type", "column"),
                new Arg("lon", "Longitude column", "type", "column")),	


        /** * Other  * */
        new Cmd(true, "Misc."),
        new Cmd("-sort", "",
                new Arg("column", "Column to sort on", "type", "column")),
        new Cmd("-descsort", "",
                new Arg("column", "Column to descending sort on", "type",
                        "column")),
        new Cmd("-count", "Show count"),

        new Cmd("-alias", "Set a field alias",
		new Arg("name","Name"),
		new Arg("alias","Alias")),
        new Cmd("-value", "Define a macro value for later use",
		new Arg("name","Name"),
		new Arg("value","Value")),
        new Cmd("-filepattern", "Extract a macro value from a filename",
		new Arg("name","Macro name"),
		new Arg("pattern","Pattern")),		
        new Cmd("-maxrows", "", "Max rows to print"),
        new Cmd("-changeline",  "Change the line",
                "from", "to"),
        new Cmd("-changeraw",  "Change input text",
                new Arg("from","From pattern"),
		new Arg("to","To string")),
        new Cmd("-crop",  "Crop last part of string after any of the patterns",
                "columns", "pattern1,pattern2"),
        new Cmd("-strict",
		"Be strict on columns. any rows that are not the size of the other rows are dropped"),
        new Cmd("-flag",
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
        new Cmd("-table", "Print table and stats"),	
        new Cmd("-stats", "Print summary stats"),
        new Cmd("-record", "Print records"),
        new Cmd("-toxml", "Generate XML", new Arg("outer tag"),new Arg("inner tag")),
        new Cmd("-tojson", "Generate JSON"),
        new Cmd("-todb", "Write to Database",
		new Arg("db id",""),
		new Arg("table","table name"),
		new Arg("columns","database columns"),		
		new Arg("properties","name value properties")),		
        new Cmd("-template", "Apply the template to make the output",
                new Arg("prefix", "", "size", "40"),
                new Arg("template", "Use ${0},${1}, etc for values", "rows",
                        "6"), new Arg("delimiter", "Output between rows",
                                      "size", "40"), new Arg("suffix", "",
							     "size", "40")),
        new Cmd("-addheader", "Add the RAMADDA point properties",
                new Arg("properties", "name1 value1 ... nameN valueN",
                        "rows", "6")),
        new Cmd(
		"-db", "Generate the RAMADDA db xml from the header",
		new Arg(
			"properties",
			"Name value pairs:\n\t\ttable.id <new id> table.name <new name> table.cansearch false table.canlist false table.icon <icon, e.g., /db/database.png>\n\t\t<column>.id <new id for column> <column>.label <new label>\n\t\t<column>.type <string|enumeration|double|int|date>\n\t\t<column>.format <yyyy MM dd HH mm ss format for dates>\n\t\t<column>.canlist false <column>.cansearch false\n\t\tinstall <true|false install the new db table>\n\t\tnukedb <true|false careful! this deletes any prior created dbs", "rows", "6")),
        new Cmd(
		"-dbprops", "Print to stdout props for db generation",
		new Arg("id pattern"),
		new Arg("suffix pattern")),
        new Cmd("-fields", "Print the fields"),
        new Cmd("-run", "", "Name of process directory"),
        new Cmd("-dots", "Print a dot every count row. Used to show progress",
		new Arg("every", "Dot every")),
        new Cmd("-debugrows", "Debug # rows",
		new Arg("rows", "# of rows")),	
	new Cmd("-script", "Generate the script to call"),
        new Cmd("-args", "Generate the CSV file commands"),
        new Cmd("-pointheader", "Generate the RAMADDA point properties"),
    };


    private String getValue(String s) {
        for (Enumeration keys = macros.keys(); keys.hasMoreElements(); ) {
	    String key =(String) keys.nextElement(); 
	    String value = macros.get(key);
	    s= s.replace("%" + key+"%",value);
        }
	return s;
    }

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
        String  pad             = "    ";
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
						"label",c.cmd));

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
		    cmd = cmd.replaceAll("<br>","\n");
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
	StringBuilder sb = new StringBuilder();
	StringBuilder header = new StringBuilder();	

	List<StringBuilder> headers = new ArrayList<StringBuilder>();

	boolean open = false;
	StringBuilder hb = new StringBuilder();
	int cnt = 0;
        for (Cmd c : commands) {
	    if(c.category) {
		if(open) sb.append("</ul><br>");
		open = true;
		if(header.length()>0) header.append(" | ");
		header.append("<a href='#" + c.cmd +"'>" + c.cmd+"</a>");
		sb.append("<hr>");
		sb.append("<a name='" + c.cmd+"'></a>");
		sb.append("<b style='font-size:120%;'>" + c.cmd+"</b><br>\n");
		sb.append(c.desc);
		sb.append("${header" + headers.size()+"}");
		hb = new StringBuilder("Commands: ");
		headers.add(hb);
		String extra = IO.readContents("/org/ramadda/util/text/help/category_" + Utils.makeID(c.cmd).toLowerCase()+".html",(String) null);
		if(extra!=null) sb.append(extra);
		continue;
	    }
	    cnt++;
	    String path = "/org/ramadda/util/text/help/" + c.cmd.replace("-","")+".html";
	    String extra = IO.readContents(path,(String)null);
	    if(c.cmd.startsWith("-help")) continue;
	    sb.append("<a name='" + c.cmd+"'></a>");
	    hb.append("<a href='#" + c.cmd +"'>" + c.cmd+"</a> ");
	    sb.append("<div class=command> <i><a href='#" + c.cmd +"'>" + c.cmd+"</a></i> ");
	    for(Arg arg: c.args) {
		sb.append(" &lt;" +arg.id+"&gt; ");
	    }
	    sb.append("</div><div class=command-block>");
	    if(Utils.stringDefined(c.desc)) {
		sb.append(c.desc);
		sb.append("<br>\n");
	    }
	    if(c.args.size()>0) {
		sb.append("<b>Arguments:</b><br>\n");
		sb.append("<ul>\n");
		for(Arg arg: c.args) {
		    sb.append("<i>" +arg.id+"</i>\n");
		    if(Utils.stringDefined(arg.desc)) sb.append(arg.desc.replace("<br>"," "));
		    for(int i=0;i<arg.props.length;i+=2) {
			if(arg.props[i].equals("values"))
			    sb.append(" values:" + arg.props[i+1]+" "); 
		    }
		    sb.append("<br>\n");
		}
	    }
	    sb.append("<br>\n");
	    sb.append("</ul>\n");
	    if(extra!=null) {
		sb.append(extra);
		sb.append("<p>");
	    }
	    sb.append("</div>");
	}
	sb.append("</ul>\n");

        PrintWriter pw = new PrintWriter(getOutputStream());
	String intro = IO.readContents("/org/ramadda/util/text/help/intro.html","");
	intro = intro.replace("${header}",header.toString()+"<br>The RAMADDA CSV Utils package provides " + cnt +" commands for manipulating CSV and other types of files");
	pw.println(intro);

	String html = sb.toString();
	for(int i=0;i< headers.size(); i++) {
	    String s  = "<div class=header>" + headers.get(i).toString() +"</div>";
	    html = html.replace("${header" + i+"}", s);
	}
	pw.append(html);
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
	defineFunction("-skiplines",1,(ctx,args,i) -> {
		ctx.setSkipLines(Integer.parseInt(args.get(++i)));
		return i;
	    });	

	defineFunction("-skippattern",1,(ctx,args,i) -> {
		ctx.setSkipPattern(args.get(++i));
		return i;
	    });

	defineFunction("-pass",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Pass(ctx));
		return i;
	    });

	defineFunction("-changeline",2,(ctx,args,i) -> {
		ctx.setChangeString(args.get(++i), args.get(++i));
		return i;
	    });

	defineFunction("-image",2, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.ImageSearch(ctx, getCols(args.get(++i)), args.get(++i)));
		return i;
	    });
	defineFunction("-rows_first",2, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.RowShuffler(ctx, true,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });
	defineFunction("-rows_last",2, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.RowShuffler(ctx,false,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });		
	defineFunction("-embed",1, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.Embed(ctx, args.get(++i)));
		return i;
	    });
	defineFunction("-fetch",2, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.Fetch(ctx, args.get(++i), args.get(++i)));
		return i;
	    });		
	defineFunction("-countunique",1, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.CountUnique(ctx, getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-download",2, (ctx,args,i) -> {
		ctx.addProcessor(new Processor.Downloader(ctx, this, args.get(++i), args.get(++i)));
		return i;
	    });	

	defineFunction(new String[]{"-c","-columns"},1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnSelector(ctx, getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-columnsbefore",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnsBefore(ctx, args.get(++i),getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-columnsafter",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnsAfter(ctx, args.get(++i),getCols(args.get(++i))));
		return i;
	    });		
	defineFunction("-firstcolumns",1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnFirst(ctx, getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-notcolumns",1,(ctx,args,i) -> {
		List<String> cols = getCols(args.get(++i));
		ctx.addProcessor(new Converter.ColumnNotSelector(ctx, cols));
		return i;
	    });

	defineFunction("-number",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Number(ctx));
		return i;
	    });

	defineFunction("-letter",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Letter(ctx));
		return i;
	    });

	defineFunction("-uuid",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.UUID(ctx));
		return i;
	    });

	defineFunction("-start",1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.Start(ctx,args.get(++i)));
		return i;
	    });

	defineFunction("-stop",1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.Stop(ctx,args.get(++i)));

		return i;
	    });

	defineFunction("-min",1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.MinColumns(ctx,new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-max",1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.MaxColumns(ctx, new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-decimate",2,(ctx,args,i) -> {
		int start = Integer.parseInt(args.get(++i));
		int skip  = Integer.parseInt(args.get(++i));
		if (skip > 0) {
		    ctx.addProcessor(
				     new Filter.Decimate(ctx, start, skip));
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
		ctx.addProcessor(dbXml =  new Processor.DbXml(ctx,props));
		ctx.setMaxRows(30);
		return i;
	    });

	defineFunction(new String[]{"-dbprops"},2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.DbProps(ctx,args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(new String[]{"-fields"},0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Fields(ctx));
		return i;
	    });
	

	defineFunction("-if",3, (ctx,args,i) -> {
		String type = args.get(++i);
		CsvFunctionHolder pfunc = getFunction(type);
		if(pfunc==null) throw new RuntimeException("Unknown -if predicate:" + type);
		TextReader predicate =new TextReader();
		try {
		    i = pfunc.run(predicate, args,i);
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
		List<String> ifArgs = new ArrayList<String>();
		while(true) {
		    if(i>=args.size()) throw new RuntimeException("Unclosed -if");
		    String a = args.get(++i);
		    if(a.equals("-endif")) break;
		    ifArgs.add(a);
		}
		//		System.err.println("if args:" + ifArgs);
		TextReader ifCtx = new TextReader();
		for(int j=0;j<ifArgs.size();j++) {
		    String arg = ifArgs.get(j);
		    CsvFunctionHolder func = getFunction(arg);
		    if(func==null) {
			throw new RuntimeException("Unknown function in -if:" + ifArgs);
		    }
		    int idx=0;
		    try {
			idx = func.run(ifCtx, ifArgs,j);
		    } catch(Exception exc) {
			throw new RuntimeException(exc);
		    }
		    if(idx==SKIP_INDEX) {
			continue;
		    }
		    if(idx<0)
			throw new RuntimeException("Unknown function in -if:" + ifArgs);
		    j=idx;
		}
		ctx.addProcessor(new Processor.If(ctx, this,predicate,ifCtx));
		return i;
	    });


	CsvFunction unfurlFunc = (ctx,args,i) -> {
	    String       mainCol   = args.get(++i);
	    List<String> valueCols = getCols(args.get(++i));
	    String       uniqueCol = args.get(++i);
	    List<String> extraCols = getCols(args.get(++i));
	    ctx.addProcessor(new RowCollector.Unfurler(ctx,
						       mainCol, valueCols, uniqueCol, extraCols));

	    return i;
	};

	defineFunction("-unfurl",4,unfurlFunc);
	defineFunction("-makefields",4,unfurlFunc);	

	defineFunction("-furl",3,(ctx,args,i) -> {
		List<String> valueCols = getCols(args.get(++i));
		ctx.addProcessor(new RowCollector.Furler(ctx,
							 valueCols, args.get(++i), args.get(++i)));

		return i;
	    });

	defineFunction("-break",3,(ctx,args,i) -> {
		String       label1 = args.get(++i);
		String       label2 = args.get(++i);
		List<String> cols   = getCols(args.get(++i));
		ctx.addProcessor(new RowCollector.Breaker(ctx,label1, label2, cols));

		return i;
	    });

	defineFunction("-sort",1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Sorter(ctx,args.get(++i), true));

		return i;
	    });

	defineFunction("-descsort",1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Sorter(ctx,args.get(++i), false));
		return i;
	    });

	defineFunction("-ifin",3,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.IfIn(ctx, true, args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction("-ifnotin",3,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.IfIn(ctx, false,args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	

	defineFunction("-join",5,(ctx,args,i) -> {
		List<String> keys1   = getCols(args.get(++i));
		List<String> values1 = getCols(args.get(++i));
		String       file    = args.get(++i);
		List<String> keys2   = getCols(args.get(++i));
		ctx.addProcessor(new Processor.Joiner(ctx,keys1, values1, file, keys2,args.get(++i)));
		return i;
	    });

	defineFunction("-fuzzyjoin",6,(ctx,args,i) -> {
		int threshold = Integer.parseInt(args.get(++i));
		List<String> keys1   = getCols(args.get(++i));
		List<String> values1 = getCols(args.get(++i));
		String       file    = args.get(++i);
		List<String> keys2   = getCols(args.get(++i));
		ctx.addProcessor(new Processor.FuzzyJoiner(ctx, threshold, keys1, values1, file, keys2,args.get(++i)));
		return i;
	    });
	
	defineFunction("-normal",1,(ctx,args,i) -> {
		List<String> cols   = getCols(args.get(++i));
		ctx.addProcessor(new RowCollector.Normal(ctx, cols));
		return i;
	    });

	defineFunction("-ranges",4,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ranges(ctx, args.get(++i),args.get(++i), Double.parseDouble(args.get(++i)), Double.parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction("-sum",3,(ctx,args,i) -> {
		List<String> keys   = getCols(args.get(++i));
		List<String> values = getCols(args.get(++i));
		List<String> extra  = getCols(args.get(++i));
		List<String> what = new ArrayList<String>();
		what.add("sum");
		ctx.addProcessor(new RowCollector.Summary(ctx,what,keys, values, extra));
		return i;
	    });
	defineFunction("-summary",4,(ctx,args,i) -> {
		List<String> keys   = getCols(args.get(++i));
		List<String> values = getCols(args.get(++i));
		List<String> extra  = getCols(args.get(++i));
		List<String> what   = Utils.split(args.get(++i),",",true,true);
		ctx.addProcessor(new RowCollector.Summary(ctx, what,keys, values, extra));
		return i;
	    });	
	defineFunction("-histogram",4,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Histogram(ctx, args.get(++i),args.get(++i),getCols(args.get(++i)),args.get(++i)));
		return i;
	    });	

	defineFunction(new String[]{"-u","-unique"},1,(ctx,args,i) -> {
		List<String> toks = getCols(args.get(++i));
		ctx.addProcessor(new Filter.Unique(ctx, toks));
		return i;
	    });
	defineFunction(new String[]{"-sample"},1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.Sample(ctx, Double.parseDouble(args.get(++i))));
		return i;
	    });	

	defineFunction("-dups",1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Dups(ctx,getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-verify",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Verifier(ctx));
		return i;
	    });

	defineFunction("-count",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Counter(ctx));
		return i;
	    });


	defineFunction("-log",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Logger(ctx));
		return i;
	    });

	defineFunction("-strict",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Counter(ctx,true));
		return i;
	    });

	defineFunction("-flag",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Counter(ctx, true, true));
		return i;
	    });


	defineFunction("-rotate",0,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Rotator(ctx));
		return i;
	    });

	defineFunction("-flip",0,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Flipper(ctx));
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
	defineFunction("-quotesnotspecial",0,(ctx,args,i) -> {
		ctx.setQuotesNotSpecial(true);
		return i;
	    });
	defineFunction("-cleaninput",0,(ctx,args,i) -> {
		ctx.setCleanInput(true);
		return i;
	    });
	defineFunction("-bom",0,(ctx,args,i) -> {
		inputIsBom = true;
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
		ctx.addProcessor(new Filter.RowCutter(ctx,Utils.getNumbers(args.get(++i)), true));
		return i;
	    });


	defineFunction("-include",1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.RowCutter(ctx, Utils.getNumbers(args.get(++i)), false));
		return i;
	    });


	
	defineFunction("-prop",2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Propper(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-rowop",3,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.RowOperator(getCols(args.get(++i)),getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	
	defineFunction("-percent",  1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnPercenter(getCols(args.get(++i))));
		return i;
	    });


	defineFunction("-average",3,(ctx,args,i) -> {
		List<String> cols   = getCols(args.get(++i));
		int          period = Integer.parseInt(args.get(++i));
		String       label  = args.get(++i);
		ctx.addProcessor(
				 new Converter.ColumnAverage(
							     Converter.ColumnAverage.MA, cols, period, label));

		return i;
	    });

	defineFunction("-increase",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnIncrease(args.get(++i), Integer.parseInt(args.get(++i))));
		return i;
	    });
	defineFunction("-column_and",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.And(args.get(++i), getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-column_or",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Or(args.get(++i), getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-column_not",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Not(args.get(++i), args.get(++i)));
		return i;
	    });			



	defineFunction("-sumrow",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnOperator());

		return i;
	    });

	defineFunction("-pad",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Padder(new Integer(args.get(++i)).intValue(), args.get(++i)));
		return i;
	    });

	defineFunction("-prefix",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Prefixer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-suffix",1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Suffixer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction("-explode",1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Exploder(ctx, args.get(++i)));
		return i;
	    });
	defineFunction("-dissect",2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Dissector(args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction("-keyvalue",1,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.KeyValue(args.get(++i)));
		return i;
	    });		

	defineFunction("-firstchars",3,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.First(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });
	defineFunction("-lastchars",3,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Last(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });			
	defineFunction("-between_indices",4,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Between(args.get(++i), args.get(++i),args.get(++i), args.get(++i)));
		return i;
	    });			


	defineFunction("-gender",1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Genderizer(args.get(++i)));
		return i;
	    });

	defineFunction("-ximage",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ImageSearch(ctx,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-imagefill",3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ImageSearch(ctx,getCols(args.get(++i)), args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction("-wikidesc",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.WikiDescSearch(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-statename",1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.StateNamer(args.get(++i)));
		return i;
	    });
	defineFunction("-geoname",3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.GeoNamer(args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction("-elevation",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Elevation(args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction("-neighborhood",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Neighborhood(args.get(++i),args.get(++i)));
		return i;
	    });			

	defineFunction("-geocode",3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim()));
		return i;
	    });

	defineFunction("-geocodejoin",5,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Geocoder(args.get(++i),args.get(++i), Integer.parseInt(args.get(++i)),
							Integer.parseInt(args.get(++i)),
							Integer.parseInt(args.get(++i)), false));
		return i;
	    });



	defineFunction("-geocodeaddressdb",3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim(), true));
		return i;
	    });

	defineFunction("-geocodedb",5,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Geocoder(args.get(++i),args.get(++i), Integer.parseInt(args.get(++i)),
							Integer.parseInt(args.get(++i)),
							Integer.parseInt(args.get(++i)), true));
		return i;
	    });


	defineFunction("-population",3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Populator(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim()));
		return i;
	    });

	defineFunction("-region",1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Regionator(getCols(args.get(++i))));
		return i;
	    });


	defineFunction("-crop",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Cropper(getCols(args.get(++i)), Utils.split(args.get(++i), ",", true, true)));
		return i;
	    });

	defineFunction("-change",3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnChanger(ctx,getCols(args.get(++i)),Utils.convertPattern(args.get(++i)),  args.get(++i)));
		return i;
	    });


	defineFunction("-ascii",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ascii(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });
	

	defineFunction("-endswith",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnEndsWith(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });



	defineFunction("-extract",4,(ctx,args,i) -> {
		String   col     = args.get(++i);
		String pattern = args.get(++i);
		String replace = args.get(++i);
		String name    = args.get(++i);
		pattern = Utils.convertPattern(pattern);
		ctx.addProcessor(
				 new Converter.ColumnExtracter(
							       col, pattern, replace, name));

		return i;
	    });


	defineFunction("-urlarg",2,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Converter.UrlArg(args.get(++i), args.get(++i)));
		return i;
	    });



	defineFunction("-truncate",3,(ctx,args,i) -> {
		int    col    = new Integer(args.get(++i));
		int    length = new Integer(args.get(++i));
		String suffix = args.get(++i);
		ctx.addProcessor(
				 new Converter.Truncater(col, length, suffix));

		return i;
	    });


	defineFunction("-changerow",4,(ctx,args,i) -> {
		List<Integer> rows    = Utils.getNumbers(args.get(++i));
		List<String>  cols    = getCols(args.get(++i));
		String        pattern = args.get(++i);
		pattern = Utils.convertPattern(pattern);
		ctx.addProcessor(
				 new Converter.RowChanger(
							  rows, cols, pattern, args.get(++i)));

		return i;
	    });

	defineFunction("-formatdate", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.DateFormatter(getCols(args.get(++i)), dateFormat, args.get(++i)));
		return i;
	    });

	defineFunction("-elapsed", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Elapsed(args.get(++i), dateFormat));
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
		ctx.addProcessor(
				 new Converter.DateConverter(
							     col, dateFormatString,
							     sdf2));
		return i;
	    });

	defineFunction("-extractdate",2,(ctx,args,i) -> {
		String col  = args.get(++i);
		String what = args.get(++i);
		ctx.addProcessor(
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
		    ctx.addProcessor(
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
		    ctx.addProcessor(
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
		ctx.addProcessor(
				 new RowCollector.DateLatest(ctx,
							     cols, col, new SimpleDateFormat(sdf)));

		return i;
	    });






	defineFunction(new String[]{"-htmltable","-html"},3,(ctx,args,i) -> {
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

	defineFunction("-harvest",1,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Harvester( args.get(++i)));
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


	defineFunction("-extractpattern",2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.PatternExtractDataProvider(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-text3",2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.PatternExtractDataProvider(args.get(++i), args.get(++i)));
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

	defineFunction("-geojson",1,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.GeoJsonDataProvider(args.get(++i).equals("true")));

		return i;
	    });
	
	defineFunction("-sql",5,(ctx,args,i) -> {
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

	defineFunction("-pdf",0,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Pdf(this));
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
		ctx.addProcessor(new Converter.RowMerger(Utils.getNumbers(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-prepend",1,(ctx,args,i) -> {
		String text = args.get(++i);
		text = text.replaceAll("_nl_", "\n");
		ctx.setPrepend(text);
		return i;
	    });

	defineFunction("-columndebug", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnDebugger(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	
	defineFunction("-map", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnMapper(getCols(args.get(++i)), args.get(++i),
							    Utils.parseCommandLine(args.get(++i))));
		return i;
	    });


	defineFunction("-split", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnSplitter(
							      args.get(++i), args.get(++i),
							      Utils.split(args.get(++i), ",")));
		return i;
	    });

	defineFunction("-delete", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnDeleter(getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-insert", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnInserter(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction("-shift", 3,(ctx,args,i) -> {
		List<Integer> rows  = Utils.getNumbers(args.get(++i));
		int           col   = Integer.parseInt(args.get(++i));
		int           count = Integer.parseInt(args.get(++i));
		ctx.addProcessor(new Converter.Shifter(rows, col, count));
		return i;
	    });

	defineFunction("-slice", 3,(ctx,args,i) -> {
		List<String> cols  = getCols(args.get(++i));
		String dest = args.get(++i);
		List<String> fill = Utils.split(args.get(++i),",",false,false);
		ctx.addProcessor(new RowCollector.Slicer(ctx, cols, dest,fill));
		return i;
	    });
	

	defineFunction("-generate", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Generator(args.get(++i), Double.parseDouble(args.get(++i)),
							 Double.parseDouble(args.get(++i))));
		return i;
	    });



	defineFunction("-macro", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnMacro(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction("-format", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnFormatter(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction("-scale", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnScaler(getCols(args.get(++i)), Double.parseDouble(args.get(++i)),
							    Double.parseDouble(args.get(++i)),
							    Double.parseDouble(args.get(++i))));

		return i;
	    });


	defineFunction("-decimals", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Decimals(getCols(args.get(++i)), new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-ceil", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ceil(getCols(args.get(++i)), Double.parseDouble(args.get(++i))));
		return i;
	    });
	defineFunction("-floow", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Floor(getCols(args.get(++i)), Double.parseDouble(args.get(++i))));
		return i;
	    });

	
	defineFunction("-copy", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnCopier(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-concat", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnNewer(getCols(args.get(++i)), args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction("-splat", 4,(ctx,args,i) -> {
		String key       = args.get(++i);
		String value     = args.get(++i);
		String delimiter = args.get(++i);
		String name      = args.get(++i);
		ctx.addProcessor(new RowCollector.Splatter(ctx, key, value, delimiter, name));
		return i;
	    });
	defineFunction("-delta", 2,(ctx,args,i) -> {
		List<String> keyidxs = getCols(args.get(++i));
		List<String> idxs    = getCols(args.get(++i));
		ctx.addProcessor(new Converter.Delta(keyidxs, idxs));
		return i;
	    });



	defineFunction("-operator", 3,(ctx,args,i) -> {
		List<String> idxs = getCols(args.get(++i));
		String       name = args.get(++i);
		String       op   = args.get(++i);
		Processor processor = new Converter.ColumnMathOperator(idxs, name, op);
		ctx.addProcessor(processor);
		return i;
	    });

	defineFunction("-js", 1,(ctx,args,i) -> {
		js.append(args.get(++i));
		js.append("\n");
		return i;
	    });

	defineFunction("-func", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnFunc(ctx, js.toString(), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-mercator", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Mercator(getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-round", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnRounder(getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-bytes", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Bytes(args.get(++i),getCols(args.get(++i))));
		return i;
	    });	
	defineFunction("-abs", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnAbs(getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-rand", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnRand(args.get(++i), Double.parseDouble(args.get(++i)),Double.parseDouble(args.get(++i))));
		return i;
	    });		
	defineFunction("-md", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.MD(ctx,getCols(args.get(++i)),args.get(++i)));
		return i;
	    });
	defineFunction("-soundex", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.SoundexMaker(getCols(args.get(++i))));
		return i;
	    });	
	defineFunction("-even", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Even(getCols(args.get(++i))));
		return i;
	    });	


	defineFunction("-striptags", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.StripTags(getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-decode", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Decoder(getCols(args.get(++i))));
		return i;
	    });	


	defineFunction("-case", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Case(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });
	defineFunction("-padleft", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PadLeftRight(true,getCols(args.get(++i)),args.get(++i),Integer.parseInt(args.get(++i))));
		return i;
	    });
	defineFunction("-padright", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PadLeftRight(false,getCols(args.get(++i)),args.get(++i),Integer.parseInt(args.get(++i))));
		return i;
	    });	


	defineFunction("-numcolumns", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.NumColumns(Integer.parseInt(args.get(++i))));
		return i;
	    });	



	defineFunction("-trim", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Trim(getCols(args.get(++i))));
		return i;
	    });	

	defineFunction("-trimquotes", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.TrimQuotes(getCols(args.get(++i))));
		return i;
	    });	
	
	defineFunction("-addcell", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnNudger(Integer.parseInt(args.get(++i)),Integer.parseInt(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction("-deletecell", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnUnNudger(Integer.parseInt(args.get(++i)), getCols(args.get(++i))));
		return i;
	    });

	defineFunction("-copyif", 4,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.CopyIf(getCols(args.get(++i)),args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction("-copycolumns", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.CopyColumns(getCols(args.get(++i)), getCols(args.get(++i))));
		return i;
	    });	

	defineFunction("-filldown", 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.FillDown(getCols(args.get(++i))));
		return i;
	    });


	defineFunction("-priorprefix", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PriorPrefixer(Integer.parseInt(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-set", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnSetter(getCols(args.get(++i)),getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction("-makeids", 0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.MakeIds());
		return i;
	    });

	defineFunction("-setcol", 4,(ctx,args,i) -> {
		String col1    = args.get(++i);
		String pattern = args.get(++i);
		String col2    = args.get(++i);
		String what    = args.get(++i);
		ctx.addProcessor(new Converter.ColumnPatternSetter(col1, pattern, col2, what));
		return i;
	    });

	defineFunction("-width", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnWidth(getCols(args.get(++i)), Integer.parseInt(args.get(++i))));
		return i;
	    });

	defineFunction("-combine", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnConcatter(getCols(args.get(++i)),args.get(++i),args.get(++i),false));
		return i;
	    });


	defineFunction("-combineinplace", 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnConcatter(getCols(args.get(++i)),args.get(++i),args.get(++i),true));
		return i;
	    });

	defineFunction("-denormalize", 6,(ctx,args,i) -> {
		String file = args.get(++i);
		int    col1 = Integer.parseInt(args.get(++i));
		int    col2 = Integer.parseInt(args.get(++i));
		int    col3 = Integer.parseInt(args.get(++i));
		String name = args.get(++i);
		String mode = args.get(++i);
		ctx.addProcessor(new Converter.Denormalizer(file, col1, col2, col3, name, mode));
		return i;
	    });


	defineFunction("-or",0,(ctx,args,i) -> {
		ctx.setFilterToAddTo(new Filter.FilterGroup(ctx,false));
		ctx.addProcessor(ctx.getFilterToAddTo());
		return i;
	    });

	defineFunction("-alias",2,(ctx,args,i) -> {
		ctx.putFieldAlias(args.get(++i),args.get(++i));
		return i;
	    });

	defineFunction("-and",0,(ctx,args,i) -> {
		ctx.setFilterToAddTo(new Filter.FilterGroup(ctx,true));
		ctx.addProcessor(ctx.getFilterToAddTo());
		return i;
	    });


	defineFunction("-same", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.Same(ctx,args.get(++i), args.get(++i),false));
		return i;
	    });

	defineFunction("-notsame", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.Same(ctx,args.get(++i), args.get(++i),true));
		return i;
	    });		


	defineFunction("-pattern", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.PatternFilter(ctx,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });
	defineFunction("-notpattern", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.PatternFilter(ctx,getCols(args.get(++i)),args.get(++i), true));
		return i;
	    });

	defineFunction("-fuzzypattern", 3,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.FuzzyFilter(ctx,Integer.parseInt(args.get(++i)), getCols(args.get(++i)), args.get(++i),false));
		return i;
	    });

	defineFunction("-countvalue", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(), new Filter.CountValue(ctx,args.get(++i), Integer.parseInt(args.get(++i))));
		return i;
	    });

	defineFunction("-groupfilter", 4,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.GroupFilter(ctx, getCols(args.get(++i)), Integer.parseInt(args.get(++i)),
							      CsvOperator.getOperator(args.get(++i)),
							      args.get(++i)));
		return i;
	    });


	defineFunction("-eq", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,getCols(args.get(++i)),
						     Filter.ValueFilter.OP_EQUALS,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction("-ne", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,getCols(args.get(++i)),
						     Filter.ValueFilter.OP_NOTEQUALS,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });


	defineFunction("-lt", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,
						     getCols(args.get(++i)), Filter.ValueFilter.OP_LT,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction("-gt", 2,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,
						     getCols(args.get(++i)), Filter.ValueFilter.OP_GT,
						     Double.parseDouble(args.get(++i))));
		return i;
	    });


	defineFunction("-between", 3,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.RangeFilter(ctx,true,
						     getCols(args.get(++i)), 
						     Double.parseDouble(args.get(++i)),
						     Double.parseDouble(args.get(++i))));						     
		return i;
	    });

	defineFunction("-notbetween", 3,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.RangeFilter(ctx,false,
						     getCols(args.get(++i)), 
						     Double.parseDouble(args.get(++i)),
						     Double.parseDouble(args.get(++i))));						     
		return i;
	    });
	


	defineFunction("-defined", 1,(ctx,args,i) -> {
		handlePattern(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,getCols(args.get(++i)),Filter.ValueFilter.OP_DEFINED, 0));
		return i;
	    });
	defineFunction("-maxvalue", 2,(ctx,args,i) -> {
		String key   = args.get(++i);
		String value = args.get(++i);
		ctx.addProcessor(
				 new RowCollector.MaxValue(ctx, key, value));

		return i;
	    });


	defineFunction("-quit",0,(ctx,args,i) -> {
		String last = args.get(args.size() - 1);
		if (last.equals("-print") || last.equals("-p")) {
		    ctx.addProcessor(
				     new Processor.Printer(ctx.getPrintFields(), false));
		} else if (last.equals("-table")) {
		    ctx.addProcessor(new RowCollector.Html(ctx));
		}
		return -1;
	    });



	defineFunction("-dots",1,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Dots(new Integer(args.get(++i))));
		return i;
	    });

	defineFunction("-debugrows",1,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.DebugRows(new Integer(args.get(++i))));
		return i;
	    });
	

	defineFunction("-headernames",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.HeaderNames());
		return i;
	    });


	defineFunction("-addheader",1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.HeaderMaker(parseProps(args.get(++i))));
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
		    ctx.addProcessor(new Processor.Printer(ctx.getPrintFields(), false));
		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });


	defineFunction("-toxml",2,(ctx,args,i) -> {
		hasSink = true;
		ctx.addProcessor(new DataSink.ToXml(args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction("-todb",4,(ctx,args,i) -> {
		//		hasSink = true;
		ctx.addProcessor(new DataSink.ToDb(this, args.get(++i), args.get(++i),args.get(++i),parseProps(args.get(++i))));
		return i;
	    });



	defineFunction("-tojson",0,(ctx,args,i) -> {
		hasSink = true;
		ctx.addProcessor(new DataSink.ToJson());
		return i;
	    });

	


	defineFunction("-table",0, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Html(ctx));
		return i;
	    });


	defineFunction("-dump",0,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Processor.Printer(ctx.getPrintFields(), false));
		return i;
	    });

	defineFunction("-record",0,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Processor.Prettifier());
		return i;
	    });

	defineFunction(new String[]{"-print","-p"}, 0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		if (ctx.getProperty("seenPrint")!=null) {
		    return i;
		}
		ctx.putProperty("seenPrint","true");
		ctx.addProcessor(new Processor.Printer(ctx.getPrintFields(), false));
		return i;
	    });

	defineFunction("-stats",0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		ctx.addProcessor(new RowCollector.Stats(ctx, this,true));
		return i;
	    });

	defineFunction("-doit",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Doit());
		return i;
	    });	

	defineFunction("-table",0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		ctx.addProcessor(new RowCollector.Stats(ctx, this,false));
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
		    ctx.addProcessor(
				     new Processor.Printer(
							   prefix, template, delim, suffix));

		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });



	defineFunction(new String[]{"-printheader","-ph"},0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PrintHeader());
		return i;
	    });

	
	defineFunction("-pointheader",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PrintHeader(true));
		return i;
	    });



	defineFunction("-tcl", 1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.TclWrapper(ctx, args.get(++i)));
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
	if (debugArgs) {
	    System.err.println("ParseArgs");
	}
	List<String> filePatterns = new ArrayList<String>();
	List<String> filePatternNames = new ArrayList<String>();	
	List<String> newArgs = new ArrayList<String>();
	for (int i = 0; i < args.size(); i++) {
	    String arg = args.get(i);
	    if(arg.equals("-value")) {
		macros.put(args.get(++i),args.get(++i));
		continue;
	    }
	    if(arg.equals("-filepattern")) {
		filePatternNames.add(args.get(++i));		
		filePatterns.add(args.get(++i));

		continue;
	    }
	    newArgs.add(arg);
	}


	if(filePatternNames.size()>0) {
	    List<String> tmpFiles = new ArrayList<String>();
	    if(files.size()>0) {
		for(String arg: files) {
		    File  f = new File(arg);
		    tmpFiles.add(f.getName());
		}
	    } else  {
		for (int i = newArgs.size()-1; i>=0;i--) {
		    String arg = newArgs.get(i);
		    File  f = new File(arg);
		    if(f.exists()) {
			tmpFiles.add(f.getName());
			break;
		    }
		}
	    }
	    for(String file: tmpFiles) {
		for(int i=0;i<filePatterns.size();i++) {
		    String value = StringUtil.findPattern(file, ".*"+filePatterns.get(i));
		    System.err.println("file:" + file +" pattern:" + filePatterns.get(i) +" " + value);
		    if(value!=null) {
			macros.put(filePatternNames.get(i), value);
		    }
		}
	    }
	}

	//	System.err.println("macros:" + macros);

	if(macros.size()>0) {
	    List<String> tmp = new ArrayList<String>();
	    for(String s: newArgs) {
		s=  getValue(s);
		tmp.add(s);
	    }
	    newArgs = tmp;
	}

	args = newArgs;

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
			    ctx.addProcessor(other);
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
     * @param ctx _more_
     * @param filterToAddTo _more_
     * @param converter _more_
     */
    private void handlePattern(TextReader ctx,
			       Filter.FilterGroup filterToAddTo,
			       Filter converter) {
	if (filterToAddTo != null) {
	    filterToAddTo.addFilter(converter);
	} else {
	    ctx.addProcessor(converter);
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
	//	System.err.println("s:" + s);
	//	System.err.println("toks:" + toks);
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
	    //	    System.err.println(toks.get(j)+"="+ toks.get(j + 1));
	    props.put(toks.get(j), toks.get(j + 1));
	    //	    System.err.println(toks.get(j) +" " + toks.get(j+1));

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
	GeoUtils.setCacheDir(new File("."));
	CsvUtil csvUtil = new CsvUtil(args);
	csvUtil.setCsvContext(new CsvContext() {
		public List<Class> getClasses() {
		    String _name=null;
		    ArrayList<Class> classes = new ArrayList<Class>();
		    String prop = System.getenv("CSV_CLASSES");
		    if(prop!=null) {
			for(String name: Utils.split(prop,":",true,true)) {
			    _name = name;
			    try {
				Class c  = Class.forName(name);
				classes.add(c);
			    } catch(Exception exc) {
				if(_name!=null) {
				    System.err.println("Error reading class:" + _name +" error:" + exc);
				}
			    }
			}
			//			throw new RuntimeException(exc);
		    }
		    return classes;
		}
		public String getProperty(String key, String dflt) {
		    String v =  System.getProperty(key);
		    if(v==null) return dflt;
		    return v;
		}
		public File getTmpFile(String name) {
		    File tmp = new File(name);
		    int index = 1;
		    while(tmp.exists()) {
			tmp = new File((index++)+"_" + name);
		    }
		    return tmp;
		}
	    });

	try {
	    csvUtil.run(null);
	} catch(MessageException cexc) {
	    System.err.println(cexc.getMessage());
	    System.exit(1);
	} catch(Exception exc) {
	    Throwable inner = LogUtil.getInnerException(exc);
	    System.err.println(exc.getMessage());
	    inner.printStackTrace();
	    System.exit(1);
	}
	System.exit(0);
    }



}

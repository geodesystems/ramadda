/**
   Copyright (c) 2008-2022 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.util.seesv;

import org.json.*;

import org.ramadda.util.S3File;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.IO;
import org.ramadda.util.geo.Bounds;
import org.ramadda.util.geo.GeoUtils;
import org.ramadda.util.JsonUtil;
import org.ramadda.util.NamedInputStream;
import org.ramadda.util.NamedChannel;
import org.ramadda.util.MapProvider;
import org.ramadda.util.PatternProps;
import org.ramadda.util.PropertyProvider;
import org.ramadda.util.PhoneUtils;
import org.ramadda.util.Utils;
import org.ramadda.util.XlsUtil;


import org.apache.commons.io.input.BOMInputStream;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.channels.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


import java.util.function.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.zip.*;


import java.sql.*;
import org.ramadda.util.sql.SqlUtil;


/**
 *
 * @author Jeff McWhirter
 */
@SuppressWarnings("unchecked")
public class Seesv implements SeesvCommands {

    private static final String PREFIX_FILE = "file:";

    /** _more_          */
    private static boolean debugFiles = false;

    /** _more_          */
    private static boolean debugArgs = false;

    private static File tmpCacheDir;


    private boolean interactive = false;
    
    /** _more_ */
    private List<String> args;

    /** _more_ */
    private OutputStream outputStream = System.out;

    /** _more_ */
    private InputStream inputStream;

    private ReadableByteChannel channel;
    

    private int sheetNumber =-1;
    
    private Hashtable<String,String> macros = new Hashtable<String,String>();
    
    /** _more_ */
    private File destDir = new File(".");

    /** _more_ */
    private TextReader myTextReader;

    private boolean multiFiles = false;
    private String multiFileTemplate = null;

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
    private List<String> changeTo = new ArrayList<String>();

    /** _more_ */
    private StringBuilder js = new StringBuilder();


    private Dater inDater = new Dater();    
    private Dater outDater = new Dater();

    private PropertyProvider propertyProvider;

    private MapProvider mapProvider;    

    private List<IO.Path> inputFiles;

    private boolean makeInputStreamRaw = false;

    private SeesvContext seesvContext;

    private List<ExtCommand> extCommands;

    private boolean hasSink = false;

    private boolean inputIsBom = false;
    private String encoding;

    private boolean commandLine = false;
    private boolean isVerifiedUser = false;    

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public Seesv(String[] args) throws Exception {
        this.args = new ArrayList<String>();
        for (String arg : args) {
            this.args.add(arg);
        }
        if (debugArgs) {
            System.err.println("Initial args");
            for (String arg : this.args) {
                System.err.println("Arg:" + arg);
            }
        }
	//	debugArgs = true;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public Seesv(List<String> args) throws Exception {
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
    public Seesv(List<String> args, File destDir) throws Exception {
        this(args);
        this.destDir = destDir;
    }


    public Seesv(String[]args,InputStream input, OutputStream output) throws Exception {
	this(args);
	this.inputStream = input;
        this.outputStream = output;
    }	

    public Seesv(List<String> args,InputStream input, OutputStream output) throws Exception {
        this.args = args;
	this.inputStream = input;
        this.outputStream = output;
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
    public Seesv(List<String> args, OutputStream out, File destDir)
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
    public Seesv(String[] args, OutputStream out, File destDir)
	throws Exception {
        this(args);
        this.destDir      = destDir;
        this.outputStream = out;
    }


    private TextReader initTextReader(TextReader textReader) {
	textReader.setInDater(inDater);
	textReader.setOutDater(outDater);	
	textReader.setEncoding(encoding);
	return textReader;
    }

    private TextReader makeTextReader() {
	TextReader textReader = new TextReader();
	return initTextReader(textReader);
    }


    /**
     * _more_
     *
     * @param seesv _more_
     */
    public void initWith(Seesv seesv) {
	this.inDater = seesv.inDater;
	this.outDater = seesv.outDater;	
        this.js      = seesv.js;
	this.extCommands = seesv.extCommands;
	this.macros.putAll(seesv.macros);
        //        this.delimiter = seesv.delimiter;
    }

    public List<IO.Path> getInputFiles() {
	return inputFiles;
    }


    /**
       Set the Context property.

       @param value The new value for Context
    **/
    public void setSeesvContext (SeesvContext value) {
	seesvContext = value;
	extCommands = new ArrayList<ExtCommand>();
	//	ExtCommand cmd = new TestCommand1();
	try {
	    for(Class c: seesvContext.getClasses()) {
		if (ExtCommand.class.isAssignableFrom(c)) {
		    ExtCommand command = (ExtCommand) c.getDeclaredConstructor().newInstance();
		    extCommands.add(command);
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
    public SeesvContext getSeesvContext () {
	return seesvContext;
    }

    public File getTmpFile(String name) {
	if(seesvContext!=null) return seesvContext.getTmpFile(name);
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
	    //	    System.err.println("Seesv.getProperty: no PropertyProvider set");
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

    public void setIsVerifiedUser(boolean v) {
	isVerifiedUser = v;
    }


    /**
     * _more_
     *
     * @param inputStream _more_
     */
    public Seesv setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
	return this;
    }

    public Seesv setChannel(ReadableByteChannel channel)  {
        this.channel = channel;
	return this;
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


    public Connection getDbConnection(TextReader ctx, SeesvOperator op, Dictionary<String,String> props, String db, String table) throws Exception {
	String dbs = getProperty("seesv_dbs");
	if(!Utils.stringDefined(db)) {
	    op.fatal(ctx, "No db specified." + (dbs!=null?"Available dbs:" + dbs:""));
	}
	String prefix = "seesv_db_" + db;
	String jdbcUrl = getProperty(prefix + "_url");
	Properties connectionProps = new Properties();
	if(jdbcUrl==null) {
	    op.fatal(ctx, "No " + prefix + "_url environment variable specified. " + (dbs!=null?"Available dbs:" + dbs:""));
	}

	String     user            = props.get("db.user");
	String     password        = props.get("db.password");
	if(user==null) user = getProperty(prefix +"_user");
	if(password==null) password = getProperty(prefix+"_password");	

	Connection connection = null;
	try {
	    connection = SqlUtil.getConnection(jdbcUrl, user, password);
	} catch (Exception exc) {
	    op.fatal(ctx, "Error making JDBC connection:"+ jdbcUrl, exc);
	}

	if (props.get("help")!=null && props.get("help").equals("true")) {
	    throw new SeesvException("table:" + table
				     + "\ncolumns:\n"
				     + Utils.wrap(SqlUtil.getColumnNames(connection,
									 table, true), "\t", "\n"));
	}


	//Check tables whitelist
	String tables   = getProperty(prefix + "_tables",  "");
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

    public static String makeCsvCommands(List<String> args) {
	StringBuilder argsBuff = null;
	int                doArgsCnt     = 0;
	int                doArgsIndex   = 1;
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
	    if(arg.equals(CMD_ARGS)) {
		continue;
	    }

	    if(arg.equals("-print")) {
		continue;
	    }
	    if(arg.equals("-maxrows")) {
		i++;
		continue;
	    }
	    
	    if (argsBuff == null) {
		argsBuff = new StringBuilder();
		argsBuff.append("csvcommands1=");
	    } else {
		doArgsCnt++;
		if (doArgsCnt > 4) {
		    argsBuff.append("\n");
		    doArgsCnt = 0;
		    doArgsIndex++;
		    argsBuff.append("csvcommands" + doArgsIndex + "=");
		} else {
		    argsBuff.append(",");
		}
	    }
	    arg = arg.replaceAll(",", "\\\\,").replace("\n"," ");
	    argsBuff.append(arg);
	}
	if(argsBuff!=null) return argsBuff.toString();
	return "";
    }


    /**
     * _more_
     *
     * @param files _more_
     *
     * @throws Exception _more_
     */
    public void run(List<IO.Path> files) throws Exception {
        try {
            runInner(files);
        } catch (Exception exc) {
            SeesvOperator op = (myTextReader == null)
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
    private void runInner(List<IO.Path> files) throws Exception {

        if (files == null) {
            files = new ArrayList<IO.Path>();
        }
	this.inputFiles = files;
        boolean      doConcat      = false;
        boolean      doAppend      = false;
        boolean      doLast        = false;
        int          lastLines     = 0;			
	int appendSkip = 1;
        boolean      doHeader      = false;
        boolean      doRaw         = false;
        int          rawCut        = 0;
        Hashtable    dbProps       = new Hashtable<String, String>();
        boolean      doPoint       = false;
        String       iterateColumn = null;
        List<String> iterateValues = new ArrayList<String>();

        String       prepend       = null;
        myTextReader = initTextReader(new TextReader(destDir, outputFile, outputStream));

        boolean      printArgs = false;
        List<String> extra     = new ArrayList<String>();
	boolean doArgs = false;
	int                doArgsCnt     = 0;
	int                doArgsIndex   = 1;
	StringBuilder argsBuff = null;
	boolean doTypeXml = false;
	String typeName = "";
	String typeDesc = "";
	String typeColumns = "";
	String typeWiki = "";		
	PrintWriter pw        = null;
	//	System.err.println("args:" + args);
	//Check for the -args command
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
	    if(arg.equals(CMD_ARGS)) {
		doArgs = true;
		continue;
	    }
	    if(arg.equals(CMD_TYPE_XML)) {
		doArgs = true;
		doTypeXml = true;
		continue;
	    }	    
	}


	//	System.err.println("args:" + args);
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
	    if(arg.equals(CMD_ARGS)) {
		doArgs = true;
		continue;
	    }
	    if(arg.equals(CMD_TYPE_XML)) {
		doArgs = true;
		doTypeXml = true;
		typeName = (args.get(++i));
		typeDesc = (args.get(++i));
		typeColumns = (args.get(++i));
		//		typeWiki = (args.get(++i));    						
		continue;
	    }	    
	    if (doArgs) {
		if(doTypeXml && arg.equals("-maxrows")) {
		    i++;
		    continue;
		}
		//Don't include maxrows or print commands
		if(arg.equals("-print")) {
		    continue;
		}
		if(arg.equals("-maxrows")) {
		    i++;
		    continue;
		}

		if (argsBuff == null) {
		    argsBuff = new StringBuilder();
		    argsBuff.append("csvcommands1=");
		} else {
		    doArgsCnt++;
		    if (doArgsCnt > 4) {
			argsBuff.append("\n");
			doArgsCnt = 0;
			doArgsIndex++;
			argsBuff.append("csvcommands" + doArgsIndex + "=");
		    } else {
			argsBuff.append(",");
		    }
		}
		arg = arg.replaceAll(",", "\\\\,").replace("\n"," ");
		//		System.err.println("arg:" + arg);
		argsBuff.append(arg);
		continue;
	    }


            //      System.out.println("ARG:" + arg);
            //      if(true) continue;
            if (arg.equals("-printargs")) {
                System.out.print("java  org.ramadda.util.seesv.Seesv ");
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
            if (arg.equals("-genhelp")) {
                genHelp();
                return;
            }
            if (arg.equals(CMD_VERSION)) {
		printVersion();
		return;
	    }

            if (arg.equals(CMD_HELP)) {
                usage("", !interactive,null);
                return;
            }
            if (arg.equals("-helpraw")) {
                usage("", false,null, CMD_RAW, "true");
                return;
            }
            if (arg.equals("-helpjson")) {
                usage("", false,null, CMD_JSON, "true");
                return;
            }
            if (arg.startsWith("-help:")) {
                usage("", true,arg.substring("-help:".length()));
                return;
            }
            if (arg.startsWith("-helpraw:")) {
                usage("", false,arg.substring("-helpraw:".length()));
                return;
            }
            if (arg.equals("-helppretty")) {
                usage("", true,null);
                return;
            }
            if (arg.equals(CMD_HELP)) {
                usage("", false,null);
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


            if (arg.equals(CMD_CAT)) {
                doConcat = true;
                continue;
            }


            if (arg.equals(CMD_MULTIFILES)) {
		multiFiles = true;
		multiFileTemplate = args.get(++i);
		continue;
	    }

            if (arg.equals(CMD_APPEND)) {
                doAppend = true;
		appendSkip = parseInt(args.get(++i));
                continue;
            }

            if (arg.equals(CMD_CHOP)) {
                doLast = true;
		lastLines = parseInt(args.get(++i));
                continue;
            }	    	    

            if (arg.equals(CMD_RAW)) {
                doRaw = true;
                continue;
            }

            if (arg.equalsIgnoreCase("-commentchar")) {
                myTextReader.setCommentChar(args.get(++i));
                continue;
            }

            if (arg.equals(CMD_HEADER)) {
                myTextReader.setFirstRow(new Row(Utils.split(args.get(++i), ",")));
                continue;
            }

            if (arg.startsWith("-iter")) {
                iterateColumn = args.get(++i);
                iterateValues = Utils.split(args.get(++i), ",");
                continue;
            }
            extra.add(arg);
	}


	if(doTypeXml) {
	    if (argsBuff != null) {
		argsBuff.append("\n");
	    }
	    pw   = new PrintWriter(getOutputStream());	
	    pw.println("<types>");
	    pw.println("<!-- This has automatically been generated by SeeSV");
	    pw.println("it contains the SeeSV commands for converting the file, a set of other");
	    pw.println("properties and a wiki text section that you can modify to define how you want this entry type to be displayed");	    
	    pw.println("copy this file into your RAMADDA home/plugins directory and restart your RAMADDA");
	    pw.println("if you want to have RAMADDA automatically use this entry  type for particular files");
	    pw.println("set the pattern= below to a regular expression that matches on the file name");	    
	    pw.println("-->");

	    pw.println("<type name=\"" + typeName +"\" \ndescription=\"" + typeDesc +"\" \nsuper=\"type_point\" \ncategory=\"Point Data\"\nsupercategory=\"Geoscience\"  \npattern=\"some pattern\"\nhandler=\"org.ramadda.data.services.PointTypeHandler\">"); 	    
	    pw.println("<property name=\"record.file.class\" value=\"org.ramadda.data.point.text.CsvFile\"/>");
	    for(String line:Utils.split(typeColumns,";",true,true)) {
		if(line.startsWith("#")) continue;
		List<String> toks = Utils.split(line,",");
		String name = toks.get(0);
		String desc = toks.get(1);		
		String type=toks.get(2);		
		StringBuilder cb=new StringBuilder("<column ");
		cb.append(HtmlUtils.attrs("name",name,"label",desc,"type",type));
		boolean seenSearch=false;
		for(int i=3;i<toks.size();i+=2) {
		    String n=toks.get(i);
		    String v=toks.get(i+1).replace("\\,",",");		    
		    if(n.equals("cansearch"))seenSearch=true;
		    cb.append(HtmlUtils.attrs(n,v));
		}
		if(!seenSearch)
		    cb.append(HtmlUtils.attrs("cansearch","true"));		    
		cb.append("/>");
		pw.println(cb);
	    }		

	    pw.println("<property name=\"icon\" value=\"/icons/file.png\"/>");
	    pw.println("<property name=\"form.date.show\" value=\"false\"/>");
	    pw.println("<property name=\"form.area.show\" value=\"false\"/>");
	    pw.println("<property name=\"form.location.show\" value=\"true\"/>");
	    pw.println("<property name=\"form.properties.show\" value=\"false\"/>");
	    pw.println("<property name=\"record.properties\"><![CDATA[\n");
	    if(argsBuff!=null)
		pw.print(argsBuff.toString().replaceAll(" ","_csvcommandspace_"));
	    pw.println("]]></property>");

	    if(!Utils.stringDefined(typeWiki)) {
		typeWiki ="+section  title=\"{{name}}\"\n:rem Edit this wiki text\n{{description wikify=true}}\n{{group}}\n{{display_download}}\n{{display_htmltable}}\n:heading Information\n{{information details=true}}\n-section";
	    }
	    pw.println("<wiki>\n<![CDATA[\n" + typeWiki.replace("\\n","\n")+"\n]]></wiki>\n");
	    pw.println("</type>");
	    pw.println("</types>");	    	    
	    pw.close();
	    return;
	}


	if (doArgs) {
	    if (argsBuff != null) {
		argsBuff.append("\n");
	    }
	    pw   = new PrintWriter(getOutputStream());
	    pw.print(argsBuff);
	    pw.close();
	    return;
	}

        if ( !parseArgs(extra, myTextReader, files)) {
            currentArg = null;
            return;
        }


        currentArg = null;
        if (printArgs) {
            for (IO.Path f : files) {
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
	} else if (doLast) {
            lastLines(files.get(0), getOutputStream(),lastLines);	    	    
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
            for (int i = 0; i < iterateValues.size(); i++) {
                String pattern = iterateValues.get(i);
                if (iteratePattern != null) {
                    iteratePattern.setPattern(pattern);
                }
		int providerCnt = 0;
                for (DataProvider provider : providers) {
		    providerCnt++;
		    int cnt=0;
		    for (NamedInputStream input : getStreams(files)) {
			//			System.err.println("file:" + input);
			int fileCnt = cnt++;
			if(multiFiles) {
			    File newFile;
			    if(Utils.stringDefined(multiFileTemplate)) {
				String source  = new File(input.getName()).getName();
				String name  = IOUtil.stripExtension(source);
				newFile = new File(multiFileTemplate.replace("${file_name}",source).replace("${count}",""+(cnt)).replace("${file_shortname}",name));
			    } else {
				newFile = new File(input.getName()+".csv");
			    }
			    checkOkToWrite(newFile.toString());
			    myTextReader.setOutputFile(newFile);
			    //			    System.err.println("new file:" + newFile);
			}
			myTextReader.resetProcessors(multiFiles);
			myTextReader.setInput(input);
			process(myTextReader, provider,multiFiles?0:fileCnt);
			if(!multiFiles) {
			    myTextReader.setFirstRow(null);
			}
			input.close();
			if(multiFiles) {
			    myTextReader.finishProcessing();
			}
		    }
		    if (okToRun && !multiFiles) {
			myTextReader.finishProcessing();
		    }
		    provider.finish();
		    myTextReader.flush();
		    myTextReader.close();
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
            new DataProvider.CsvDataProvider(ctx,0);
        process(ctx, provider,0);
	ctx.flush();
	ctx.close();
    }


    /**
     * Run through the csv file in the TextReader
     *
     * @param ctx Holds input, output, skip, delimiter, etc
     * @param provider _more_
     *
     * @throws Exception On badness
     */
    public void process(TextReader ctx, DataProvider provider,int fileCnt)
	throws Exception {
	provider.initialize(this, ctx);
        try {
            errorDescription = null;
            processInner(ctx, provider,fileCnt);
	    ctx.flush();
        } catch (Exception exc) {
            SeesvOperator op = (ctx == null)
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
    private void processInner(TextReader ctx, DataProvider provider,int fileCnt)
	throws Exception {
        int rowCnt   = 0;
        Row firstRow = ctx.getFirstRow();
        ctx.setFirstRow(null);
        if (firstRow != null) {
	    if(fileCnt==0) {
		processRow(ctx, firstRow);
		rowCnt++;
		provider.incrRowCnt();
	    }
        }
	long t1 = System.currentTimeMillis();
        Row row;
	double mem1=Utils.getUsedMemory();
        while ((row = provider.readRow()) != null) {
	    if(row==null) break;
	    if(rowCnt++==0 && fileCnt>0) {
		continue;
	    }
            if (rowCnt <= ctx.getSkipRows()) {
                continue;
            }

	    if ( !processRow(ctx, row)) {
		break;
	    }
        }
	long t2 = System.currentTimeMillis();
	//	System.err.println("time:" + (t2-t1));
	//        if (okToRun) {
	//            ctx.finishProcessing();
	//        }
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
    private List<NamedInputStream> getStreams(List<IO.Path> files)
	throws Exception {
        if (debugFiles) {
            System.err.println("getStreams:" + files);
        }
        ArrayList<NamedInputStream> streams =
            new ArrayList<NamedInputStream>();
        for (IO.Path file : files) {
            streams.add(new NamedInputStream(file.getPath(), wrapInputStream(makeInputStream(file))));
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
		    InputStream bais=  XlsUtil.xlsToCsv(new IO.Path(file),-1,sheetNumber);
		    ReadableByteChannel in = Channels.newChannel(bais);
		    channels.add(new NamedChannel(file, in));
		    continue;
		}
		if (file.toLowerCase().endsWith(".xlsx")) {
		    InputStream bais=  XlsUtil.xlsxToCsv(new IO.Path(file),-1,sheetNumber);
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



    public static void setTmpCacheDir(File dir) {
	tmpCacheDir = dir;
    }

    public static File getTmpCacheDir() {
	return tmpCacheDir;
    }    

    /*
      Throw an error if we're not allows to write the file
    */
    public static void checkOkToWrite(String file) throws Exception {
	if(!IO.okToWriteTo(file)) 
	    throw new IllegalArgumentException("Cannot write file:"   + file);
    }


    /*
      Throw an error if we're not allows to read the file
    */
    public static void checkOkToRead(String file) {
	if(!IO.okToReadFrom(file)) 
	    throw new IllegalArgumentException("Cannot read file:"   + file);
    }


    public String readFile(String file) throws Exception {
	if (file.startsWith(PREFIX_FILE)) {
	    file = file.substring(PREFIX_FILE.length());
	}
	checkOkToRead(file);
	return  IO.readContents(new File(file));
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
    public InputStream makeInputStream(IO.Path file) throws Exception {
	if(!Utils.isUrl(file.getPath())) {
	    checkOkToRead(file.getPath());
        }
        if (file.matchesSuffix(".xls")) {
            return  XlsUtil.xlsToCsv(file,myTextReader.getMaxRows(),sheetNumber);
	} else if (file.matchesSuffix(".xlsx")) {
            return  XlsUtil.xlsxToCsv(file,myTextReader.getMaxRows(),sheetNumber);
	} else if (file.matchesSuffix(".gz",".gzip")) {
	    return new BufferedInputStream(new GZIPInputStream(new FileInputStream(file.getPath())));
        } else if (!makeInputStreamRaw && file.matchesSuffix(".zip")) {
            InputStream    fis = IO.getInputStream(file.getPath());
            ZipInputStream zin = new ZipInputStream(fis);
            ZipEntry       ze  = null;
            while ((ze = zin.getNextEntry()) != null) {
		if (ze.isDirectory()) {
                    continue;
                }
                String p = ze.getName().toLowerCase();
                if (p.endsWith(".csv") || p.endsWith(".tsv")||p.endsWith(".txt") || p.endsWith(".json")||p.endsWith(".geojson")) {
                    return zin;
                }
                //Apple health
                if (p.endsWith("export.xml")) {
                    return zin;
                }
            }
	    throw new IllegalArgumentException("Could not find .csv, .tsv or .txt file in the zip file");

        } else {
            if (file.isFile()) {
                try {
		    FileInputStream fis = new FileInputStream(file.getPath());
		    //		    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
		    //		    BufferedReader reader = new BufferedReader(isr)
                    return new BufferedInputStream(fis);
                } catch (Exception exc) {
                    System.err.println("Error opening file:" + file);
                    throw exc;
                }
            }
            return IO.doMakeInputStream(file,true);
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
    public void header(List<IO.Path> files, TextReader ctx, boolean asPoint)
	throws Exception {
        PrintWriter   writer    = ctx.getWriter();
        List<Integer> widths    = ctx.getWidths();
        String        delimiter = ctx.getDelimiter();
        if ((widths == null) && (delimiter == null)) {
            delimiter = ",";
        }
        List<BufferedReader> readers = new ArrayList<BufferedReader>();
        for (IO.Path file : files) {
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
        writer.flush();
        writer.close();
    }

    public void lastLines(IO.Path file, OutputStream os, int lines)
	throws Exception {
	BufferedReader br = new BufferedReader(new InputStreamReader(makeInputStream(file)));
	TextReader textReader = new TextReader(br);
	//??	textReader.setCleanInput(true);
	int numLines = textReader.countLines();
	br.close();
	br = new BufferedReader(new InputStreamReader(makeInputStream(file)));
	textReader = new TextReader(br);
	int linesToSkip = numLines-lines;
	//??	textReader.setCleanInput(true);
	PrintWriter pw = new PrintWriter(os);
	String line = textReader.readLine();
	pw.println(line);
	//	System.err.println("last lines:" + lines +" total lines:" + numLines +" toSkip:" + linesToSkip);
	while(--linesToSkip>0 && (line=textReader.readLine())!=null ) {
	}
	while((line=textReader.readLine())!=null) {
	    pw.println(line);
	}
	
	pw.flush();
	pw.close();
    }


    /**
     *     _more_
     *
     *     @param files _more_
     *     @param ctx _more_
     *
     *     @throws Exception _more_
     */
    public void raw(List<IO.Path> files, TextReader ctx) throws Exception {
        int         numLines    = ctx.getMaxRows();
        PrintWriter writer      = ctx.getWriter();
        String      prepend     = ctx.getPrepend();
        int         chars       = 0;
        int         LINE_LIMIT  = 2000;
        int         CHARS_LIMIT = 3000000;
        for (IO.Path file : files) {
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
    public static String getDbProp(Dictionary<String, String> props,
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
    public static String getDbProp(Dictionary<String, String> props,
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
    public static boolean getDbProp(Dictionary<String, String> props,
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
                desc = HELP_COLUMNS;
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

        public Cmd() {
	}

        /**
         * _more_
         *
         * @param cmd _more_
         * @param args _more_
         * @param desc _more_
         */
        public Cmd(String cmd, String desc, Object... args) {
	    //	    String _cmd = "CMD_" +cmd.toUpperCase().replace("-","");
	    //	    String tcl  ="tclsh ~/bin/cvrt.tcl ";
	    //	    String QT = "\\\"";
	    //	    if(cmd.indexOf("<")<0)System.out.println(tcl +  QT+cmd+QT +" " + _cmd +" Seesv.java");


            this.cmd  = cmd;
            this.desc = desc;
            for (int i=0;i<args.length;i++) {
		Object obj  = args[i];
                if ( !(obj instanceof Arg)) {
		    String sobj = obj.toString();
		    if(sobj.equals(ARG_LABEL)) {
			this.label = args[++i].toString();
			continue;
		    }
                    obj = new Arg(sobj, "");
                }
                this.args.add((Arg) obj);
            }
	    if(label==null) {
		label = Utils.makeLabel(cmd.replaceAll("-"," ").trim());
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
        public String getLine(boolean decorate,boolean format,boolean json) {
	    String prefix1 = (decorate?Utils.ANSI_BLUE:"");
	    String prefix2 = (decorate?Utils.ANSI_GREEN:"");
	    String prefix3 = (decorate?Utils.ANSI_CYAN:"");	    	    
	    String suffix = (decorate?Utils.ANSI_RESET:"");
            StringBuilder sb = new StringBuilder();
            if (args != null) {
                for (Arg arg : args) {
		    if(format) sb.append("\n\t");
		    String desc = arg.desc;
		    if(json && desc.length() > 0) desc = arg.desc.replace("\n","\\n");
		    if(!json)
			desc = desc.replace("<add>","").replace("</add>","").replace("&lt;","<").replace("&gt;",">");
                    sb.append("<" + prefix2+arg.id +" " + suffix.replace("\n","\\n")+ desc);
		    if (arg.props != null) {
			for (int i = 0; i < arg.props.length-1;   i+=2) {
			    if(arg.props[i].equals(ATTR_TYPE)) continue;
			    if(!json && arg.props[i].equals(ATTR_ROWS)) continue;
			    sb.append(" " + arg.props[i]+"="+arg.props[i+1]);
			}
		    }
		    sb.append("> ");
                }
            }

	    String d = Utils.stringDefined(desc)?(decorate?" ":" (") + prefix3+desc+suffix + (decorate?"":")"):"";
	    String s =  prefix1+cmd+suffix+" ";
	    if(format) {
		s = s+d+sb;
	    } else {
		s = s+sb+d;
	    }
	    return s;
        }
    }

    public static class Category extends Cmd {

        /**
         * _more_
         *
         * @param isCat _more_
         * @param category _more_
         */
        public Category(String category,String desc) {
            this.category = true;
	    this.cmd = category;
            this.desc     = desc;
        }


        public Category(String category) {
	    this( category,"");
	}
    }


    private static String add(String ...values) {
	StringBuilder sb = new StringBuilder();
	for(String s:values) {
	    sb.append("<add>"+s+"</add>  ");
	}
	return sb.toString();
    }

    private static String argnl(String prefix,String ...values) {
	StringBuilder sb = new StringBuilder();
	sb.append(prefix);
	for(String s:values) {
	    if(sb.length()>0) {
		sb.append("\n");
		sb.append(prefix);
	    }
	    sb.append(s);
	}
	return sb.toString();
    }    


    /** _more_ */
    private static final Cmd[] commands = {
        new Cmd(CMD_HELP, "print this help"),
        new Cmd(CMD_HELP+":<topic search string>",
                "print help that matches topic"),
        new Cmd(CMD_HELP_PRETTY, "pretty print help"),
        new Cmd(CMD_VERSION, "print version"),
        new Cmd(CMD_COMMANDS, "file of commands",
		new Arg("file", "The file of commands. Any # of lines",
			ATTR_TYPE, "file")),			

        /** * Input   * */
        new Category("Input","Specify the input. Default is assumed to be a CSV but can support HTML, JSON, XML, Shapefile, etc."),
        new Cmd(CMD_DELIMITER, "Specify the input delimiter",
                new Arg("delimiter", "Use 'space' for space, 'tab' for tab,'?' to guess between tab and space",  ATTR_SIZE, "5")),
	new Cmd(CMD_INPUTCOMMENT,"Input comment",
		new Arg("comment")),
        new Cmd(CMD_TAB, "Use tabs. A shortcut for -delimiter tab"),
        new Cmd(CMD_WIDTHS, "Columns are fixed widths",
		new Arg("widths", "w1,w2,...,wN")),
        new Cmd(CMD_QUOTESNOTSPECIAL, "Don't treat quotes as special characters",
		ARG_LABEL,"Quotes Not Special"),
        new Cmd(CMD_CLEANINPUT, "Input is one text line per row. i.e., no new lines in a data row. Setting this can improve performance on large files",
		ARG_LABEL,"Input is Clean"),


        new Cmd(CMD_START, "Start at pattern in source file",
                new Arg("start pattern", "", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_STOP, "End at pattern in source file",
                new Arg("stop pattern", "", ATTR_TYPE, TYPE_PATTERN)),

        new Cmd(CMD_BOM, "Input has a leading byte order mark (BOM) that should be stripped out",ARG_LABEL,"Strip BOM"),
        new Cmd(CMD_ENCODING,
		"Specify the file encoding",ARG_LABEL,"File Encoding",
		new Arg("encoding","File Encoding see https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html",
			"type","enumeration","values","UTF-8,UTF-16,UTF-16BE,UTF-16LE,UTF-32,UTF-32BE,UTF-32LE,CESU-8,IBM00858,IBM437,IBM775,IBM850,IBM852,IBM855,IBM857,IBM862,IBM866,ISO-8859-1,ISO-8859-13,ISO-8859-15,ISO-8859-2,ISO-8859-4,ISO-8859-5,ISO-8859-7,ISO-8859-9,KOI8-R,KOI8-U,Not available,US-ASCII,windows-1250,windows-1251,windows-1252,windows-1253,windows-1254,windows-1257,x-IBM737,x-IBM874,x-UTF-16LE-BOM,x-UTF-32BE-BOM,x-UTF-32LE-BOM")),			
        new Cmd(CMD_HEADER, "Raw header",ARG_LABEL,"Add Header",
		new Arg("header", "Column names", ATTR_TYPE, TYPE_LIST)),

        new Cmd(CMD_MULTIFILES, "Treat input files separately",ARG_LABEL,"Multi-files",
		new Arg("template", "File template  - ${file_shortname} ${file_name} ${count}")),

        new Cmd(CMD_JSON, "Parse the input as json",
		ARG_LABEL,"Read JSON",
                new Arg("arrayPath",
			"Path to the array e.g., obj1.arr[2].obj2",
			"label", "Array path",
			ATTR_SIZE, "30"),
		new Arg("objectPaths",
			"One or more paths to the objects e.g. geometry,features",
			"label", "Object paths",
			ATTR_TYPE, TYPE_LIST, ATTR_SIZE, "30")),
        new Cmd(CMD_JSONJOIN, "Join different arrays in the input JSON",
		ARG_LABEL,"Join JSON",
                new Arg("arrayPaths",
			"comma separated list of the array paths",
			"label", "Array paths",
			ATTR_SIZE, "30"),
		new Arg("keys",
			"Comma separated list of keys to match on",
			"label", "Keys"),
		new Arg("pattern",
			"Optional pattern to replace the key value with",
			"label", "Pattern"),
		new Arg("replace","Pattern replace"),
		new Arg("missing","Missing value")),
        new Cmd(CMD_JSONVALUE, "Extract a value from a JSON column",
		ARG_LABEL,"JSON Value",
                new Arg(ARG_COLUMNS, "Column names", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("arrayPath",
			"Path to the array e.g., obj1.arr[2].obj2",
			"label", "Array path",
			ATTR_SIZE, "30")),
        new Cmd(CMD_GEOJSON, "Parse the input as geojson",
		ARG_LABEL,"Read GeoJSON",
		new Arg("includePolygon","Include polygon",
			ATTR_TYPE,"enumeration","values","true,false")),
        new Cmd(CMD_PDF, "Read input from a PDF file.",ARG_LABEL,"Read PDF"),	
        new Cmd(CMD_XML, "Parse the input as xml",
		ARG_LABEL,"Read XML",
                new Arg("path", "Path to the elements", ATTR_SIZE, "60")),
        new Cmd(CMD_SHAPEFILE, "Parse the input shapefile",
		ARG_LABEL,"Read Shapefile",
                new Arg("props", "\"addPoints true addShapes false\"")),	
        new Cmd(CMD_LINES, "Parse the input as text lines. Treat each line as one column",
		ARG_LABEL,"Read raw lines"),
        new Cmd(CMD_HTMLTABLE, "Parse tables in the input html file",
		ARG_LABEL,"Read HTML Table",
                new Arg("skip", "Number of tables to skip", ATTR_TYPE,
			TYPE_NUMBER),
		new Arg(ARG_PATTERN, "Pattern to skip to",
			ATTR_TYPE, TYPE_PATTERN, ATTR_SIZE,
			"40"),
		new Arg("properties",
			"Other name value args - <ul><li> numTables N:Number of tables to process. Default is 1<li> removeEntity true:remove HTML entities <li> removePattern pattern<li> extractUrls true <li> columnN.extractUrls true: N=column number<li> stripTags false: strip any HTML tags. Default =true<li> columnN.stripTags false: N=column number. Set stripTags for the column</ul>",
			ATTR_ROWS, "6", ATTR_SIZE, "40")),
        new Cmd(CMD_HTMLPATTERN, "Parse the input html file",
		ARG_LABEL,"Read HTML Pattern",
                new Arg(ARG_COLUMNS, "Column names", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("startPattern", "", ATTR_TYPE, TYPE_PATTERN),
                new Arg("endPattern", "", ATTR_TYPE, TYPE_PATTERN),
                new Arg(ARG_PATTERN, "Row pattern. Use (...) to match columns",
                        ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_HARVEST, "Harvest links in web page. This results in a 2 column dataset with fields: label,url",
		new Arg(ARG_PATTERN,"regexp to match")),	
	/*
        new Cmd(CMD_SCRAPE, "For each URL in a column scrape the web page",
                new Arg(ARG_COLUMN, "Column name", ATTR_TYPE, TYPE_COLUMN),
		new Arg("column_names","comma separated new column names",ATTR_TYPE,TYPE_LIST),
		new Arg("pattern", "pattern - use (..) to extract values", ATTR_TYPE, TYPE_PATTERN)),
	*/
        new Cmd(CMD_TEXT, "Extract rows from the text",
		ARG_LABEL,"Read text",
                new Arg("comma separated header"),
                new Arg("chunk pattern", "", ATTR_TYPE, TYPE_PATTERN),
                new Arg("token pattern", "", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_EXTRACTPATTERN, "Extract rows from the text",
		ARG_LABEL,"Extract Pattern",		
                new Arg("comma separated header"),
                new Arg("token pattern", "", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_TOKENIZE, "Tokenize the input from the pattern",
                new Arg("header", "header1,header2..."),
                new Arg(ARG_PATTERN, "", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_SQL, "Read data from the given database",
		ARG_LABEL,"Read from DB",
                new Arg("db", "The database id (defined in the environment)",
			ATTR_TYPE,"enumeration","values","property:seesv_dbs"),
		new Arg("table", "Comma separate list of tables to select from",ATTR_SIZE,"60"),
		new Arg(ARG_COLUMNS, "Comma separated list of columns to select"),
		new Arg("where", "column1:expr:value;column2:expr:value;...\ne.g.: name:like:joe;age:>:60\nWhere expr is: =|<|>|<>|like|notlike",ATTR_TYPE,TYPE_ROWS,"delimiter",";",ATTR_SIZE,"60"),				
		new Arg("properties", "name space value properties. e.g., join col1,col2")),
        new Cmd(CMD_SYNTHETIC, "Generate an empty file with the given number of rows",
                new Arg("header","comma separated header"),
                new Arg("values","comma separated values"),		
                new Arg("number_rows","Number of rows",ATTR_TYPE,TYPE_NUMBER)),

        new Cmd(CMD_PRUNE, "Prune out the first N bytes",
                new Arg("bytes", "Number of leading bytes to remove", ATTR_TYPE,
                        TYPE_NUMBER)),
        new Cmd(CMD_NOHEADER, "Strip off the header"),
        new Cmd(CMD_DEHEADER, "Strip off the RAMADDA point header"),
        new Cmd(CMD_HEADERNAMES, "Make the header proper capitalization",
		ARG_LABEL,"Header Names"),
        new Cmd(CMD_HEADERIDS, "Clean up the header names",ARG_LABEL,"Header IDs"),	
        new Cmd(CMD_IDS, "Use canonical names"),
        new Cmd(CMD_SHEET, "Set XLS sheet #",
                new Arg("sheet", "Sheet number", ATTR_TYPE,
                        TYPE_NUMBER)),

        new Cmd(CMD_CAT, "Concat the columns in one or more csv files", "*.csv"),
        new Cmd(CMD_APPEND, "Append the files, skipping the given rows in the latter files",
		new Arg("skip","Number of rows to skip"),
		new Arg("files","*.csv")),
        new Cmd(CMD_CHOP, "Write out last N lines. include the header",
		new Arg("numlines","Number of lines to leave"),
		new Arg("file","*.csv")),
        new Cmd(CMD_FILENAMEPATTERN, "Extract strings from the file name and add them as new columns",
		ARG_LABEL,"Filename Pattern",
                new Arg("pattern", "Pattern to match", ATTR_TYPE, TYPE_PATTERN),
		new Arg("columnnames","Comma separated list of column names")),
        /** *  Filter * */
        new Category("Filter"),
        new Cmd(CMD_SKIPLINES, "Skip number of raw lines.",
		ARG_LABEL,"Skip Lines",
                new Arg("lines", "How many raw lines to skip", ATTR_TYPE, TYPE_NUMBER)),	
        new Cmd(CMD_MAXROWS, "Set max rows to process",
		ARG_LABEL,"Max Rows",		
		new Arg("rows","Number of rows",ATTR_TYPE, TYPE_NUMBER)),
        new Cmd(CMD_MATCH, "Pass through rows that the columns each match the pattern",
		ARG_LABEL,"Match",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg(ARG_PATTERN, "regexp or prefix with includes:s1,s2 to do substrings match", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_NOTMATCH,
                "Pass through rows that don't match the pattern",
		ARG_LABEL,"Not Match",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg(ARG_PATTERN, "regexp or prefix with includes:s1,s2 to do substrings match", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_IF, "Next N args specify a filter command followed by any change commands followed by an -endif.",ARG_LABEL,"If"),
        new Cmd(CMD_RAWLINES, "",
		ARG_LABEL,"Print raw lines",
                new Arg("lines",
                        "Pass through and print out rawlines unprocesed")),
        new Cmd(CMD_INPUTNOTCONTAINS, "Filter out input lines that contain any of the strings",
		ARG_LABEL,"Filter input lines",
                new Arg("filters",
                        "Comma separated list of strings to filter on")),
        new Cmd(CMD_MIN,
		"Only pass thorough lines that have at least this number of columns. Specify blank to use the number of columns in the header",
		new Arg("min # columns", "", ATTR_TYPE, TYPE_NUMBER)),
        new Cmd(CMD_MAX,
		"Only pass through lines that have no more than this number of columns. Specify blank to use the number of columns in the header",
		new Arg("max # columns", "", ATTR_TYPE, TYPE_NUMBER)),
        new Cmd(CMD_NUMCOLUMNS,
		"Remove or add values so each row has the number of columns",
		ARG_LABEL,"Ensure # of columns",
		new Arg("number", "use -1 to use the # of columns in the header",
			ATTR_TYPE, TYPE_NUMBER)),
        new Cmd(CMD_HAS, "Only pass through anything if the data has the given columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_IFNUMCOLUMNS, "Only pass through rows with number of columns passing the operator",
                new Arg("operator", "<,<=,>,>=,=,!="),	
                new Arg("number", "Number of columns")),	
        new Cmd(CMD_FUZZYPATTERN, "Pass through rows that the columns each fuzzily match the pattern",
		ARG_LABEL,"Fuzzy match",
                new Arg("threshold", "Score threshold 0-100. Default:85. Higher number better match"),
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg(ARG_PATTERN, "", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_LENGTHGREATER, "Pass through rows that the length of the columns is greater than",
		ARG_LABEL,"Column length &gt;",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("length", "")),

	new Cmd(CMD_SAME, "Pass through where the 2 columns have the same value",
                new Arg("column1", "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("column2", "", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_NOTSAME, "Pass through where the 2 columns don't have the same value",
		ARG_LABEL,"Columns Not Same",
                new Arg("column1", "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("column2", "", ATTR_TYPE, TYPE_COLUMN)),			
        new Cmd(CMD_UNIQUE, "Pass through unique values",
		new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("mode","What type of matching is done - exact (exact match) or clean (lower case and remove whitespace) or fuzzy:threshold (do fuzzy matching with threshold from 1: no similarity to 100: exact match. use fuzzy:? to print out values)",ATTR_TYPE,"enumeration","values","exact,clean,fuzzy:threshold")),
        new Cmd(CMD_DUPS, "Pass through duplicate values",
		new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS)),
        new Cmd(CMD_SAMPLE, "Pass through rows based on probablity",
                new Arg("probablity", "0-1 probability of passing through a row")),

        new Cmd(CMD_MINVALUE,  "Pass through the row that has the min value in the group of columns defined by the key column",
		ARG_LABEL,"Min Value",
		new Arg("key column","",ATTR_TYPE,TYPE_COLUMN),
                new Arg("value column","",ATTR_TYPE,TYPE_COLUMN)),
        new Cmd(CMD_MAXVALUE,  "Pass through the row that has the max value in the group of columns defined by the key column",
		ARG_LABEL,"Max Value",
		new Arg("key column","",ATTR_TYPE,TYPE_COLUMN),
                new Arg("value column","",ATTR_TYPE,TYPE_COLUMN)),
        new Cmd(CMD_EQ, "Pass through rows that the column value equals the given value",
		ARG_LABEL,"Value Equals",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_NE, "Pass through rows that the column value does not equal the given value",
		ARG_LABEL,"Value Not Equals",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_GT, "Pass through rows that the column value is greater than the given value",
		ARG_LABEL,"Value &gt;",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_GE, "Pass through rows that the column value is greater than or equals the given value",
		ARG_LABEL,"Value &gt;=",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_LT, "Pass through rows that the column value is less than the given value",
		ARG_LABEL,"Value &lt;",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_LE, "Pass through rows that the column value is less than or equals the given value",
		ARG_LABEL,"Value &lt;=",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_BETWEEN, "Extract rows that are within the range",
		ARG_LABEL,"Value Between",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("min value"),
		new Arg("max value")),
	new Cmd(CMD_NOTBETWEEN,"Extract rows that are not within the range",
		ARG_LABEL,"Not Between",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("min value"),
		new Arg("max value")),
        new Cmd(CMD_BETWEENSTRING, "Extract rows that are between the given strings",
		ARG_LABEL,"Between String",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("start string"),new Arg("end string")),
        new Cmd(CMD_NOTBETWEENSTRING, "Extract rows that are between the given strings",
		ARG_LABEL,"Not Between String",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("start string"),
		new Arg("end string")),
        new Cmd(CMD_GROUPFILTER, "One row in each group has to match",
		ARG_LABEL,"Group Filter",
                new Arg(ARG_COLUMN, "key column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("value_column", "Value column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("operator", "", "values", "=,!=,~,<,<=,>,>="),
                new Arg("value")),
        new Cmd(CMD_BEFORE, "Pass through rows whose date is before the given date",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("date")),
        new Cmd(CMD_AFTER, "Pass through rows whose date is after the given date",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN), 
                new Arg("date")),
	new Cmd(CMD_COUNTVALUE, "No more than count unique values",
		ARG_LABEL,"#Unique Values",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("count")),
        new Cmd(CMD_DECIMATE, "only include every <skip factor> row",
                new Arg("rows", "# of start rows to include"),
                new Arg("skip", "skip factor")),

        new Cmd(CMD_IFIN, "Pass through rows that the columns with ID is in given file",
		ARG_LABEL,"Is in File",
                new Arg(ARG_COLUMN, "Column in the file", ATTR_TYPE, TYPE_COLUMN),
                new Arg("file", "The file"),
                new Arg("column2", "Column in main file", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_IFNOTIN, "Pass through rows that the columns with ID is not in given file",
		ARG_LABEL,"Is not in File",
                new Arg(ARG_COLUMN, "Column in the file", ATTR_TYPE, TYPE_COLUMN),
                new Arg("file", "The file"),
                new Arg("column2", "Column in main file", ATTR_TYPE, TYPE_COLUMN)),	

        new Cmd(CMD_IFMATCHESFILE, "Pass through rows that the columns with ID begins with something in the given file",
		ARG_LABEL,"Matches in File",
                new Arg(ARG_PATTERN, "Pattern template, e.g. ^${value}"),
                new Arg(ARG_COLUMN, "Column in the file", ATTR_TYPE, TYPE_COLUMN),
                new Arg("file", "The file"),
                new Arg("column2", "Column in main file", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_IFNOTMATCHESFILE, "Pass through rows that the columns with ID does not begin with something in the given file",
		ARG_LABEL,"Not Matches in File",
                new Arg(ARG_PATTERN, "Pattern template, e.g. ^${value}"),
                new Arg("file", "The file"),
                new Arg("column2", "Column in main file", ATTR_TYPE, TYPE_COLUMN)),	
	

        new Cmd(CMD_SKIPPATTERN, "Skip any line that matches the pattern",
		ARG_LABEL,"Skip Pattern",
                new Arg(ARG_PATTERN, "", ATTR_TYPE, TYPE_PATTERN)),
        new Cmd(CMD_SKIPROWS, "Skip number of processed rows.",
		ARG_LABEL,"Skip Rows",
                new Arg("rows", "How many rows to skip", ATTR_TYPE, TYPE_NUMBER)),

        new Cmd(CMD_ENSURE_NUMERIC, "Throw error if non-numeric",
		ARG_LABEL,"Ensure Numeric",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
	
        /** *  Sliceand dice * */
        new Category("Slice and Dice","Add/remove columns, rows, restructure, etc"),
        new Cmd(CMD_COLUMNS,  "Only include the given columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_NOTCOLUMNS, "Don't include given columns",
		ARG_LABEL,"Not Columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_FIRSTCOLUMNS, "Move columns to beginning",
		ARG_LABEL,"Move Columns First",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_LASTCOLUMNS, "Move columns to end",
		ARG_LABEL,"Move Columns Last",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_COLUMNSBEFORE, "Move columns before the given column",
		ARG_LABEL,"Move Columns Before",
                new Arg(ARG_COLUMN, "Column to move before", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_COLUMNS, "Columns to move", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_COLUMNSAFTER, "Move columns after given column",
		ARG_LABEL,"Move Columns After",
                new Arg(ARG_COLUMN, "Column to move after", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_COLUMNS, "Columns to move", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_DELETE, "Remove the columns",
		ARG_LABEL,"Delete Columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_CUT, "Drop rows",
		ARG_LABEL,"Delete Rows",
                new Arg("rows",   "One or more rows. -1 to the end. e.g., 0-3,5,10,-1",
                        ATTR_TYPE, TYPE_ROWS)),
        new Cmd(CMD_INCLUDE, "Only include specified rows",
		ARG_LABEL,"Include Rows",
                new Arg("rows", "one or more rows, -1 to the end", ATTR_TYPE,
                        TYPE_ROWS)),
	new Cmd(CMD_ROWS_FIRST, "Move rows to the top that match the pattern",
		ARG_LABEL,"Move Rows First",
                new Arg(ARG_COLUMNS, "columns to match on", ATTR_TYPE,  TYPE_COLUMNS),
                new Arg(ARG_PATTERN, "Pattern")),
	new Cmd(CMD_ROWS_LAST, "Move rows to the end of list that match the pattern",
		ARG_LABEL,"Move Rows Last",
                new Arg(ARG_COLUMNS, "columns to match on", ATTR_TYPE,  TYPE_COLUMNS),
                new Arg(ARG_PATTERN, "Pattern")),

        new Cmd(CMD_COPY, "Copy column",
		ARG_LABEL,"Copy Column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN), 
		new Arg(ARG_NAME)),
        new Cmd(CMD_ADD,"Add new columns",
		ARG_LABEL,"Add Columns",
		new Arg("names", "Comma separated list of new column names",ATTR_TYPE,TYPE_LIST),		
		new Arg("values",
			"Comma separated list of new values", ATTR_TYPE,
			TYPE_LIST)),
        new Cmd(CMD_INSERT,"Insert new column values",
		ARG_LABEL,"Insert Columns",
		new Arg(ARG_COLUMN, "Column to insert before", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_NAME, "Name of new column"),		
		new Arg("values",
			"Value to insert. Use ${row} to add the row index", ATTR_TYPE,
			TYPE_LIST)),
	new Cmd(CMD_CONCAT, "Create a new column from the given columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("delimiter"),
		new Arg(ARG_NAME,"Name of new colums")),
        new Cmd(CMD_CONCATROWS, "Concatenate multiple rows into a single row",
                new Arg("num_rows", "Number of rows", ATTR_TYPE, TYPE_NUMBER)), 
        new Cmd(CMD_COMBINE,
                "Combine columns with the delimiter. deleting columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("delimiter"),
                new Arg("column name","New column name")),
        new Cmd(CMD_COMBINEINPLACE, "Combine columns with the delimiter",
		ARG_LABEL,"Combine in place",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS), 
		new Arg("delimiter"),
                new Arg("column name","New column name")),
        new Cmd(CMD_MERGE,
                "Apply operators to columns",
                new Arg(ARG_COLUMNS, "Columns to merge", ATTR_TYPE, TYPE_COLUMNS),
		new Arg(ARG_NAME, "New column(s) name"),
                new Arg("operator", "Operator", "values",
                        "average,min,max,count")),
        new Cmd(CMD_SPLIT, "Split the column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("delimiter", "What to split on"),
                new Arg("names", "Comma separated new column names", ATTR_TYPE,
                        TYPE_LIST)),
        new Cmd(CMD_SPLAT,
                "Create a new column from the values in the given column",
                new Arg("keycol","Key column", ATTR_TYPE,TYPE_COLUMN),
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("delimiter"),
		new Arg(ARG_NAME, "new column name")),
        new Cmd(CMD_ROLL, "Roll columns down into rows",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_SHIFT, "Shift columns over by count for given rows",
                new Arg("rows", "Rows to apply to", ATTR_TYPE, TYPE_ROWS),
                new Arg(ARG_COLUMN, "Column to start at", ATTR_TYPE, TYPE_COLUMN),
                new Arg("count")),
        new Cmd(CMD_SLICE,
                "Slide columns down and over to append new rows to the bottom",
                new Arg(ARG_COLUMNS, "Columns to move", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("dest", "Desc column to move to", ATTR_TYPE, TYPE_COLUMN),
                new Arg("fill", "Comma separated list of values to fill out the new row")),				
        new Cmd(CMD_ADDCELL, "Add a new cell at row/column",
		ARG_LABEL,"Add Cell",
		new Arg("row"),
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN), 
		new Arg("value")),
        new Cmd(CMD_DELETECELL,  "Delete cell at row/column",
		ARG_LABEL,"Delete Cell",
		new Arg("row"),
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_CLONE,
                "Clone each row N times",
                new Arg("count", "Number of clonese",ATTR_TYPE, TYPE_NUMBER)),
        new Cmd(CMD_APPENDROWS, "Only include specified rows",
		ARG_LABEL,"Append Rows",
                new Arg("skip", "How many rows to skip", ATTR_TYPE, TYPE_NUMBER),
                new Arg("count", "How many rows to merge", ATTR_TYPE, TYPE_NUMBER),
                new Arg("delimiter", "How many rows to merge")),

        new Cmd(CMD_MERGEROWS, "Merge rows",
		ARG_LABEL,"Merge Rows",
                new Arg("rows", "2 or more rows", ATTR_TYPE, TYPE_ROWS),
                new Arg("delimiter"),
		new Arg("close")),
        new Cmd(CMD_ROWOP, "Apply an operator to columns and merge rows",
                new Arg("keys", "Key columns", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("values", "Value columns", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("operator", "Operator", "values",
                        "average,min,max,count")),
        new Cmd(CMD_ROTATE, "Rotate the data"),
        new Cmd(CMD_FLIP, "Reverse the order of the rows except the header"),
        new Cmd(CMD_MAKEFIELDS, "Make new columns from data values",
		ARG_LABEL,"Make Fields",
                new Arg(ARG_COLUMN, "Column to get new column header#", ATTR_TYPE,
			TYPE_COLUMN),
		new Arg("value columns",
			"Columns to get values from", ATTR_TYPE,
			TYPE_COLUMNS),
		new Arg("unique column",
			"The unique value, e.g. date", ATTR_TYPE,
			TYPE_COLUMN),
		new Arg("other columns",
			"Other columns to include", ATTR_TYPE,
			TYPE_COLUMNS)),
        new Cmd(CMD_MELT, "Use values in header to make new row",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("header label"),
                new Arg("value label")),
        new Cmd(CMD_EXPLODE, "Make separate files based on value of column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_JOIN, "Join the 2 files together",
                new Arg("key columns", "key columns the file to join with", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("value_columns", "value columns"),
                new Arg("file", "File to join with", ATTR_TYPE, "file"),
                new Arg("source_columns", "source key columns"),
		new Arg("default_value", "default value - can be a comma separated list of defaults")),
        new Cmd(CMD_FUZZYJOIN, "Join the 2 files together using fuzzy matching logic",
		ARG_LABEL,"Fuzzy Join",
                new Arg("threshold", "Score threshold 0-100. Default:85. Higher number better match"),
                new Arg("key columns", "Numeric column numbers of the file to join with", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("value_columns", "numeric columns of the values to join"),
                new Arg("file", "File to join with", ATTR_TYPE, "file"),
                new Arg("source_columns", "source key columns"),
		new Arg("default_value", "default value")),

        new Cmd(CMD_CROSS, "Make a cross product of 2 data files",
                new Arg("file", "File to cross with", ATTR_TYPE, "file")),


        new Cmd(CMD_NORMAL, "Normalize the strings",
		ARG_LABEL,"Normalize",
                new Arg(ARG_COLUMNS, "Columns", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_COUNTUNIQUE, "Count number of unique values",
		ARG_LABEL,"Count Unique Values",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_DISSECT, "Make fields based on patterns",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_PATTERN,"e.g., \"(field1:.*) (field2:.*) ...\"",ATTR_TYPE,TYPE_PATTERN,ATTR_SIZE,"80")),
        new Cmd(CMD_KEYVALUE, "Make fields from key/value pairs, e.g. name1=value1 name2=value2 ...",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_FIRSTCHARS,
		"Extract first N characters and create new column",
		ARG_LABEL,"Make Column from  1st Chars",
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_NAME, "New column name"),
		new Arg("number", "Number of characters")),
        new Cmd(CMD_LASTCHARS,
		"Extract last N characters and create new column",
		ARG_LABEL,"Make Column from  Last Chars",
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_NAME, "New column name"),
		new Arg("number", "Number of characters")),
        new Cmd(CMD_BETWEEN_INDICES,
		"Extract characters between the 2 indices",
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_NAME, "New column name"),
		new Arg("start", "Start index"),
		new Arg("end", "End index")),		
        new Cmd(CMD_FROMHEADING, "Extract column values from headings",
		ARG_LABEL,"From Heading",
                new Arg(ARG_COLUMNS, "Columns of headings", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("names", "Comma separated list of new column names", ATTR_TYPE, TYPE_LIST),
                new Arg(ARG_PATTERN, "Regexp to apply to header with () defining column values")),				
        /** *  Change values * */
        new Category("Change"),
        new Cmd(CMD_CHANGE, "Change columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
                new Arg("substitution string",HELP_SUBSTITUTION)),
        new Cmd(CMD_CHANGEROW, "Change the values in the row/cols",
		ARG_LABEL,"Change Row Values",
                new Arg("rows","",ATTR_TYPE,TYPE_LIST),
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
                new Arg("substitution string",HELP_SUBSTITUTION)),
        new Cmd(CMD_REPLACE, "Replace",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("substitution string",HELP_SUBSTITUTION+"<br>use {value} for value")),
        new Cmd(CMD_SET, "Write the value into the cells",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("rows", "", ATTR_TYPE, TYPE_LIST),
		new Arg("value")),
        new Cmd(CMD_CLEANWHITESPACE, "Clean whitespace",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_MACRO,
		"Look for the pattern in the header and apply the template to make a new column, template: '{1} {2} ...', use 'none' for column name for no header",
		new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
		new Arg("template"),
		new Arg("column label")),
        new Cmd(CMD_SETCOL,
		"Write the value into the write col for rows that match the pattern",
		new Arg(ARG_COLUMN, "match col #", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
		new Arg("write column", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("value")),
        new Cmd(CMD_COPYIF,
		"Copy column 2 to column 3 if all of the columns match the pattern",
		ARG_LABEL,"Copy If Match",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
		new Arg("column1", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("column2", "", ATTR_TYPE, TYPE_COLUMN)),		
        new Cmd(CMD_COPYCOLUMNS,
		"Copy columns 1  to columns 2",
		ARG_LABEL,"Copy Columns",
		new Arg("columns1", "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("columns2", "", ATTR_TYPE, TYPE_COLUMNS)),		

        new Cmd(CMD_FILLDOWN,
		"Fill down with last non-null value",
		ARG_LABEL,"Fill Down",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_FILLACROSS,
		"Fill across with last non-null value",
		ARG_LABEL,"Fill Across",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("rows",   "One or more rows. -1 to the end. e.g., 0-3,5,10,-1",
                        ATTR_TYPE, TYPE_ROWS)),		
        new Cmd(CMD_UNFILL,
		"Set following cells to blank if the same as previous cell",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_PRIORPREFIX,
		"Append prefix from the previous element to rows that match pattern",
		ARG_LABEL,"Prior Prefix",
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
		new Arg("delimiter")),
        new Cmd(CMD_CASE, "Change case of column - type:lower,upper,proper,capitalize",
		ARG_LABEL,"Change Case",
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("type", "", "values", "lower,upper,proper,capitalize")),
        new Cmd(CMD_TOID, "Convert the column(s) into IDS (lowercase, no space, a-z0-9_)",
		ARG_LABEL,"TOID",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_PADLEFT, "Pad left with given character",
		ARG_LABEL,"Pad Left",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("character", "Character to pad to"),
                new Arg("length", "Length")),
        new Cmd(CMD_PADRIGHT, "Pad right with given character",
		ARG_LABEL,"Pad Right",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("character", "Character to pad to"),
                new Arg("length", "Length")),	


        new Cmd(CMD_TRIM, "Trim leading and trailing white space",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_TRIMQUOTES, "Trim leading and trailing quotes",
		ARG_LABEL,"Trim Quotes",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_WIDTH, "Limit the string size of the columns",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("size")),
        new Cmd(CMD_PREPEND,
		"Add the text to the beginning of the file. use _nl_ to insert newlines",
		new Arg("text")),
        new Cmd(CMD_PAD, "Add or remove columns to achieve the count",
                new Arg("count"),
		new Arg("pad string")),
        new Cmd(CMD_PREFIX, "Add prefix to column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("prefix","String to use")),
        new Cmd(CMD_SUFFIX, "Add suffix to column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("suffix")),
        new Cmd(CMD_SUBST, "Create a new column with the template",
                new Arg("column_name", "New Column Name"),
                new Arg("template", "Template - use ${column_name} ... ")),
        new Cmd(CMD_ASCII, "Convert non ascii characters",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("substitution string", HELP_SUBSTITUTION)),
        new Cmd(CMD_CLEANPHONE, "Clean the phone number",
		ARG_LABEL,"Clean Phone",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_FORMATPHONE, "Format the phone number",
		ARG_LABEL,"Format Phone #",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_ISMOBILE, "Add a true/false if the string is a mobile phone",
		ARG_LABEL,"Is Mobile",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_SMS, "Send a text message - only for command line",
		ARG_LABEL,"Send SMS",
                new Arg(ARG_COLUMN, "Phone number", ATTR_TYPE, TYPE_COLUMN),
                new Arg("campaign", "Campaign"),
                new Arg("message", "Message template", ATTR_SIZE, "60",ATTR_ROWS, "6")),
        new Cmd(CMD_JS,
		"Define Javascript (e.g., functions) to use later in the -func call",
		ARG_LABEL,"Define Javascript",
		new Arg("javascript", "", ATTR_ROWS, "6")),
        new Cmd(CMD_FUNC, "Apply the javascript function. Use _colname or _col#",
		ARG_LABEL,"Javascript Function",
                new Arg("names", "New column names", ATTR_TYPE, TYPE_LIST),
                new Arg("javascript", "javascript expression", ATTR_SIZE, "60")),
        new Cmd(CMD_ENDSWITH, "Ensure that each column ends with the string",
		ARG_LABEL,"Ends With",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("string")),
        new Cmd(CMD_TRUNCATE, "",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("max length"),
		new Arg("suffix")),
        new Cmd(CMD_EXTRACT, "Extract text from column and make a new column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_PATTERN, HELP_PATTERN, ATTR_TYPE, TYPE_PATTERN),
                new Arg("replace with", "use 'none' for no replacement"),
                new Arg(ARG_NAME,"new column name")),
        new Cmd(CMD_URLARG, "Extract URL argument and make a new column",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN),
                new Arg("argname", "URL arg name")),
        new Cmd(CMD_EXTRACTHTML, "Extract text from HTML",
		ARG_LABEL,"Extract HTML",
                new Arg(ARG_COLUMN, "URL Column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("names", "Comma separated list of new column names",ATTR_TYPE,TYPE_LIST),
		new Arg(ARG_PATTERN,HELP_PATTERN,ATTR_TYPE, TYPE_PATTERN)),		
        new Cmd(CMD_HTMLINFO, "Extract icon and description from input URL",
                new Arg(ARG_COLUMN, "URL Column", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_CHECKMISSING, "Check for missing URL",
		ARG_LABEL,"Check Missing URL",
                new Arg(ARG_COLUMN, "URL Column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("replace_with", "Replace with")),
        new Cmd(CMD_XMLENCODE,
		"Encode the value for XML",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_URLENCODE, "URL encode the columns",
		ARG_LABEL,"URL Encode",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_URLDECODE, "URL decode the columns",
		ARG_LABEL,"URL Decode",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),	
        new Cmd(CMD_MAP, "Change values in column to new values",
                new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("new column name"),
                new Arg("value","old_value new_value old_value new_value")),
        new Cmd(CMD_FORMAT, "Apply decimal format to the columns (see https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html)", 
		ARG_LABEL,"Decimal Format",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("format", "Decimal format  e.g. '##0.00'")),
        new Cmd(CMD_DENORMALIZE,
		"Read the id,value from file and substitute the value in the dest file col idx",
		new Arg("file", "From csv file", ATTR_TYPE, "file"), 
		new Arg("from id idx"),
		new Arg("from value idx"),
		new Arg("to idx"),
		new Arg("new col name"),
		new Arg("mode replace add")),
        new Cmd(CMD_BREAK, "Break apart column values and make new rows",
		new Arg("label1"),
		new Arg("label2"),
		new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS)),

        new Cmd(CMD_PARSEEMAIL, "Parse out name and email",
		ARG_LABEL,"Parse Email",
		new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS)),
		
        new Cmd(CMD_MAKEIDS, "Turn the header row into IDs (lowercase, no space, a-z0-9_)",
		ARG_LABEL,"Make IDs"),

        new Cmd(CMD_FAKER, "Fake up data. See the docs at https://ramadda.org/repository/userguide/seesv.html#-faker",
		ARG_LABEL,"Fake Data",
		new Arg("what","firstname|lastname|fullname|etc"),
		new Arg(ARG_COLUMNS,"Columns to change. If none given then add the fake value",ATTR_TYPE,TYPE_COLUMNS)),		

        new Cmd(CMD_EDIT, "Hand edit a column (command line only). ESC-stop, BLANK-skip",
                new Arg(ARG_COLUMN, "key column", ATTR_TYPE, TYPE_COLUMN)),
	/** *  Add values * */
        new Category("Values"),
        new Cmd(CMD_MD, "Make a message digest of the column values",
		ARG_LABEL,"Message Digest",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("type", "", ATTR_TYPE,"enumeration","values", "MD5,SHA-1,SHA-256,SHA-512,SHA3-256,SHA3-512,")),
        new Cmd(CMD_TOB64, "Base 64 Encode",
		ARG_LABEL,"Base 64 Encode",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_FROMB64, "Base 64 Decode",
		ARG_LABEL,"Base 64 Decode",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_ROT13, "Rot 13",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
	new Cmd(CMD_ENCRYPT, "Encrypt using AES with SHA-256 key",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("password")),
	new Cmd(CMD_DECRYPT, "Encrypt using AES with SHA-256 key",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("password")),	
        new Cmd(CMD_UUID, "Add a UUID field"),
        new Cmd(CMD_NUMBER, "Add 1,2,3... as column"),
        new Cmd(CMD_LETTER, "Add 'A','B', ... as column"),
	new Cmd(CMD_SOUNDEX, "Generate a soundex code",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_WIKIDESC, "Add a description from Wikipedia",
		ARG_LABEL,"Add Wikipedia Desc.",
                new Arg(ARG_COLUMNS, "Search string columns", ATTR_TYPE, TYPE_COLUMNS), 
		new Arg("suffix","Text to add after")),
        new Cmd(CMD_IMAGE, "Do a Bing image Search for an image",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("suffix","Text to add after")),
        new Cmd(CMD_EMBED, "Download the URL and embed the image contents",
                new Arg("url column")),
        new Cmd(CMD_FETCH, "Fetch the URL and embed the contents",
                new Arg(ARG_NAME,"Name of new column"),
                new Arg("ignore_errors","Ignore Errors e.g., true or false",ATTR_TYPE,"boolean"),
		new Arg("url","URL template, e.g., https://foo.com/${column_name}")),	
	new Cmd(CMD_IMAGEFILL,
		"Search for an image with the query column text if the given image column is blank. Add the given suffix to the search. ",
		ARG_LABEL,"Image Fill",
		new Arg("querycolumn", "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("suffix"),
		new Arg("imagecolumn", "", ATTR_TYPE, TYPE_COLUMN)),
        new Cmd(CMD_DOWNLOAD, "Download the URL",
                new Arg(ARG_COLUMN, "Column that holds the URL", ATTR_TYPE, TYPE_COLUMN),
		new Arg("suffix", "File suffix")),
        new Cmd(CMD_GENDER, "Figure out the gender of the name in the column",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),


        /** *  Dates * */
        new Category("Dates"),
        new Cmd(CMD_INDATEFORMATS, "Specify one or more date formats for parsing",
		ARG_LABEL,"Input Date Format",
                new Arg("format", "e.g. yyyy-MM-dd HH:mm:ss.  Use semi-colon separated formats for multiples. <a target=_help href=https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html>Help</a>"),
		new Arg("timezone", "")),
        new Cmd(CMD_OUTDATEFORMAT, "Specify date format for formatting",
		ARG_LABEL,"Output Date Format",
                new Arg("format", "e.g. yyyy-MM-dd HH:mm:ss. <a target=_help href=https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html>Help</a>"),
		new Arg("timezone", "")),		

        new Cmd(CMD_CONVERTDATE, "Convert date", 
		ARG_LABEL,"Convert Date",
                new Arg(ARG_COLUMNS, "Columns to convert", ATTR_TYPE, TYPE_COLUMNS)),

        new Cmd(CMD_ADDDATE, "Add date", 
		ARG_LABEL,"Add to Date",
                new Arg("date_column", "Date Column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("value", "Value Column"),		
                new Arg("value_type", "Value type - millisecond,second,minute,hour,hour_of_day,week,month,year")),
        new Cmd(CMD_CLEARDATE, "Clear date components", 
		ARG_LABEL,"Clear the date to",
                new Arg("date_column", "Date Column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("component", "Date component",
			ATTR_TYPE,"enumeration",
			"values","millisecond,second,minute,hour_of_day,day_of_month,month")),	

        new Cmd(CMD_EXTRACTDATE, "Extract date",
		ARG_LABEL,"Extract Date",
		new Arg("date column", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("what", "What to extract, e.g., year, month, day_of_week, etc", "values",
			"era,year,month,day_of_month,day_of_week,week_of_month,\nday_of_week_in_month,am_pm,hour,hour_of_day,\nminute,second,millisecond,days_in_year, hours_in_year, minutes_in_year,seconds_in_year")),
        new Cmd(CMD_FORMATDATE, "Format date",
		ARG_LABEL,"Format Date - use -outdateformat to set the date format",
                new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS)),

        new Cmd(CMD_FORMATDATEOFFSET, "Format the date offset, e.g. the hours in year",
		ARG_LABEL,"Format Date Offset",
		new Arg("column", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("what", "What type of offset, e.g., year, month, day_of_week, etc", "values",
			"days_in_year, hours_in_year, minutes_in_year,seconds_in_year")),	


        new Cmd(CMD_ELAPSED, "Calculate elapsed time (ms) between rows",
                new Arg(ARG_COLUMN,"",ATTR_TYPE,TYPE_COLUMN)),
        new Cmd(CMD_MSTO, "Convert milliseconds to",
                new Arg(ARG_COLUMN,"",ATTR_TYPE,TYPE_COLUMN),
                new Arg("to","seconds|hours|days|weeks|months|years")),	

        new Cmd(CMD_LATEST, "Pass through rows whose date is the latest in the group of rows defined by the key column",
                new Arg(ARG_COLUMNS, "Key columns", ATTR_TYPE, TYPE_COLUMNS),
                new Arg(ARG_COLUMN, "Date column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("format","Date Format, e.g. yyyy-MM-dd")),

        new Cmd(CMD_DATEDIFF, "Calculate elapsed time between columns column1-column2",
		ARG_LABEL,"Date Difference",
                new Arg("column1","Column 1",ATTR_TYPE,TYPE_COLUMN),
                new Arg("column2","Column 2",ATTR_TYPE,TYPE_COLUMN),
		new Arg("unit","Unit-milliseconds,seconds,minutes,hours,days",ATTR_TYPE,"enumeration","values","milliseconds,seconds,minutes,hours,days")),
	new Cmd(CMD_DATECOMPARE, "add a true/false column comparing the date values",
		ARG_LABEL,"Date Compare",
		new Arg("column1", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("column2", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("operator", "<,<=,=,!=,>=,>", ATTR_TYPE, "enumeration","values","<,<=,=,!=,>=,>")),	
	

        /** *  Numeric * */
	new Category("Numeric"),
        new Cmd(CMD_SCALE, "Set value={value+delta1}*scale+delta2",
		ARG_LABEL,"Scale Value",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("delta1","",ATTR_TYPE,TYPE_NUMBER),
		new Arg("scale","",ATTR_TYPE,TYPE_NUMBER),
                new Arg("delta2","",ATTR_TYPE,TYPE_NUMBER)),
        new Cmd(CMD_MAKENUMBER, "Try to parse as number",
		ARG_LABEL,"Make Number",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_GENERATE, "Add row values",
		new Arg("label"),
		new Arg("start","",ATTR_TYPE,TYPE_NUMBER),
		new Arg("step","",ATTR_TYPE,TYPE_NUMBER)),
        new Cmd(CMD_DECIMALS, "Round decimals",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("num_decimals","how many decimals to round to",
			ATTR_TYPE,TYPE_NUMBER)),
        new Cmd(CMD_FUZZ, "fuzz the number. if num_places less than zero than that is the # of decimals. else that is the lower digits to fuzz out",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("num_places","how many places to round to. use <=0 for decimals",ATTR_TYPE,TYPE_NUMBER),
		new Arg("num_random_digits","how many random digits",ATTR_TYPE,TYPE_NUMBER)),	
        new Cmd(CMD_CEIL, "Set the max value",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("value","Value",ATTR_TYPE,TYPE_NUMBER)),
        new Cmd(CMD_FLOOR, "Set the min value",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("value","Value",ATTR_TYPE,TYPE_NUMBER)),		
        new Cmd(CMD_DELTA,
                "Add column that is the delta from the previous step",
                new Arg("key columns","",ATTR_TYPE,TYPE_COLUMNS),
		new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS)),
        new Cmd(CMD_RUNNINGSUM,
                "Make a running sum of the column values",
		new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS)),
        new Cmd(CMD_TRENDCOUNTER,
                "Make counter field that is incremented everytime the value column decreases",
		new Arg(ARG_COLUMN,"The value column",ATTR_TYPE,TYPE_COLUMN),
		new Arg("name","Name of counter column")),	
        new Cmd(CMD_OPERATOR,
                "Apply the operator to the given columns and create new one",
                new Arg(ARG_COLUMNS,"Columns",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("new col name"),
		new Arg("operator","Operator:+,-,*,/,%,average",
			ATTR_TYPE,"enumeration","values","+,-,*,/,%,average")),
	new Cmd(CMD_COMPARE, "Add a true/false column comparing the values",
		new Arg("column1", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("column2", "", ATTR_TYPE, TYPE_COLUMN),
		new Arg("operator", "<,<=,=,!=,>=,>", ATTR_TYPE, "enumeration","values","<,<=,=,!=,>=,>")),
        new Cmd(CMD_ROUND, "Round the values",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_ABS, "Make absolute values",
		ARG_LABEL,"Absolute Value",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
	//TODO:
        new Cmd(CMD_CLIP, "Clip the number to within the range",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("min","",ATTR_TYPE,TYPE_NUMBER),
		new Arg("max","",ATTR_TYPE,TYPE_NUMBER)),
        new Cmd(CMD_RAND, "make random value",
		new Arg("column name"),
		new Arg("minrange","Minimum range (e.g. 0)",ATTR_TYPE,TYPE_NUMBER),
		new Arg("maxrange","Maximum range (e.g. 1)",ATTR_TYPE,TYPE_NUMBER)),		
        new Cmd(CMD_EVEN, "Add true if the column starts with an even number",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_SUM,
		"Sum values keying on key column value. If no value columns specified then do a count",
		new Arg("key columns", "",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("value columns",  "",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("carry over columns", "",ATTR_TYPE,TYPE_COLUMNS)),
        new Cmd(CMD_PIVOT,
		"Make a pivot table",
		new Arg("key columns","Columns to key on",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("column columns", "The columns the values of which are used to make the new columns in the result",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("value column","The value column",ATTR_TYPE,TYPE_COLUMN),
		new Arg("operator","The operator to apply -  count,sum,average,min,max")),
        new Cmd(CMD_SUMMARY,
		"count/sum/average/min/max values keying on key column value. If no value columns specified then do a count",
		new Arg("key columns","Columns to key on",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("value columns", "Columns to apply operators on",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("carry over columns","Extra columns to include",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("ops","any of count,sum,average,min,max")),
        new Cmd(CMD_HISTOGRAM,
		"Make a histogram with the given column and bins",
		new Arg(ARG_COLUMN,"The column",ATTR_TYPE,TYPE_COLUMN),
		new Arg("bins","Comma separated set of bin values"),
		new Arg("value columns","Extra columns to sum up",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("ops","ops to apply to extra columns - any of count,sum,average,min,max")),		
        new Cmd(CMD_PERCENT, "Add columns together. Replace with their percentage", 
		ARG_LABEL,"Calculate Percent",
		new Arg(ARG_COLUMNS,"Columns to add",ATTR_TYPE,"columns")),
        new Cmd(CMD_INCREASE, "Calculate percent increase",
		ARG_LABEL,"% Increase",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS), 
		new Arg("how far back","",ATTR_TYPE,TYPE_NUMBER)),
        new Cmd(CMD_DIFF, "Difference from previous value",
		ARG_LABEL,"Difference",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS), 
		new Arg("how far back (default 1)","",ATTR_TYPE,TYPE_NUMBER)),	
        new Cmd(CMD_AVERAGE, "Calculate a moving average", 
		new Arg(ARG_COLUMNS,"Columns",ATTR_TYPE,"columns"),
                new Arg("period","",ATTR_TYPE,TYPE_NUMBER),
		new Arg("label")),
        new Cmd(CMD_RANGES, "Create a new column with the (string) ranges where the value falls in",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg(ARG_NAME, "New column name"),		
		new Arg("start", "Numeric start of range"),
		new Arg("size", "Numeric size of range")),		
        new Cmd(CMD_BYTES, "Convert suffixed values (e.g., 2 MB) into the number",
                new Arg("unit", "", ATTR_TYPE, "enumeration","values","binary,metric"),
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_COLUMN_AND, "And values", ARG_LABEL,"Logical And",
		new Arg(ARG_NAME,"New column name"),
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_COLUM_NOR, "Or values",
		ARG_LABEL,"Logical Or",
		new Arg(ARG_NAME,"New column name"),
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_COLUMN_NOT, "Not value",
		new Arg(ARG_NAME,"New column name"),
		new Arg(ARG_COLUMN, "", ATTR_TYPE, TYPE_COLUMN)),		

	new Cmd(CMD_CHECK, "Check that the values are numbers",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("what", "How strict", ATTR_TYPE, "enumeration","values","strict,ramadda")),

        /** * Geocode  * */
        new Category("Geospatial"),
        new Cmd(CMD_GEOCODE, 
		"Geocode using given columns", 
		new Arg(ARG_COLUMNS, "Address columns", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("prefix", "optional prefix e.g., state: or county: or country:"),
                new Arg("suffix")),
        new Cmd(CMD_GEOCODEIFNEEDED, 
		"Geocode if needed", 
		ARG_LABEL,"Geocode if needed",
		new Arg(ARG_COLUMNS, "Address columns", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("prefix", "optional prefix e.g., state: or county: or country:"),
                new Arg("suffix"),
                new Arg(ARG_LATITUDE, "latitude column",ATTR_TYPE,TYPE_COLUMN),
                new Arg(ARG_LONGITUDE, "longitude column",ATTR_TYPE,TYPE_COLUMN)),		
        new Cmd(CMD_GEOCODEADDRESSDB, "Geocode for import into RAMADDA's DB. The lat/lon is one semi-colon delimited column", 
		ARG_LABEL,"Geocode for DB",
                new Arg(ARG_COLUMNS,"columns",ATTR_TYPE,TYPE_COLUMNS),
                new Arg("prefix", "optional prefix e.g., state: or county: or country:"),
		new Arg("suffix")),
        new Cmd(CMD_GEOCODEJOIN, "Geocode with file",
		ARG_LABEL,"Geocode Join",
                new Arg(ARG_COLUMN, "key column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("csv file", "File to get lat/lon from", ATTR_TYPE,
                        "file"),
		new Arg("key idx"), new Arg("lat idx"), new Arg("lon idx")),
        new Cmd(CMD_BOUNDS, 
		"Geocode within bounds", 
		new Arg("north"),
		new Arg("west"),
		new Arg("south"),
		new Arg("east")),
        new Cmd(CMD_DECODELATLON, 
		"Decode latlon", 
		ARG_LABEL,"Deocde Lat/Lon",
		new Arg(ARG_COLUMNS,"Lat or Lon column",ATTR_TYPE,TYPE_COLUMNS)),
        new Cmd(CMD_GETADDRESS, "Get address from lat/lon",
		ARG_LABEL,"Reverse geocode",
                new Arg(ARG_LATITUDE, "latitude column"),
                new Arg(ARG_LATITUDE, "latitude column")),		
        new Cmd(CMD_STATENAME, "Add state name from state ID",
		ARG_LABEL,"State Name from ID",
                new Arg("state_column","State ID column",ATTR_TYPE,TYPE_COLUMN)),
	new Cmd(CMD_GEONAME, "Look up location name",
                new Arg("lookup","('counties' or 'states' or 'countries' or 'timezones')"),
                new Arg("fields","fields in shapefile"),		
                new Arg(ARG_LATITUDE, "Latitude column", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_LONGITUDE, "Longitude column", ATTR_TYPE, TYPE_COLUMN)),
	new Cmd(CMD_GEOCONTAINS, "Check for containment",
                new Arg("lookup","('counties' or 'states' or 'countries' or 'timezones')"),
                new Arg(ARG_NAME,"new column name"),		
                new Arg(ARG_LATITUDE, "Latitude column", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_LONGITUDE, "Longitude column", ATTR_TYPE, TYPE_COLUMN)),		
	new Cmd(CMD_ELEVATION, "Look up elevation(using 1/3 arc-second DEM)",
                new Arg(ARG_LATITUDE, "Latitude column", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_LONGITUDE, "Longitude column", ATTR_TYPE, TYPE_COLUMN)),	

        new Cmd(CMD_MERCATOR, "Convert x/y to lon/lat",
		ARG_LABEL,"Convert Mercator",
		new Arg(ARG_COLUMNS,"x and y columns")),
        new Cmd(CMD_REGION, "Add the state's region",
                new Arg(ARG_COLUMNS, "Columns with state name or abbrev.", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_POPULATION, "Add in population from address",
                new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("prefix", "e.g., state: or county: or city:"),
		new Arg("suffix")),

	new Cmd(CMD_NEIGHBORHOOD, "Look up neighborhood for a given location",
                new Arg(ARG_LATITUDE, "Latitude column", ATTR_TYPE, TYPE_COLUMN),
                new Arg(ARG_LONGITUDE, "Longitude column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("default","Default value")),	


        /** * Other  * */
        new Category("Misc"),
        new Cmd(CMD_APPLY, "Apply the commands to each of the columns",
		ARG_LABEL,"Procedure",
		new Arg(ARG_COLUMNS, "Columns to expand with", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("commands", "Commands. Use the macro ${column}. End with -endapply", ATTR_ROWS, "6")),
        new Cmd(CMD_SORTBY, "", ARG_LABEL,"Sort",
                new Arg(ARG_COLUMNS, "Column to sort on", ATTR_TYPE, TYPE_COLUMNS),
                new Arg("direction", "Direction - up or down", ATTR_TYPE, "enumeration","values","up,down"),
		new Arg("how", "How to sort - string, length, date, extract (number)", ATTR_TYPE, "enumeration","values","string,number,length,date,extract")),

	/*        new Cmd(CMD_SORT, "Sort",
		  new Arg(ARG_COLUMNS, "Column to sort on", ATTR_TYPE, TYPE_COLUMNS)),
		  new Cmd(CMD_DESCSORT, "",
		  ARG_LABEL,"Sort descending",
		  new Arg(ARG_COLUMN, "Column to descending sort on", ATTR_TYPE,  TYPE_COLUMN)),
	*/
        new Cmd(CMD_COUNT, "Show count"),
        new Cmd(CMD_ALIAS, "Set a field alias",
		new Arg(ARG_NAME,"Name"),
		new Arg("alias","Alias")),
        new Cmd(CMD_VALUE, "Define a macro value for later use",
		new Arg(ARG_NAME,ARG_NAME),
		new Arg("value","Value")),
        new Cmd(CMD_FILEPATTERN, "Extract a macro value from a filename",
		ARG_LABEL,"File Pattern",
		new Arg(ARG_NAME,"Macro name"),
		new Arg(ARG_PATTERN,HELP_PATTERN,ATTR_TYPE, TYPE_PATTERN)),		
        new Cmd(CMD_CHANGELINE,  "Change the line",
		ARG_LABEL,"Change Line",
                new Arg("from","From pattern"),
		new Arg("to","To string")),
        new Cmd(CMD_CHANGERAW,  "Change input text",
		ARG_LABEL,"Change Input",
                new Arg("from","From pattern"),
		new Arg("to","To string")),
        new Cmd(CMD_CROP,  "Crop last part of string after any of the patterns",
                new Arg(ARG_COLUMNS,"",ATTR_TYPE,TYPE_COLUMNS),
		new Arg("patterns","Comma separated list of patterns",ARG_TYPE,"list")),
        new Cmd(CMD_STRICT,
		"Be strict on columns. any rows that are not the size of the other rows are dropped"),
        new Cmd(CMD_FLAG,
		"Be strict on columns. any rows that are not the size of the other rows are shown"),
        new Cmd(CMD_PROP, "Set a property",
                new Arg("property", "", "values", "position"),
                new Arg("value", "start, end, etc")),

        new Cmd(CMD_GOEASY, "Go easy on missing columns"),
	//        new Cmd(CMD_COMMENT, "",new Arg("comment")),
        new Cmd(CMD_VERIFY,
                "Verify that all of the rows have the same # of columns"),
        new Cmd(CMD_EXT,
                "Execute the external program",
		new Arg("program_id",
			"matches with seesv.ext.&lt;program_id&gt;=/path")),
        new Cmd(CMD_EXEC,
                "Execute the external program for every line",
		new Arg("program_id",
			"matches with seesv.ext.&lt;program_id&gt;=/path")),
        /*  Output   */
        new Category("Output"), 
	new Cmd(CMD_PRINT, "Print text output",ARG_LABEL,"Text Output"),
	new Cmd(CMD_PRINTDELIM, "Print with delimited output", ARG_LABEL, "Delimited Print",
		new Arg("delimiter","Delimiter - ,|^ etc. Use \"tab\" for tab")),	
        new Cmd(CMD_OUTPUT, "Write to the given file (command line only)",
		ARG_LABEL,"Write to file",
                new Arg("file", "The file")),
        new Cmd(CMD_COMMENT, "Add a comment to the output",
		new Arg("comment","The comment")),
        new Cmd(CMD_OUTPUTPREFIX, "Specify text to add to the beginning of the file",
		ARG_LABEL,"Output Prefix",
		new Arg("text","The text. Use '_nl_' to add a new line. Use '_bom_' to write out the byte order mark.")),	
        new Cmd(CMD_HIGHLIGHT, "Highlight the columns",
		new Arg(ARG_COLUMNS, "", ATTR_TYPE, TYPE_COLUMNS),
		new Arg("color", "Color",
			ATTR_TYPE, "enumeration","values","red,green,yellow,blue,purple,cyan")),
        new Cmd(CMD_BACKGROUND, "Background the columns",
		new Arg("color", "Color",
			ATTR_TYPE, "enumeration","values","red,green,yellow,blue,purple,cyan")),	
        new Cmd(CMD_PRINTHEADER, "Print header",
		ARG_LABEL,"Print Header"),
        new Cmd(CMD_RAW, "Print the file raw"),
        new Cmd(CMD_TABLE, "Print HTML table and stats"),
        new Cmd(CMD_COLS, "Set the width of the columns for output. Use with -p",
		new Arg("width","Column width")),		
        new Cmd(CMD_STATS, "Print summary stats",ARG_LABEL,"Print Stats"),
        new Cmd(CMD_TORECORD, "Print records",ARG_LABEL,"Print Records"),
	new Cmd(CMD_SCRIPT, "Generate script",ARG_LABEL,"Generate Script"),
        new Cmd(CMD_TOXML, "Generate XML",
		ARG_LABEL,"To XML",
		new Arg("outer tag"),
		new Arg("inner tag")),
        new Cmd(CMD_TOJSON, "Generate JSON",
		ARG_LABEL,"To JSON",
                new Arg("key index", "If defined use this as a map")),
        new Cmd(CMD_TOGEOJSON, "Generate GeoJSON",
		ARG_LABEL,"To GeoJSON",
                new Arg("latitude", "latitude column", ATTR_TYPE, TYPE_COLUMN),
                new Arg("longitude", "longitude column", ATTR_TYPE, TYPE_COLUMN),		
		new Arg(ARG_COLUMNS, "property columns - use * for all", ATTR_TYPE, TYPE_COLUMNS)),
        new Cmd(CMD_TOURL, "Generate DB publish urls",
		ARG_LABEL,"To Publish URLS"),
	new Cmd(CMD_TODB, "Write to Database",
		ARG_LABEL,"To Database",
		new Arg("db id",""),
		new Arg("table","table name"),
		new Arg(ARG_COLUMNS,"database columns"),		
		new Arg("properties","name value properties")),		
        new Cmd(CMD_TEMPLATE, "Apply the template to make the output",
		ARG_LABEL,"Apply Template",
                new Arg("prefix", "", ATTR_SIZE, "40"),
                new Arg("template", "Use ${column_name} or indices: ${0},${1}, etc for values", ATTR_ROWS, "6"),
		new Arg("row_delimiter", "Output between rows",	ATTR_SIZE, "40"),
		new Arg("suffix", "", ATTR_SIZE, "40")),
        new Cmd(CMD_SUBD, "Subdivide into different files",
		ARG_LABEL,"Subdivide",
		new Arg(ARG_COLUMNS,"columns to subdivide on",ATTR_TYPE,TYPE_COLUMNS),		
		new Arg("ranges","Comma separated ranges min1;max1;step1,min2;max2;step2"),
		new Arg("output_template","Output template - use ${ikey} or ${vkey}, e.g., grid${ikey}.csv")),		
        new Cmd(CMD_MAPTILES, "Tile the data on lat/lon",
		ARG_LABEL,"Map Tiles",
		new Arg(ARG_COLUMNS,"lat/lon columns to subdivide on",ATTR_TYPE,TYPE_COLUMNS),		
		new Arg("degrees","Degrees per tile. Defaults to 1"),
		new Arg("output_template","Output template - use ${ikey} or ${vkey}, e.g., tile${vkey}.csv. Defaults to a tile${vket}.csv")),		


        new Cmd(CMD_CHUNK, "Make a number of output files with a max number of rows",
		ARG_LABEL,"Chunk Output",
		new Arg("output_template","Output template - use ${number}, e.g., output${number}.csv. Defaults to a output${number}.csv"),
		new Arg("number","Number of rows in each file")),		

        new Cmd(CMD_ADDHEADER, "Add the RAMADDA point properties",
		ARG_LABEL,"Add Point Header",
                new Arg("properties", "name1 value1 ... nameN valueN<br>Set default: default.type double", ATTR_ROWS, "6")),
        new Cmd(CMD_DB, "Generate the RAMADDA db xml from the header. See <a class=ramadda-decor target=_help href=https://ramadda.org/repository/userguide/seesv.html#-db>Help</a>",
		ARG_LABEL,"RAMADDA Database XML",
		new Arg("properties",
			"Name value pairs:" +
			argnl("\t\t",
			      add("table.id &lt;new id&gt;", "table.name &lt;new name&gt;"),
			      add("table.cansearch false","table.canlist false"),
			      add("table.icon &lt;icon&gt;")+", e.g., /db/database.png",
			      add("&lt;column&gt;.id &lt;new id for column&gt;",
				  "&lt;column&gt;.label &lt;new label&gt;"),
			      add("&lt;column&gt;.type &lt;string|enumeration|double|int|date|latlon&gt;"),
			      add("&lt;column&gt;.format &lt;yyyy MM dd HH mm ss format for dates&gt;"),
			      add("&lt;column&gt;.canlist false &lt;column&gt;.cansearch false"),
			      add("db.install &lt;true|false&gt;")+"install the new db table",
			      add("db.droptable &lt;true|false&gt;")+" careful! this deletes any prior created dbs",
			      add("db.yesreallydroptable true") +" - this double checks"),
			ATTR_ROWS, "10")),
        new Cmd(CMD_DBPROPS, "Print to stdout props for db generation",
		ARG_LABEL,"Print DB Properties",
		new Arg("id pattern"),
		new Arg("suffix pattern")),
        new Cmd(CMD_WRITE, "Write the contents of a row to a named file",
		new Arg("file name template"),
		new Arg("contents template")),
        new Cmd(CMD_FIELDS, "Print the fields",ARG_LABEL,"Print Fields"),
        new Cmd(CMD_RUN, "", new Arg("Name of process directory"),ARG_LABEL,"Name Process Dir"),
        new Cmd(CMD_PROGRESS, "Show progress",
		ARG_LABEL,"Print Progress",
		new Arg("rows", "How often to print")),
        new Cmd(CMD_DEBUGROWS, "Debug # rows",
		ARG_LABEL,"Debug Rows",
		new Arg("rows", "# of rows")),	
        new Cmd(CMD_POINTHEADER, "Generate the RAMADDA point properties",
		ARG_LABEL,"Generate Point Properties"),
        new Cmd(CMD_ARGS, "Generate the CSV file commands",
		ARG_LABEL,"CSV File Commands"),
        new Cmd(CMD_TYPE_XML, "Generate the RAMADDA type xml",
		ARG_LABEL,"Generate type.xml",
		new Arg("type_id","Type ID, prefix with type_, no spaces, lowercase, e.g.type_point_mypointdata"),
		new Arg("type_label","Type Label - human readable label"),
		new Arg("database_columns","List of database columns, one per line<br>e.g:id,label,type,prop,value1,prop2,value1<br>Types can be:<br>string,int,double,enumeration,enumerationplus<br>list,latlon,latlonbox,url,date,datetime<br>If enumeration then define values with escaped commas - \\,:<pre>fruit,Fruit,enumeration,values,banana\\,apple\\,orange</pre>",
			ATTR_SIZE,"40",ATTR_TYPE,"list",ATTR_ROWS, "6","delimiter",";"))
	    };

    private String getValue(String s) {
        for (Enumeration keys = macros.keys(); keys.hasMoreElements(); ) {
	    String key =(String) keys.nextElement(); 
	    String value = macros.get(key);
	    s= s.replace("%" + key+"%",value);
        }
	return s;
    }


    public void printVersion() throws Exception {
	try {
	    String path = "/org/ramadda/util/seesv/build.properties";
	    InputStream inputStream = IOUtil.getInputStream(path, getClass());
	    if (inputStream == null) {
		System.err.println("SeeSV:  null properties: " + path);
		return;
	    }
	    Properties tmp = new Properties();
	    tmp.load(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
	    System.err.println("SeeSV: build date: "+ tmp.get("ramadda.build.date"));
	} catch(Exception exc) {
	    System.err.println("SeeSV: error:"+  exc);
	    exc.printStackTrace();
	}
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
    public void usage(String msg, boolean format, String match, String... args)
	throws Exception {
        boolean exact = false;
        boolean raw   = false;
        boolean json  = false;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-exact")) {
                exact = args[i + 1].equals("true");
            } else if (args[i].equals(CMD_RAW)) {
                raw = args[i + 1].equals("true");
            } else if (args[i].equals(CMD_JSON)) {
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
	    String cmd = c.getLine(format,format,json);
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
		//                cmd = cmd.replaceAll("_nl_", "\n").replaceAll("_tab_", "\n");
            }
            if (json) {
                if (cnt > 0) {
                    pw.println(",");
                }
                if (c.category) {
                    pw.println(JsonUtil.mapAndQuote(Utils.makeList("isCategory", "true",
								   "label",c.cmd)));

                } else {
                    String argList = "[]";
                    if (c.args != null) {
                        List tmp = new ArrayList();
                        for (Arg arg : c.args) {
                            List<String> attrs = new ArrayList<String>();
                            attrs.add("id");
                            attrs.add(JsonUtil.quote(arg.id));
                            attrs.add("description");
                            attrs.add(JsonUtil.quote(arg.desc));
                            if (arg.props != null) {
                                for (int i = 0; i < arg.props.length;   i += 2) {
                                    attrs.add(arg.props[i]);
				    String v = arg.props[i + 1];
				    if(v.startsWith("property:")) {
					v = (String)getProperty(v.substring("property:".length()));
				    }
				    if(v==null) v="";
				    attrs.add(JsonUtil.quote(v));
                                }
                            }
                            tmp.add(JsonUtil.map(attrs));

                        }
                        argList = JsonUtil.list(tmp);
                    }
                    pw.println(JsonUtil.map(Utils.makeList(
							   "command", JsonUtil.quote(c.cmd),
							   "label", (c.label != null)
							   ? JsonUtil.quote(c.label)
							   : "null", "args", argList, "description",
							   JsonUtil.quote(c.desc))));
                }
            } else {
                if (c.category) {
		    if(format)
			pw.print(Utils.ANSI_GREEN_BACKGROUND);
                    pw.print(c.cmd +": " +c.desc);
		    if(format)
			pw.print(Utils.ANSI_RESET);
		    pw.println("");
                } else {
		    cmd = cmd.replaceAll("<br>","\n");
                    pw.println(pad + cmd);
		    
                }
            }
            if (raw && cmd.startsWith(CMD_DB)) {
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
		if(open) sb.append("</ul><br class=seesv-hide>");
		open = true;
		String extra = IO.readContents("/org/ramadda/util/seesv/help/category_" + Utils.makeID(c.cmd).toLowerCase()+".html",(String) null);
		if(header.length()>0) header.append(" | ");
		header.append("<a href='#" + c.cmd +"'>" + c.cmd+"</a>");
		sb.append("<div class=seesv-hide><hr>");
		sb.append("<a name='" + c.cmd+"'></a>");
		sb.append("<b style='font-size:120%;'>" + c.cmd+"</b><br>\n");
		sb.append(c.desc);
		sb.append("</div>");
		if(extra!=null) {
		    sb.append("<div class=seesv-hide>");
		    sb.append(" ");
		    processHelpContents(sb, extra);
		    sb.append("</div>");
		}
		sb.append("${header" + headers.size()+"}");
	
		if(hb.length()>0) {
		    hb.append("</ul></div>");
		}
		hb = new StringBuilder("<div class=seesv-hide>Commands:<ul>");
		headers.add(hb);
		continue;
	    }
	    cnt++;
	    String path = "/org/ramadda/util/seesv/help/" + c.cmd.replace("-","")+".html";
	    String extra = IO.readContents(path,(String)null);
	    if(c.cmd.startsWith(CMD_HELP)) continue;
	    sb.append("<div class=seesv-item>\n");
	    sb.append("<a name='" + c.cmd+"'></a>");
	    hb.append("<li> <a href='#" + c.cmd +"'><i>" + c.cmd+"</i>: " + c.desc +"</a>");
	    sb.append("<div class=command> <i><a href='#" + c.cmd +"'>" + c.cmd+"</a></i> ");
	    for(Arg arg: c.args) {
		sb.append(" &lt;" +arg.id+"&gt; ");
	    }
	    sb.append("</div><div class=command-block>");
	    if(Utils.stringDefined(c.desc)) {
		sb.append(c.desc);
		sb.append("<br class=seesv-hide>\n");
	    }
	    if(c.args.size()>0) {
		sb.append("<ul>\n");
		for(Arg arg: c.args) {
		    sb.append("<li> <i>" +arg.id+"</i>:\n");
		    if(Utils.stringDefined(arg.desc)) {
			sb.append(arg.desc.replace("<xbr>"," ").replace("[","\\["));
		    }
		    for(int i=0;i<arg.props.length;i+=2) {
			if(arg.props[i].equals("values"))
			    sb.append(" values:" + arg.props[i+1]+" "); 
		    }
		    sb.append("<br>\n");
		}
		sb.append("</ul>\n");		
	    }
	    if(extra!=null) {
		processHelpContents(sb, extra);
		sb.append("<p>");
	    }
	    sb.append("</div>");
	    sb.append("</div>");	    

	}
	sb.append("</ul>\n");

        PrintWriter pw = new PrintWriter(getOutputStream());
	String intro = IO.readContents("/org/ramadda/util/seesv/help/intro.html","");
	intro = intro.replace("${header}",header.toString()+"<br>The RAMADDA SeeSV package provides " + cnt +" commands for manipulating CSV and other types of files");
	pw.println(intro);

	String html = sb.toString();
	//	html = html.replace("[","\\[");
	for(int i=0;i< headers.size(); i++) {
	    String s  = "<div class=header>" + headers.get(i).toString() +"</div>";
	    html = html.replace("${header" + i+"}", s);
	}
	pw.append(html);
	pw.append("</body>\n");
	pw.append("</html>\n");	
        pw.flush();
    }


    private void processHelpContents(StringBuilder sb, String help) throws Exception {
	if(help==null) return;
	List<String> dataLines = null;
	boolean inData = false;
	for(String line:Utils.split(help,"\n")) {
	    if(line.trim().startsWith("seesv:")) {
		line = line.substring("seesv:".length()).trim();
		line = line.replaceAll("<","&lt;").replaceAll(">","&gt;");
		if(!line.startsWith("seesv")) line = "seesv " + line;
		sb.append("<seesv>");
		sb.append(line);
		sb.append("</seesv>");
		continue;
	    }
	    if(line.startsWith("import:")) {
		line = line.substring("import:".length()).trim();
		if(!line.startsWith("/")) {
		    line = "/org/ramadda/util/seesv/help/" + line;
		}
		String include = IO.readContents(line,(String)null);
		if(include==null) throw new IllegalArgumentException("Bad import:" + line);
		include = include.replaceAll("<","&lt;").replaceAll(">","&gt;");
		sb.append(include);
		continue;
	    }
	    if(line.startsWith("data:")) {
		line = line.substring("data:".length()).trim();
		if(!line.startsWith("/")) {
		    line = "/org/ramadda/util/seesv/test/" + line;
		}
		String include = IO.readContents(line,(String)null);
		if(include==null) throw new IllegalArgumentException("Bad import:" + line);
		genHelpData(sb, Utils.split(include,"\n"));
		continue;
	    }

	    String _line = line.trim();
	    if(_line.equals("<data>")) {
		inData = true;
		dataLines = new ArrayList<String>();
		continue;
	    }
	    if(_line.equals("</data>")) {
		inData = false;
		genHelpData(sb,dataLines);
		continue;
	    }
	    if(inData) {
		dataLines.add(line);
		continue;
	    }
	    sb.append(line);
	    sb.append("\n");
	}
    }	

    private void genHelpData(StringBuilder sb, List<String> dataLines) {
	sb.append("<br>");
	sb.append("<table cellpadding=0 cellspacing=0 style=' border-collapse: collapse;'>");
	for(int i=0;i<dataLines.size();i++) {
	    String l = dataLines.get(i);
	    sb.append("<tr>");
	    List<String> cells = Utils.split(l,",",false,false);
	    String cellExtra = "";
	    String style = "padding:4px;border:1px solid #ccc;";
	    if(i==0) {
		cellExtra = " align=center ";
		style+="background:#F5F5F5;";
	    }
	    for(String cell:cells) {
		if(cell.trim().equals("")) cell ="&lt;blank&gt;";
		sb.append("<td style='" + style+"' " + cellExtra+">"+cell+"</td>");
	    }
	    sb.append("</tr>");
	    sb.append("\n");
	}
	sb.append("</table>");
	sb.append("<br>");

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
            usage("Bad argument count for:" + arg, false, arg, "-exact", "true");
            return false;
        }
        return true;
    }

    private interface CsvFunction {
	int method(TextReader ctx, List<String> args,int index); 

    }


    public static int SKIP_INDEX = -999;
    public static class CsvFunctionHolder {
	private Seesv seesv;
	private String  name;
	private int numargs;
	private CsvFunction func;
	CsvFunctionHolder(Seesv seesv, String name,int numargs,CsvFunction func) {
	    this.seesv = seesv;
	    this.name = name;
	    this.numargs = numargs;
	    this.func = func;
	}
	public int run(TextReader ctx, List<String> args, int index) throws Exception {
	    if ( !seesv.ensureArg(args, index, numargs)) {
		return -1;
	    }
	    return this.func.method(ctx,args,index);
	}
    }

    private Hashtable<String,CsvFunctionHolder> functions;

    private String getText(String v)  {
	try {
	    if (v.startsWith(PREFIX_FILE)) {
		v = readFile(v);
	    }
	    return v;
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
    }


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

	defineFunction(new String[]{CMD_SKIPROWS,"-skip"},1,(ctx,args,i) -> {
		ctx.setSkipRows(parseInt(args.get(++i)));
		return i;
	    });
	defineFunction(CMD_SKIPLINES,1,(ctx,args,i) -> {
		ctx.setSkipLines(parseInt(args.get(++i)));
		return i;
	    });	

	defineFunction(CMD_SKIPPATTERN,1,(ctx,args,i) -> {
		ctx.setSkipPattern(args.get(++i));
		return i;
	    });

	defineFunction("-pass",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Pass(ctx));
		return i;
	    });

	defineFunction(CMD_CHANGELINE,2,(ctx,args,i) -> {
		ctx.setChangeString(args.get(++i), args.get(++i));
		return i;
	    });

	defineFunction(CMD_IMAGE,2, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.ImageSearch(ctx, getCols(args.get(++i)), args.get(++i)));
		return i;
	    });
	defineFunction(CMD_APPENDROWS,3, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.RowAppender(parseInt(args.get(++i)),
							   parseInt(args.get(++i)),
							   args.get(++i)));
		return i;
	    });
	defineFunction(CMD_ROWS_FIRST,2, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.RowShuffler(ctx, true,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });
	defineFunction(CMD_ROWS_LAST,2, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.RowShuffler(ctx,false,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });		
	defineFunction(CMD_EMBED,1, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.Embed(ctx, args.get(++i)));
		return i;
	    });
	defineFunction(CMD_FETCH,3, (ctx,args,i) -> {
		ctx.addProcessor(new Converter.Fetch(ctx, args.get(++i), args.get(++i).equals("true"), args.get(++i)));
		return i;
	    });		
	defineFunction(CMD_COUNTUNIQUE,1, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.CountUnique(ctx, getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_DOWNLOAD,2, (ctx,args,i) -> {
		ctx.addProcessor(new Processor.Downloader(ctx, this, args.get(++i), args.get(++i)));
		return i;
	    });	

	defineFunction(CMD_FILENAMEPATTERN,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.FileNamePattern(ctx, args.get(++i),args.get(++i)));
		return i;
	    });



	defineFunction(new String[]{"-c",CMD_COLUMNS},1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnSelector(ctx, getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_COLUMNSBEFORE,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnsBefore(ctx, args.get(++i),getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_COLUMNSAFTER,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnsAfter(ctx, args.get(++i),getCols(args.get(++i))));
		return i;
	    });		
	defineFunction(CMD_FIRSTCOLUMNS,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnFirst(ctx, getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_LASTCOLUMNS,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnLast(ctx, getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_NOTCOLUMNS,1,(ctx,args,i) -> {
		List<String> cols = getCols(args.get(++i));
		ctx.addProcessor(new Converter.ColumnNotSelector(ctx, cols));
		return i;
	    });

	defineFunction(CMD_NUMBER,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Number(ctx));
		return i;
	    });

	defineFunction(CMD_LETTER,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Letter(ctx));
		return i;
	    });

	defineFunction(CMD_UUID,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.UUID(ctx));
		return i;
	    });


	defineFunction(CMD_FROMB64,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.B64Decode(ctx,getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_PARSEEMAIL,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ParseEmail(ctx,getCols(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_TOB64,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.B64Encode(ctx,getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_ROT13,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Rot13(ctx,getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_ENCRYPT,2,(ctx,args,i) -> {
		try {
		    ctx.addProcessor(new Converter.EncryptDecrypt(ctx,true,getCols(args.get(++i)),args.get(++i)));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
		return i;
	    });
	defineFunction(CMD_DECRYPT,2,(ctx,args,i) -> {
		try {
		    ctx.addProcessor(new Converter.EncryptDecrypt(ctx,false,getCols(args.get(++i)),args.get(++i)));
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
		return i;
	    });		    			

	defineFunction(CMD_START,1,(ctx,args,i) -> {
		ctx.setStartPattern(args.get(++i));
		//		ctx.addProcessor(new Filter.Start(ctx,args.get(++i)));
		return i;
	    });

	defineFunction(CMD_UNIQUE_HEADER,0, (ctx,args,i) -> {
		ctx.addProcessor(new Filter.UniqueHeader(ctx));
		ctx.setUniqueHeader(true);
		return i;
	    });	
	

	defineFunction(CMD_ENSURE_NUMERIC,1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.EnsureNumeric(ctx,getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_STOP,1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.Stop(ctx,args.get(++i)));
		return i;
	    });

	defineFunction(CMD_MIN,1,(ctx,args,i) -> {
		String s = args.get(++i).trim();
		ctx.addProcessor(new Filter.MinColumns(ctx, s.length()==0?-1:parseInt(s)));
		return i;
	    });

	defineFunction(CMD_MAX,1,(ctx,args,i) -> {
		String s = args.get(++i).trim();
		ctx.addProcessor(new Filter.MaxColumns(ctx, s.length()==0?-1:parseInt(s)));
		return i;
	    });

	defineFunction(CMD_DECIMATE,2,(ctx,args,i) -> {
		int start = parseInt(args.get(++i));
		int skip  = parseInt(args.get(++i));
		if (skip > 0) {
		    ctx.addProcessor(
				     new Filter.Decimate(ctx, start, skip));
		}
		return i;
	    });

	defineFunction(CMD_DB,1,(ctx,args,i) -> {
		Dictionary<String, String> props =  parseProps(args.get(++i));
		ctx.putProperty("installPlugin", ""+(Utils.equals(props.get("-db.install"),"true") || Utils.equals(props.get("db.install"),
														"true")));
		ctx.putProperty("db.droptable", ""+(Utils.equals(props.get("-db.droptable"), "true")
					      || Utils.equals(props.get("db.droptable"),
							      "true")));
		ctx.putProperty("db.yesreallydroptable", ""+(Utils.equals(props.get("-db.yesreallydroptable"), "true")
					      || Utils.equals(props.get("db.yesreallydroptable"),
							      "true")));		
		ctx.addProcessor(dbXml =  new Processor.DbXml(ctx,props));
		ctx.setMaxRows(30);
		return i;
	    });

	defineFunction(new String[]{CMD_DBPROPS},2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.DbProps(ctx,args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(new String[]{CMD_FIELDS},0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Fields(ctx));
		return i;
	    });
	

	defineFunction(CMD_IF,3, (ctx,args,i) -> {
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
		TextReader ifCtx = makeTextReader();
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

	defineFunction(new String[]{CMD_APPLY,CMD_PROC,CMD_EXPANDCOMMANDS},1, (ctx,args,i) -> {
		List<String> cols =  Utils.split(args.get(++i), ",", true, true);
		List<String> applyArgs = new ArrayList<String>();
		while(true) {
		    if(i>=args.size()) throw new RuntimeException("Unclosed -exand");
		    String a = args.get(++i);
		    if(a.equals("-endapply")) break;
		    if(a.equals("-endproc")) break;
		    if(a.equals("-endexpandcommands")) break;		    
		    applyArgs.add(a);
		}
		ctx.addProcessor(new Processor.Expand(this,ctx,cols,applyArgs));
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
	defineFunction(CMD_MAKEFIELDS,4,unfurlFunc);	

	defineFunction(new String[]{CMD_MELT,CMD_FURL},3,(ctx,args,i) -> {
		List<String> valueCols = getCols(args.get(++i));
		ctx.addProcessor(new RowCollector.Melter(ctx,
							 valueCols, args.get(++i), args.get(++i)));

		return i;
	    });

	defineFunction(CMD_BREAK,3,(ctx,args,i) -> {
		String       label1 = args.get(++i);
		String       label2 = args.get(++i);
		List<String> cols   = getCols(args.get(++i));
		ctx.addProcessor(new RowCollector.Breaker(ctx,label1, label2, cols));

		return i;
	    });

	defineFunction(CMD_SORTBY,3,(ctx,args,i) -> {
		List<String> cols= getCols(args.get(++i));
		String  dir = args.get(++i);
		if(!dir.equals("up") && !dir.equals("down") && !dir.equals(""))
		    throw new IllegalArgumentException("Bad -sortby direction:" + dir);
		ctx.addProcessor(new RowCollector.Sorter(ctx,cols,
							 dir.equals("up"),
							 args.get(++i))); 
		return i;
	    });

	defineFunction(CMD_SORT,1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Sorter(ctx,getCols(args.get(++i)), true));

		return i;
	    });

	defineFunction(CMD_DESCSORT,1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Sorter(ctx,getCols(args.get(++i)), false));
		return i;
	    });

	defineFunction(CMD_IFIN,3,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.IfIn(ctx, true, args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_IFNOTIN,3,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.IfIn(ctx, false,args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_IFMATCHESFILE,4,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.MatchesFile(ctx, true, args.get(++i),args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_IFNOTMATCHESFILE,4,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.MatchesFile(ctx, false, args.get(++i),args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	

	defineFunction(CMD_JOIN,5,(ctx,args,i) -> {
		List<String> keys1   = getCols(args.get(++i));
		List<String> values1 = getCols(args.get(++i));
		String       file    = args.get(++i);
		List<String> keys2   = getCols(args.get(++i));
		ctx.addProcessor(new Processor.Joiner(ctx,keys1, values1, file, keys2,args.get(++i)));
		return i;
	    });
	defineFunction(CMD_CROSS,1,(ctx,args,i) -> {
		String       file    = args.get(++i);
		ctx.addProcessor(new RowCollector.Crosser(ctx, file));
		return i;
	    });	

	defineFunction(CMD_FUZZYJOIN,6,(ctx,args,i) -> {
		int threshold = parseInt(args.get(++i));
		List<String> keys1   = getCols(args.get(++i));
		List<String> values1 = getCols(args.get(++i));
		String       file    = args.get(++i);
		List<String> keys2   = getCols(args.get(++i));
		ctx.addProcessor(new Processor.FuzzyJoiner(ctx, threshold, keys1, values1, file, keys2,args.get(++i)));
		return i;
	    });
	
	defineFunction(CMD_NORMAL,1,(ctx,args,i) -> {
		List<String> cols   = getCols(args.get(++i));
		ctx.addProcessor(new RowCollector.Normal(ctx, cols));
		return i;
	    });

	defineFunction(CMD_RANGES,4,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ranges(ctx, args.get(++i),args.get(++i), parseDouble(args.get(++i)), parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_SUM,3,(ctx,args,i) -> {
		List<String> keys   = getCols(args.get(++i));
		List<String> values = getCols(args.get(++i));
		List<String> extra  = getCols(args.get(++i));
		List<String> what = new ArrayList<String>();
		what.add("sum");
		ctx.addProcessor(new RowCollector.Summary(ctx,what,keys, values, extra));
		return i;
	    });
	defineFunction(CMD_SUMMARY,4,(ctx,args,i) -> {
		List<String> keys   = getCols(args.get(++i));
		List<String> values = getCols(args.get(++i));
		List<String> extra  = getCols(args.get(++i));
		List<String> what   = Utils.split(args.get(++i),",",true,true);
		ctx.addProcessor(new RowCollector.Summary(ctx, what,keys, values, extra));
		return i;
	    });
	defineFunction(CMD_PIVOT,4,(ctx,args,i) -> {
		List<String> keys   = getCols(args.get(++i));
		List<String> columns = getCols(args.get(++i));
		String value = args.get(++i);
		List<String> ops   = Utils.split(args.get(++i),",",true,true);
		ctx.addProcessor(new RowCollector.Pivot(ctx, keys, columns, value, ops));
		return i;
	    });		
	defineFunction(CMD_HISTOGRAM,4,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Histogram(ctx, args.get(++i),args.get(++i),getCols(args.get(++i)),args.get(++i)));
		return i;
	    });	

	defineFunction(new String[]{"-u",CMD_UNIQUE},2,(ctx,args,i) -> {
		List<String> toks = getCols(args.get(++i));
		ctx.addProcessor(new Filter.Unique(ctx, toks,args.get(++i)));
		return i;
	    });
	defineFunction(new String[]{CMD_SAMPLE},1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.Sample(ctx, parseDouble(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_DUPS,1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Dups(ctx,getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_VERIFY,0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Verifier(ctx));
		return i;
	    });
	defineFunction(CMD_EXT,1,(ctx,args,i) -> {
		if(!commandLine)
		    throw new IllegalArgumentException(CMD_EXT +  "is only available from command line");
		List<String> a = new ArrayList<String>();
		String id = args.get(++i);
		int j=i+1;
		for(;j<args.size();j++) {
		    String arg = args.get(j);
		    if(arg.equals("-")) break;
		    a.add(arg);
		}
		ctx.addProcessor(new Processor.Ext(this,ctx,id,a));
		return j;
	    });
	defineFunction(CMD_EXEC,1,(ctx,args,i) -> {
		if(!commandLine)
		    throw new IllegalArgumentException(CMD_EXEC + "is only available from command line");
		List<String> a = new ArrayList<String>();
		String id = args.get(++i);
		int j=i+1;
		for(;j<args.size();j++) {
		    String arg = args.get(j);
		    if(arg.equals("-")) break;
		    a.add(arg);
		}
		ctx.addProcessor(new Processor.Exec(this,ctx,id,a));
		return j;
	    });		

	defineFunction(CMD_COUNT,0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Counter(ctx));
		return i;
	    });


	defineFunction("-log",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Logger(ctx));
		return i;
	    });

	defineFunction(CMD_STRICT,0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Counter(ctx,true));
		return i;
	    });

	defineFunction(CMD_FLAG,0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Counter(ctx, true, true));
		return i;
	    });


	defineFunction(CMD_ROTATE,0,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Rotator(ctx));
		return i;
	    });

	defineFunction(CMD_FLIP,0,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Flipper(ctx));
		return i;
	    });

	defineFunction(CMD_RAWLINES,1,(ctx,args,i) -> {
		rawLines = parseInt(args.get(++i));
		return i;
	    });

	defineFunction(CMD_INPUTNOTCONTAINS,1,(ctx,args,i) -> {
		ctx.setLineFilters(Utils.split(args.get(++i)));
		return i;
	    });
	
	defineFunction(CMD_TAB,0,(ctx,args,i) -> {
		ctx.setDelimiter(delimiter = "tab");
		return i;
	    });

	defineFunction(CMD_DELIMITER,1,(ctx,args,i) -> {
		ctx.setDelimiter(delimiter = args.get(++i));
		return i;
	    });
	defineFunction(CMD_INPUTCOMMENT,1,(ctx,args,i) -> {
		ctx.setInputComment(args.get(++i));
		return i;
	    });	
	defineFunction(CMD_QUOTESNOTSPECIAL,0,(ctx,args,i) -> {
		ctx.setQuotesNotSpecial(true);
		return i;
	    });
	defineFunction(CMD_CLEANINPUT,0,(ctx,args,i) -> {
		ctx.setCleanInput(true);
		return i;
	    });
	defineFunction(CMD_BOM,0,(ctx,args,i) -> {
		inputIsBom = true;
		return i;
	    });
	defineFunction(CMD_ENCODING,1,(ctx,args,i) -> {
		encoding = args.get(++i);
		ctx.setEncoding(encoding);
		return i;
	    });				

	defineFunction(CMD_WIDTHS,1,(ctx,args,i) -> {
		List<Integer> widths = new ArrayList<Integer>();
		for (String tok : Utils.split(args.get(++i), ",", true,
					      true)) {
		    widths.add(parseInt(tok));
		}
		ctx.setWidths(widths);
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

	defineFunction(CMD_CUT,1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.RowCutter(ctx,Utils.getNumbers(args.get(++i)), true));
		return i;
	    });


	defineFunction(CMD_INCLUDE,1,(ctx,args,i) -> {
		ctx.addProcessor(new Filter.RowCutter(ctx, Utils.getNumbers(args.get(++i)), false));
		return i;
	    });


	
	defineFunction(CMD_GOEASY,0,(ctx,args,i) -> {
		ctx.putProperty("goeasy",true);
		return i;
	    });


	defineFunction(CMD_SHEET,1,(ctx,args,i) -> {
		sheetNumber = Integer.parseInt(args.get(++i));
		return i;
	    });

	defineFunction(CMD_PROP,2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Propper(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_ROWOP,3,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.RowOperator(getCols(args.get(++i)),getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	
	defineFunction(CMD_PERCENT,  1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnPercenter(getCols(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_AVERAGE,3,(ctx,args,i) -> {
		List<String> cols   = getCols(args.get(++i));
		int          period = parseInt(args.get(++i));
		String       label  = args.get(++i);
		ctx.addProcessor(
				 new Converter.ColumnAverage(
							     Converter.ColumnAverage.MA, cols, period, label));

		return i;
	    });

	defineFunction(CMD_INCREASE,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnIncrease(args.get(++i), parseInt(args.get(++i),1)));
		return i;
	    });
	defineFunction(CMD_DIFF,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnDiff(args.get(++i), parseInt(args.get(++i),1)));
		return i;
	    });	
	defineFunction(CMD_COLUMN_AND,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.And(args.get(++i), getCols(args.get(++i))));
		return i;
	    });
	defineFunction("-column_or",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Or(args.get(++i), getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_COLUMN_NOT,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Not(args.get(++i), args.get(++i)));
		return i;
	    });
	defineFunction(CMD_CHECK,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Checker(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });				


	defineFunction("-sumrow",0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnOperator());

		return i;
	    });

	defineFunction(CMD_PAD,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Padder(parseInt(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_PREFIX,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Prefixer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_SUFFIX,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Suffixer(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction(CMD_EXPLODE,1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Exploder(ctx, args.get(++i)));
		return i;
	    });
	defineFunction(CMD_DISSECT,2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Dissector(args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction(CMD_SCRAPE,3,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Scraper(args.get(++i), args.get(++i),args.get(++i)));
		return i;
	    });	

	defineFunction(CMD_KEYVALUE,1,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.KeyValue(args.get(++i)));
		return i;
	    });		

	defineFunction(CMD_FIRSTCHARS,3,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.First(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });
	defineFunction(CMD_LASTCHARS,3,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Last(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });			
	defineFunction(CMD_BETWEEN_INDICES,4,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Between(args.get(++i), args.get(++i),args.get(++i), args.get(++i)));
		return i;
	    });
	defineFunction(CMD_FROMHEADING,3,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.FromHeading(getCols(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });				


	defineFunction(CMD_GENDER,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Genderizer(args.get(++i)));
		return i;
	    });

	defineFunction("-ximage",2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ImageSearch(ctx,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_IMAGEFILL,3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ImageSearch(ctx,getCols(args.get(++i)), args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_WIKIDESC,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.WikiDescSearch(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_STATENAME,1,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.StateNamer(args.get(++i)));
		return i;
	    });
	defineFunction(CMD_GEONAME,4,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.GeoNamer(args.get(++i),args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction(CMD_DECODELATLON,1,(ctx,args,i) -> {	
		ctx.addProcessor(new Geo.DecodeLatLon(getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_GEOCONTAINS,4,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.GeoContains(args.get(++i),args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });	
	defineFunction(CMD_ELEVATION,2,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Elevation(args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction(CMD_NEIGHBORHOOD,3,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Neighborhood(args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });


	defineFunction(CMD_GEOCODE,3,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim()));
		return i;
	    });
	defineFunction(CMD_BOUNDS,4,(ctx,args,i) -> {
		ctx.setBounds(new Bounds(parseDouble(args.get(++i)),
					 parseDouble(args.get(++i)),
					 parseDouble(args.get(++i)),
					 parseDouble(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_GEOCODEIFNEEDED,5,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim(),args.get(++i),
						  args.get(++i)));
		return i;
	    });	

	defineFunction(CMD_GETADDRESS,2,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.GetAddress(args.get(++i), args.get(++i)));
		return i;
	    });	

	defineFunction(CMD_GEOCODEJOIN,5,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Geocoder(args.get(++i),args.get(++i), parseInt(args.get(++i)),
						  parseInt(args.get(++i)),
						  parseInt(args.get(++i)), false));
		return i;
	    });



	defineFunction(CMD_GEOCODEADDRESSDB,3,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Geocoder(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim(), true));
		return i;
	    });

	defineFunction("-geocodedb",5,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Geocoder(args.get(++i),args.get(++i), parseInt(args.get(++i)),
						  parseInt(args.get(++i)),
						  parseInt(args.get(++i)), true));
		return i;
	    });


	defineFunction(CMD_POPULATION,3,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Populator(getCols(args.get(++i)), args.get(++i).trim(),args.get(++i).trim()));
		return i;
	    });

	defineFunction(CMD_REGION,1,(ctx,args,i) -> {
		ctx.addProcessor(new Geo.Regionator(getCols(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_CROP,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Cropper(getCols(args.get(++i)), Utils.split(args.get(++i), ",", true, true)));
		return i;
	    });

	defineFunction(CMD_CHANGE,3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnChanger(ctx,getCols(args.get(++i)),args.get(++i),  args.get(++i)));
		return i;
	    });
	defineFunction(CMD_CLEANWHITESPACE,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.CleanWhitespace(getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_REPLACE,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnReplacer(ctx,getCols(args.get(++i)),args.get(++i)));
		return i;
	    });	


	defineFunction(CMD_ASCII,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ascii(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });


	defineFunction(CMD_ISMOBILE,1,(ctx,args,i) -> {
		if(!commandLine && !isVerifiedUser) {
		    throw new IllegalArgumentException("-ismobile command only available from command line or from logged in user");
		}
		ctx.addProcessor(new Converter(args.get(++i)) {
			int index;
			@Override
			public Row processRow(TextReader ctx, Row row) {
			    if(rowCnt++==0) {
				index = getIndex(ctx);
				row.add("ismobile");
				return row;
			    }
			    try {
				if(!row.indexOk(index)) {
				    row.add("false");
				} else {
				    boolean isMobile = PhoneUtils.isPhoneMobile(row.getString(index));
				    row.add(""+ isMobile);
				}
			    } catch(Exception exc) {
				fatal(ctx, "Error checking ismobile", exc);
			    }
			    return row;
			}
		    });
		return i;
	    });
	

	defineFunction(CMD_SMS,3,(ctx,args,i) -> {
		if(!commandLine && !isVerifiedUser) {
		    throw new IllegalArgumentException("-sms command only available from command line or from logged in user");
		}
		final String phoneColumn = args.get(++i);
		final String campaign = args.get(++i);
		final String message = getText(args.get(++i)).replace("\\n","\n");		
		ctx.addProcessor(new Converter(phoneColumn) {
			int numAlreadySent=0;
			int numUnsubscribed = 0;
			int numSent=0;
			int numFailed=0;			
			Row header;
			@Override
			public Row processRow(TextReader ctx, Row row) {
			    if(rowCnt++==0) {
				header = row;
				return row;
			    }
			    int idx = getIndex(ctx);
			    if(!row.indexOk(idx)) {
				return row;
			    }
			    String phone = row.getString(idx);
			    String template = message;
			    for (int i = 0; i < header.size(); i++) {
				String value = row.getString(i);
				String colName = (String) header.get(i);
				String colId   = Utils.makeID(colName, false);
				template = template.replace("{" + colId+"}",value).replace("{" + colName+"}",value).replace("{" + i+"}",value).replace("\\n","\n");
			    }
			    if(template.indexOf("{")>=0) {
				throw new IllegalArgumentException("There appears to be a macro in the message:" + template);
			    }
			    try {
				int code  = PhoneUtils.sendSMS(phone, template,true,campaign);
				if(code == PhoneUtils.SMS_CODE_ERROR) {
				    throw new RuntimeException("Failed to send message");
				}
				if(code == PhoneUtils.SMS_CODE_BADPHONE) {
				    System.err.println("bad phone:" + phone);
				} else if(code == PhoneUtils.SMS_CODE_ALREADYSENT) {
				    numAlreadySent++;
				    System.err.println("already sent:" + phone +" total:" + numAlreadySent);
				} else if(code == PhoneUtils.SMS_CODE_UNSUBSCRIBED) {
				    numUnsubscribed++;
				    System.err.println("unsubscribed:" + phone +" total unsubscribed:" + numUnsubscribed);
				} else {
				    numSent++;
				    System.err.println("sent:" + phone +" total:" + numSent);
				}
			    } catch(Exception exc) {
				throw new RuntimeException(exc);
			    }
			    return row;
			}
		    });
		return i;
	    });


	defineFunction(CMD_CLEANPHONE,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter(getCols(args.get(++i))) {
			@Override
			public Row processRow(TextReader ctx, Row row) {
			    if(rowCnt++==0) {
				return row;
			    }
			    for(int idx: getIndices(ctx)) {
				if(idx<row.size()) {
				    String v = row.getString(idx);
				    v = PhoneUtils.cleanPhone(v);
				    if(v.length()!=12) {
					v = "";
				    }
				    row.set(idx,v);
				}
			    }
			    return row;
			}
		    });
		return i;
	    });

	defineFunction(CMD_FORMATPHONE,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter(getCols(args.get(++i))) {
			@Override
			public Row processRow(TextReader ctx, Row row) {
			    if(rowCnt++==0) {
				return row;
			    }
			    for(int idx: getIndices(ctx)) {
				if(idx<row.size()) {
				    String v = row.getString(idx).trim();
				    v = PhoneUtils.formatPhone(v);
				    row.set(idx,v);
				}
			    }
			    return row;
			}
		    });
		return i;
	    });
	

	defineFunction(CMD_ENDSWITH,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnEndsWith(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });



	defineFunction(CMD_EXTRACT,4,(ctx,args,i) -> {
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


	defineFunction(CMD_EXTRACTHTML,3,(ctx,args,i) -> {
		String col = args.get(++i);
		List<String> names = Utils.split(args.get(++i),",",true,true);
 		String pattern = args.get(++i);
		ctx.addProcessor(
				 new Converter.HtmlExtracter(
							     col,names,pattern));

		return i;
	    });

	defineFunction(CMD_HTMLINFO,1,(ctx,args,i) -> {
		String col = args.get(++i);
		ctx.addProcessor(
				 new Converter.HtmlInfo(col));

		return i;
	    });	


	defineFunction(CMD_CHECKMISSING,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.CheckMissing(args.get(++i),args.get(++i)));
		return i;
	    });	
	


	defineFunction(CMD_URLARG,2,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Converter.UrlArg(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_URLENCODE,1,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Converter.UrlEncode(getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_XMLENCODE,1,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Converter.XmlEncode(getCols(args.get(++i))));
		return i;
	    });	
	defineFunction(CMD_URLDECODE,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.UrlDecode(getCols(args.get(++i))));
		return i;
	    });	
	


	defineFunction(CMD_TRUNCATE,3,(ctx,args,i) -> {
		int    col    = parseInt(args.get(++i));
		int    length = parseInt(args.get(++i));
		String suffix = args.get(++i);
		ctx.addProcessor(
				 new Converter.Truncater(col, length, suffix));

		return i;
	    });


	defineFunction(CMD_CHANGEROW,4,(ctx,args,i) -> {
		List<Integer> rows    = Utils.getNumbers(args.get(++i));
		List<String>  cols    = getCols(args.get(++i));
		String        pattern = args.get(++i);
		pattern = Utils.convertPattern(pattern);
		ctx.addProcessor(
				 new Converter.RowChanger(ctx,
							  rows, cols, pattern, args.get(++i)));

		return i;
	    });

	defineFunction(CMD_FORMATDATE, 1,(ctx,args,i) -> {
		ctx.addProcessor(new DateOps.DateFormatter(getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_ELAPSED, 1,(ctx,args,i) -> {
		ctx.addProcessor(new DateOps.Elapsed(args.get(++i)));
		return i;
	    });

	defineFunction(CMD_MSTO, 2,(ctx,args,i) -> {
		ctx.addProcessor(new DateOps.MsTo(args.get(++i),args.get(++i)));
		return i;
	    });	

	defineFunction(CMD_DATEDIFF, 3,(ctx,args,i) -> {
		ctx.addProcessor(new DateOps.Diff(args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });	


	defineFunction(CMD_DATEFORMAT, 2,(ctx,args,i) -> {
		outDater = inDater = new Dater(args.get(++i),args.get(++i));
		ctx.addProcessor(new DateOps.DateFormatSetter(true, inDater));
		ctx.addProcessor(new DateOps.DateFormatSetter(false, outDater));
		return i;
	    });


	defineFunction(new String[]{CMD_INDATEFORMATS,"-indateformat"}, 2,(ctx,args,i) -> {
		inDater = new Dater(args.get(++i),args.get(++i));
		ctx.addProcessor(new DateOps.DateFormatSetter(true, inDater));
		return i;
	    });

	defineFunction(new String[]{CMD_OUTDATEFORMAT}, 2,(ctx,args,i) -> {
		outDater = new Dater(args.get(++i),args.get(++i));
		ctx.addProcessor(new DateOps.DateFormatSetter(false, outDater));
		return i;
	    });	

	defineFunction(CMD_CONVERTDATE,1,(ctx,args,i) -> {
		ctx.addProcessor(new DateOps.DateConverter(args.get(++i)));
		return i;
	    });

	defineFunction(CMD_ADDDATE,3,(ctx,args,i) -> {
		String dateCol =args.get(++i);
		String valueCol =args.get(++i);
		String type =args.get(++i);				
		ctx.addProcessor(new DateOps.DateAdder(ctx, dateCol, valueCol, type));
		return i;
	    });

	defineFunction(CMD_CLEARDATE,2,(ctx,args,i) -> {
		String dateCol =args.get(++i);
		String type =args.get(++i);				
		ctx.addProcessor(new DateOps.DateClear(ctx, dateCol,  type));
		return i;
	    });	

	defineFunction(CMD_EXTRACTDATE,2,(ctx,args,i) -> {
		String col  = args.get(++i);
		String what = args.get(++i);
		ctx.addProcessor(new DateOps.DateExtracter(col, what));
		return i;
	    });

	defineFunction(CMD_FORMATDATEOFFSET,2,(ctx,args,i) -> {
		String col  = args.get(++i);
		String what = args.get(++i);
		ctx.addProcessor(new DateOps.FormatDateOffset(col, what));
		return i;
	    });



	defineFunction(CMD_BEFORE,2,(ctx,args,i) -> {
		try {
		    String    col  = args.get(++i);
		    String date = args.get(++i);
		    Date   dttm = null;
		    if (date.equals("now")) {
			dttm = new Date();
		    } else {
			SimpleDateFormat sdf = Utils.findDateFormat(date);
			dttm = sdf.parse(date);
		    }
		    ctx.addProcessor(new DateOps.DateBefore(col, dttm));
		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });

	defineFunction(CMD_AFTER,2,(ctx,args,i) -> {
		try {
		    String    col  = args.get(++i);
		    String date = args.get(++i);
		    Date   dttm = null;
		    if (date.equals("now")) {
			dttm = new Date();
		    } else {
			SimpleDateFormat sdf = Utils.findDateFormat(date);
			dttm = sdf.parse(date);
		    }
		    ctx.addProcessor(new DateOps.DateAfter(col,  dttm));
		    return i;
	       	} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}

	    });

	defineFunction(CMD_LATEST,3,(ctx,args,i) -> {
		List<String> cols = getCols(args.get(++i));
		String       col  = args.get(++i);
		String       sdf  = args.get(++i);
		ctx.addProcessor(
				 new RowCollector.DateLatest(ctx,
							     cols, col, Utils.makeDateFormat(sdf)));

		return i;
	    });






	defineFunction(new String[]{CMD_HTMLTABLE,"-html"},3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.HtmlDataProvider(args.get(++i), args.get(++i),
									 parseProps(args.get(++i))));

		return i;
	    });
	defineFunction(CMD_HTMLPATTERN,4,(ctx,args,i) -> {
		ctx.getProviders().add(
				       new DataProvider.HtmlPatternDataProvider(args.get(++i), args.get(++i),
										args.get(++i), args.get(++i)));

		return i;
	    });

	defineFunction(CMD_HARVEST,1,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Harvester( args.get(++i)));
		return i;
	    });


	defineFunction(CMD_TEXT,3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.TextDataProvider(args.get(++i), args.get(++i), args.get(++i)));

		return i;
	    });
	defineFunction("-text2",3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Pattern2DataProvider(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction(CMD_EXTRACTPATTERN,2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.PatternExtractDataProvider(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction("-text3",2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.PatternExtractDataProvider(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_TOKENIZE,2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.PatternDataProvider(Utils.split(args.get(++i), ","),
									    args.get(++i)));
		return i;
	    });

	defineFunction(CMD_JSON,2,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.JsonDataProvider(args.get(++i), args.get(++i)));

		return i;
	    });
	defineFunction(CMD_JSONJOIN,5,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.JsonJoinDataProvider(args.get(++i), args.get(++i),args.get(++i),args.get(++i),args.get(++i)));

		return i;
	    });	
	defineFunction(CMD_JSONVALUE,2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.JsonValue(getCols(args.get(++i)), 
							 args.get(++i)));

		return i;
	    });	

	defineFunction(CMD_GEOJSON,1,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.GeoJsonDataProvider(args.get(++i).equals("true")));

		return i;
	    });
	
	defineFunction(CMD_SQL,5,(ctx,args,i) -> {
		//-sql db table cols "col1 value col2 value"
		ctx.getProviders().add(new DataProvider.SqlDataProvider(args.get(++i),
									args.get(++i),
									args.get(++i),
									args.get(++i),																	
									parseProps(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_XML,1,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.XmlDataProvider(args.get(++i)));
		return i;
	    });
	defineFunction(CMD_LINES,0,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Lines());
		return i;
	    });
	defineFunction(CMD_SYNTHETIC,3,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Synthetic(args.get(++i),args.get(++i),parseInt(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_PDF,0,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.Pdf(this));
		return i;
	    });
	

	defineFunction(CMD_SHAPEFILE,1,(ctx,args,i) -> {
		makeInputStreamRaw = true;
		ctx.getProviders().add(new ShapefileProvider(parseProps(args.get(++i))));
		return i;
	    });
	
	defineFunction("-kml",0,(ctx,args,i) -> {
		ctx.getProviders().add(new DataProvider.KmlDataProvider());	
		return i;
	    });
	
	defineFunction(CMD_CHANGERAW,2,(ctx,args,i) -> {
		ctx.addChangeFromTo(args.get(++i),args.get(++i));
		return i;
	    });

	defineFunction(CMD_MAXROWS,1,(ctx,args,i) -> {
		ctx.setMaxRows(parseInt(args.get(++i)));
		return i;
	    });

	defineFunction(CMD_PRUNE,1,(ctx,args,i) -> {
		ctx.setPruneBytes(parseInt(args.get(++i)));
		return i;
	    });


	defineFunction(CMD_MERGEROWS,3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.RowMerger(Utils.getNumbers(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_PREPEND,1,(ctx,args,i) -> {
		String text = args.get(++i);
		text = text.replaceAll("_nl_", "\n");
		ctx.setPrepend(text);
		return i;
	    });

	defineFunction("-columndebug", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnDebugger(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	
	defineFunction(CMD_MAP, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnMapper(getCols(args.get(++i)), args.get(++i),
							    Utils.parseCommandLine(args.get(++i))));
		return i;
	    });

	

	defineFunction(CMD_SPLIT, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnSplitter(
							      args.get(++i), args.get(++i),
							      Utils.split(args.get(++i), ",")));
		return i;
	    });

	defineFunction(CMD_DELETE, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnDeleter(getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_ADD, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnAdder(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_INSERT, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnInserter(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_SHIFT, 3,(ctx,args,i) -> {
		List<Integer> rows  = Utils.getNumbers(args.get(++i));
		int           col   = parseInt(args.get(++i));
		int           count = parseInt(args.get(++i));
		ctx.addProcessor(new Converter.Shifter(rows, col, count));
		return i;
	    });

	defineFunction(CMD_SLICE, 3,(ctx,args,i) -> {
		List<String> cols  = getCols(args.get(++i));
		String dest = args.get(++i);
		List<String> fill = Utils.split(args.get(++i),",",false,false);
		ctx.addProcessor(new RowCollector.Slicer(ctx, cols, dest,fill));
		return i;
	    });
	

	defineFunction(CMD_GENERATE, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Generator(args.get(++i), parseDouble(args.get(++i)),
							 parseDouble(args.get(++i))));
		return i;
	    });



	defineFunction(CMD_MACRO, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnMacro(args.get(++i), args.get(++i), args.get(++i)));
		return i;
	    });


	defineFunction(CMD_FORMAT, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnFormatter(getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction(CMD_SCALE, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnScaler(getCols(args.get(++i)),
							    parseDouble(args.get(++i),0),
							    parseDouble(args.get(++i),1),
							    parseDouble(args.get(++i),1)));

		return i;
	    });

	defineFunction(CMD_MAKENUMBER, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.MakeNumber(getCols(args.get(++i))));
		return i;
	    });	


	defineFunction(CMD_DECIMALS, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Decimals(getCols(args.get(++i)), parseInt(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_FUZZ, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Fuzzer(getCols(args.get(++i)), parseInt(args.get(++i)),
						      parseInt(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_CEIL, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ceil(getCols(args.get(++i)), parseDouble(args.get(++i))));
		return i;
	    });
	defineFunction("-floow", 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Floor(getCols(args.get(++i)), parseDouble(args.get(++i))));
		return i;
	    });

	
	defineFunction(CMD_COPY, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnCopier(args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_CONCAT, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnNewer(getCols(args.get(++i)), args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_CONCATROWS, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.RowConcat(parseInt(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_ROLL, 1,(ctx,args,i) -> {
		List<String> cols  = getCols(args.get(++i));
		ctx.addProcessor(new Converter.Roller(ctx, cols));
		return i;
	    });


	defineFunction(CMD_SPLAT, 4,(ctx,args,i) -> {
		String key       = args.get(++i);
		String value     = args.get(++i);
		String delimiter = args.get(++i);
		String name      = args.get(++i);
		ctx.addProcessor(new RowCollector.Splatter(ctx, key, value, delimiter, name));
		return i;
	    });
	defineFunction(CMD_DELTA, 2,(ctx,args,i) -> {
		List<String> keyidxs = getCols(args.get(++i));
		List<String> idxs    = getCols(args.get(++i));
		ctx.addProcessor(new Converter.Delta(keyidxs, idxs));
		return i;
	    });
	defineFunction(CMD_RUNNINGSUM, 2,(ctx,args,i) -> {
		List<String> idxs    = getCols(args.get(++i));
		ctx.addProcessor(new Converter.RunningSum(idxs));
		return i;
	    });
	defineFunction(CMD_TRENDCOUNTER, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.TrendCounter(args.get(++i),args.get(++i)));
		return i;
	    });		



	defineFunction(CMD_OPERATOR, 3,(ctx,args,i) -> {
		List<String> idxs = getCols(args.get(++i));
		String       name = args.get(++i);
		String       op   = args.get(++i);
		Processor processor = new Converter.ColumnMathOperator(idxs, name, op);
		ctx.addProcessor(processor);
		return i;
	    });

	defineFunction(CMD_COMPARE, 3,(ctx,args,i) -> {
		Processor processor = new Converter.CompareNumber(args.get(++i),args.get(++i),args.get(++i));
		ctx.addProcessor(processor);
		return i;
	    });


	defineFunction(CMD_DATECOMPARE, 3,(ctx,args,i) -> {
		Processor processor = new DateOps.CompareDate(args.get(++i),args.get(++i),args.get(++i));
		ctx.addProcessor(processor);
		return i;
	    });
	

	defineFunction(CMD_JS, 1,(ctx,args,i) -> {
		js.append(args.get(++i));
		js.append("\n");
		return i;
	    });

	defineFunction(CMD_FUNC, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnFunc(ctx, js.toString(), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_MERCATOR, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Mercator(getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_ROUND, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnRounder(getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_BYTES, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Bytes(args.get(++i),getCols(args.get(++i))));
		return i;
	    });	
	defineFunction(CMD_ABS, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnAbs(getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_RAND, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnRand(args.get(++i), parseDouble(args.get(++i)),parseDouble(args.get(++i))));
		return i;
	    });		
	defineFunction(CMD_SUBST, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Subst(args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_MD, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.MD(ctx,getCols(args.get(++i)),args.get(++i)));
		return i;
	    });
	defineFunction(CMD_SOUNDEX, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.SoundexMaker(getCols(args.get(++i))));
		return i;
	    });	
	defineFunction(CMD_EVEN, 1,(ctx,args,i) -> {
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


	defineFunction(CMD_CASE, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Case(getCols(args.get(++i)),args.get(++i)));
		return i;
	    });
	defineFunction(CMD_TOID, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ToId(getCols(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_PADLEFT, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PadLeftRight(true,getCols(args.get(++i)),args.get(++i),parseInt(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_PADRIGHT, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PadLeftRight(false,getCols(args.get(++i)),args.get(++i),parseInt(args.get(++i))));
		return i;
	    });	


	defineFunction(CMD_NUMCOLUMNS, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.NumColumns(parseInt(args.get(++i))));
		return i;
	    });	



	defineFunction(CMD_TRIM, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Trim(getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_TRIMQUOTES, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.TrimQuotes(getCols(args.get(++i))));
		return i;
	    });	
	defineFunction(CMD_CLONE, 1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Cloner(parseInt(args.get(++i))));
		return i;
	    });
	
	defineFunction(CMD_ADDCELL, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnNudger(parseInt(args.get(++i)),parseInt(args.get(++i)), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_DELETECELL, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnUnNudger(parseInt(args.get(++i)), getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_COPYIF, 4,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.CopyIf(getCols(args.get(++i)),args.get(++i),args.get(++i),args.get(++i)));
		return i;
	    });
	defineFunction(CMD_COPYCOLUMNS, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.CopyColumns(getCols(args.get(++i)), getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_FILLDOWN, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.FillDown(getCols(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_FILLACROSS,2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.FillAcross(getCols(args.get(++i)),Utils.getNumbers(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_UNFILL, 1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Unfill(getCols(args.get(++i))));
		return i;
	    });	


	defineFunction(CMD_PRIORPREFIX, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PriorPrefixer(parseInt(args.get(++i)), args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_SET, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnSetter(getCols(args.get(++i)),getCols(args.get(++i)), args.get(++i)));
		return i;
	    });


	defineFunction(CMD_FAKER, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Faker(ctx,args.get(++i),getCols(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_MAKEIDS, 0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.MakeIds());
		return i;
	    });

	defineFunction(CMD_SETCOL, 4,(ctx,args,i) -> {
		String col1    = args.get(++i);
		String pattern = args.get(++i);
		String col2    = args.get(++i);
		String what    = args.get(++i);
		ctx.addProcessor(new Converter.ColumnPatternSetter(col1, pattern, col2, what));
		return i;
	    });

	defineFunction(CMD_WIDTH, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnWidth(getCols(args.get(++i)), parseInt(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_MERGE, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnMerger(getCols(args.get(++i)),args.get(++i),args.get(++i)));
		return i;
	    });



	defineFunction(CMD_COMBINE, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnConcatter(getCols(args.get(++i)),args.get(++i),args.get(++i),false));
		return i;
	    });


	defineFunction(CMD_COMBINEINPLACE, 3,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.ColumnConcatter(getCols(args.get(++i)),args.get(++i),args.get(++i),true));
		return i;
	    });

	defineFunction(CMD_DENORMALIZE, 6,(ctx,args,i) -> {
		String file = args.get(++i);
		int    col1 = parseInt(args.get(++i));
		int    col2 = parseInt(args.get(++i));
		String col3 = args.get(++i);
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

	defineFunction(CMD_ALIAS,2,(ctx,args,i) -> {
		ctx.putFieldAlias(args.get(++i),args.get(++i));
		return i;
	    });

	defineFunction("-and",0,(ctx,args,i) -> {
		ctx.setFilterToAddTo(new Filter.FilterGroup(ctx,true));
		ctx.addProcessor(ctx.getFilterToAddTo());
		return i;
	    });


	defineFunction(CMD_SAME, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(), new Filter.Same(ctx,args.get(++i), args.get(++i),false));
		return i;
	    });

	defineFunction(CMD_NOTSAME, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(), new Filter.Same(ctx,args.get(++i), args.get(++i),true));
		return i;
	    });		

	defineFunction(new String[]{CMD_MATCH,CMD_FIND,CMD_PATTERN}, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(), new Filter.PatternFilter(ctx,getCols(args.get(++i)), args.get(++i)));
		return i;
	    });
	defineFunction(new String[]{CMD_NOTMATCH,CMD_NOTPATTERN}, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(), new Filter.PatternFilter(ctx,getCols(args.get(++i)),args.get(++i), true));
		return i;
	    });

	defineFunction(CMD_FUZZYPATTERN, 3,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.FuzzyFilter(ctx,parseInt(args.get(++i)), getCols(args.get(++i)), args.get(++i),false));
		return i;
	    });

	defineFunction(CMD_LENGTHGREATER, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(), 
			      new Filter.Length(ctx,true,getCols(args.get(++i)),parseInt(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_COUNTVALUE, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(), new Filter.CountValue(ctx,args.get(++i), parseInt(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_GROUPFILTER, 4,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.GroupFilter(ctx, getCols(args.get(++i)), parseInt(args.get(++i)),
							      SeesvOperator.getOperator(args.get(++i)),
							      args.get(++i)));
		return i;
	    });


	defineFunction(CMD_EQ, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,getCols(args.get(++i)),
						     Filter.ValueFilter.OP_EQUALS,
						     parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_NE, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,getCols(args.get(++i)),
						     Filter.ValueFilter.OP_NOTEQUALS,
						     parseDouble(args.get(++i))));
		return i;
	    });


	defineFunction(CMD_LT, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,
						     getCols(args.get(++i)), Filter.ValueFilter.OP_LT,
						     parseDouble(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_GT, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,
						     getCols(args.get(++i)), Filter.ValueFilter.OP_GT,
						     parseDouble(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_GE, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,
						     getCols(args.get(++i)), Filter.ValueFilter.OP_GE,
						     parseDouble(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_HAS, 1,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.Has(ctx,
					     getCols(args.get(++i))));
		return i;
	    });
	defineFunction(CMD_IFNUMCOLUMNS, 2,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			     new Filter.IfNumColumns(ctx,args.get(++i), Integer.parseInt(args.get(++i))));
		return i;
	    });		


	defineFunction(CMD_BETWEENSTRING, 3,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.BetweenString(ctx,false,
						       args.get(++i), 
						       args.get(++i),
						       args.get(++i)));
		return i;
	    });

	defineFunction(CMD_NOTBETWEENSTRING, 3,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.BetweenString(ctx,true,
						       args.get(++i), 
						       args.get(++i),
						       args.get(++i)));
		return i;
	    });

	defineFunction(CMD_BETWEEN, 3,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.RangeFilter(ctx,true,
						     getCols(args.get(++i)), 
						     parseDouble(args.get(++i)),
						     parseDouble(args.get(++i))));						     
		return i;
	    });

	defineFunction(CMD_NOTBETWEEN, 3,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.RangeFilter(ctx,false,
						     getCols(args.get(++i)), 
						     parseDouble(args.get(++i)),
						     parseDouble(args.get(++i))));						     
		return i;
	    });
	


	defineFunction("-defined", 1,(ctx,args,i) -> {
		handleFilter(ctx, ctx.getFilterToAddTo(),
			      new Filter.ValueFilter(ctx,getCols(args.get(++i)),Filter.ValueFilter.OP_DEFINED, 0));
		return i;
	    });
	defineFunction(CMD_MAXVALUE, 2,(ctx,args,i) -> {
		String key   = args.get(++i);
		String value = args.get(++i);
		ctx.addProcessor(
				 new RowCollector.MaxValue(ctx, key, value));

		return i;
	    });
	defineFunction(CMD_MINVALUE, 2,(ctx,args,i) -> {
		String key   = args.get(++i);
		String value = args.get(++i);
		ctx.addProcessor(
				 new RowCollector.MinValue(ctx, key, value));

		return i;
	    });
	

	defineFunction("-quit",0,(ctx,args,i) -> {
		String last = args.get(args.size() - 1);
		if (last.equals(CMD_PRINT) || last.equals("-p")) {
		    ctx.addProcessor(
				     new Processor.Printer(ctx.getPrintFields(), false,","));
		} else if (last.equals(CMD_TABLE)) {
		    ctx.addProcessor(new RowCollector.Html(ctx));
		}
		return -1;
	    });

	defineFunction(new String[]{CMD_PROGRESS,"-dots"},1,(ctx,args,i) -> {
		String cnt = args.get(++i);
		String prefix ="";
		if(cnt.startsWith("tab")) {
		    prefix = "\t";
		    cnt = cnt.substring("tab".length());
		}
		ctx.addProcessor(new Processor.Progress(prefix,parseInt(cnt)));
		return i;
	    });

	defineFunction(CMD_DEBUGROWS,1,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.DebugRows(parseInt(args.get(++i))));
		return i;
	    });
	

	defineFunction(CMD_HEADERNAMES,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.HeaderNames());
		return i;
	    });

	defineFunction(CMD_HEADERIDS,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.HeaderIds());
		return i;
	    });
	
	defineFunction(CMD_IDS,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Ids());
		return i;
	    });	


	defineFunction(CMD_ADDHEADER,1,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.HeaderMaker(this,parseProps(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_NOHEADER,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.NoHeader());
		return i;
	    });	

	defineFunction(CMD_DEHEADER,0,(ctx,args,i) -> {
		ctx.putProperty("deheader","true");
		return i;
	    });
 

	defineFunction("-output",1,(ctx,args,i) -> {
		try {
		    String out = args.get(++i);
		    boolean zip = out.endsWith(".zip");
		    this.outputStream = makeOutputStream(out);
		    if(zip) {
			ZipOutputStream zos = new ZipOutputStream(this.outputStream);
			String f = new File(out).getName();
			//Assume its a csv
			if(f.indexOf(",")<0) f = f+".csv";
			zos.putNextEntry(new ZipEntry(f.replace(".zip","")));
			this.outputStream = zos;
		    }
		    ctx.setWriter(new PrintWriter(this.outputStream));
		    //		    ctx.addProcessor(new Processor.Printer(ctx.getPrintFields(), false));
		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });


	defineFunction(CMD_TOXML,2,(ctx,args,i) -> {
		hasSink = true;
		ctx.addProcessor(new DataSink.ToXml(args.get(++i),args.get(++i)));
		return i;
	    });

	defineFunction(CMD_TOGEOJSON,3,(ctx,args,i) -> {
		hasSink = true;
		ctx.addProcessor(new DataSink.ToGeojson(args.get(++i),
							args.get(++i),
							getCols(args.get(++i))));
		return i;
	    });	

	defineFunction(CMD_TODB,4,(ctx,args,i) -> {
		//		hasSink = true;
		ctx.addProcessor(new DataSink.ToDb(this, args.get(++i), args.get(++i),args.get(++i),parseProps(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_TOURL,0,(ctx,args,i) -> {
		ctx.addProcessor(new DataSink.ToUrl());
		return i;
	    });
	


	defineFunction(CMD_TOJSON,1,(ctx,args,i) -> {
		hasSink = true;
		ctx.addProcessor(new DataSink.ToJson(args.get(++i)));
		return i;
	    });

	defineFunction(CMD_WRITE,2,(ctx,args,i) -> {
		ctx.addProcessor(new DataSink.Write(this,args.get(++i),args.get(++i)));
		return i;
	    });
	
	


	defineFunction(CMD_TABLE,0, (ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.Html(ctx));
		return i;
	    });

	defineFunction(CMD_COLS,1, (ctx,args,i) -> {
		ctx.addProcessor(new Processor.Cols(parseInt(args.get((++i)))));
		return i;
	    });	


	defineFunction("-dump",0,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Processor.Printer(ctx.getPrintFields(), false,","));
		return i;
	    });

	defineFunction(new String[]{CMD_TORECORD,"-record"},0,(ctx,args,i) -> {
		ctx.addProcessor(
				 new Processor.Prettifier());
		return i;
	    });

	defineFunction(CMD_COMMENT,1,(ctx,args,i) -> {
		ctx.addComment(args.get(++i));
		return i;
	    });	

	defineFunction(CMD_OUTPUTPREFIX,1,(ctx,args,i) -> {
		ctx.setOutputPrefix(args.get(++i));
		return i;
	    });	
	

	defineFunction(CMD_OUTPUT,1,(ctx,args,i) -> {
		String file  = args.get(++i);
		if(inputFiles!=null && inputFiles.size()>0) {
		    File tmp = new File(inputFiles.get(0).getPath());
		    file = file.replace("${name}", IOUtil.stripExtension(tmp.getName()));
		}
		if(!commandLine) throw new IllegalArgumentException(CMD_OUTPUT+" only enabled for command line usage");

		try {
		    checkOkToWrite(file);
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
		setOutputFile(new File(file));
		ctx.setOutputFile(new File(file));
		return i;
	    });	


	defineFunction(CMD_EDIT,1,(ctx,args,i) -> {
		if(!commandLine) throw new IllegalArgumentException(CMD_EDIT+" only enabled for command line usage");
		ctx.addProcessor(new Converter.Editor(args.get(++i)));
		return i;
	    });	


	defineFunction(new String[]{"-tocsv",CMD_PRINT,"-p",CMD_PRINTDELIM}, 0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		if (ctx.getProperty("seenPrint")!=null) {
		    return i;
		}
		ctx.putProperty("seenPrint","true");
		ctx.addProcessor(new Processor.Printer(ctx.getPrintFields(), false,","));
		return i;
	    });

	defineFunction(CMD_STATS,0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		ctx.addProcessor(new RowCollector.Stats(ctx, this,true));
		return i;
	    });

	defineFunction("-mathstats",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.MathStats());
		return i;
	    });	

	defineFunction("-doit",0,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Doit());
		return i;
	    });	

	defineFunction(CMD_TABLE,0,(ctx,args,i) -> {
		if(hasSink) return SKIP_INDEX;
		ctx.addProcessor(new RowCollector.Stats(ctx, this,false));
		return i;
	    });	
	


	defineFunction(CMD_CHUNK, 2,(ctx,args,i) -> {
		ctx.addProcessor(new Processor.Chunker(this, args.get(++i), parseInt(args.get(++i))));
		return i;
	    });

	defineFunction(CMD_SUBD,3,(ctx,args,i) -> {	
	ctx.addProcessor(new Processor.Subd(this,getCols(args.get(++i)),
						    args.get(++i), args.get(++i)));
		return i;
	    });

	defineFunction(CMD_MAPTILES,3,(ctx,args,i) -> {
		List<String> cols = getCols(Utils.getDefinedString(args.get(++i),"latitude,longitude"));
		double degrees  = parseDouble(Utils.getDefinedString(args.get(++i),"1.0"));
		String range = "-90;90;" +(int)(180/degrees)+ ",-180;180;" + (int)(360/degrees);
		ctx.addProcessor(new Processor.Subd(this,cols,
						    range,
						    Utils.getDefinedString(args.get(++i), "tile${vkey}.csv")));
		
		return i;
	    });	


	defineFunction(CMD_TEMPLATE,4,(ctx,args,i) -> {
		try {
		    String prefix   = getText(args.get(++i).replaceAll("_nl_", "\n"));
		    String template = getText(args.get(++i).replaceAll("_nl_", "\n"));
		    String delim    = args.get(++i).replaceAll("_nl_", "\n");
		    String suffix   = getText(args.get(++i).replaceAll("_nl_", "\n"));
		    ctx.addProcessor(
				     new Processor.Printer(prefix, template, delim, suffix));

		    return i;
		} catch(Exception exc) {
		    throw new RuntimeException(exc);
		}
	    });



	defineFunction(new String[]{"-hl",CMD_HIGHLIGHT},2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Highlighter(
							   getCols(args.get(++i)),
							   args.get(++i)));
		return i;
	    });
	defineFunction(new String[]{"-bg",CMD_BACKGROUND},2,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.Backgrounder(
							   getCols(args.get(++i)),
							   args.get(++i)));
		return i;
	    });	

	defineFunction(new String[]{CMD_PRINTHEADER,"-ph"},0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PrintHeader());
		return i;
	    });

	
	defineFunction(CMD_POINTHEADER,0,(ctx,args,i) -> {
		ctx.addProcessor(new Converter.PrintHeader(true));
		return i;
	    });



	defineFunction("-tcl", 1,(ctx,args,i) -> {
		ctx.addProcessor(new RowCollector.TclWrapper(ctx, args.get(++i)));
		return i;
	    });



	defineFunction("-dummy",0,(ctx,args,i) -> {return i;});

    }

    public CsvFunctionHolder getFunction(String id) {
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
    public boolean parseArgs(List<String> args, TextReader ctx,    final List<IO.Path> files)
	throws Exception {
	this.inputFiles = files;
	boolean            addFiles      = files.size() == 0;
	Filter.FilterGroup subFilter     = null;
	boolean            doArgs        = false;
	boolean            doArgs2       = false;
	int                doArgsCnt     = 0;
	int                doArgsIndex   = 1;
	if (delimiter != null) {
	    ctx.setDelimiter(delimiter);
	}

	PrintWriter pw        = null;
	if (debugArgs) {
	    System.err.println("ParseArgs:" + args);
	}
	List<String> filePatterns = new ArrayList<String>();
	List<String> filePatternNames = new ArrayList<String>();	
	List<String> newArgs = new ArrayList<String>();
	for (int i = 0; i < args.size(); i++) {
	    String arg = args.get(i);

	    if(arg.equals("-ignore")) {
		int cnt = parseInt(args.get(++i));
		i+=cnt;
		continue;
	    }
	    if(arg.equals(CMD_VALUE)) {
		macros.put(args.get(++i),args.get(++i));
		continue;
	    }
	    if(arg.equals(CMD_FILEPATTERN)) {
		filePatternNames.add(args.get(++i));		
		filePatterns.add(args.get(++i));

		continue;
	    }
	    if(arg.equals("-commands")) {
		String file = args.get(++i);
		checkOkToRead(file);
		String contents =  IO.readContents(file);
		List<List<String>> llines  =  tokenizeCommands(contents,false);
		for(List<String>lines:llines) {
		    for(String s:lines) {
			newArgs.add(s.toString());
		    }
		}
		continue;
	    }
	    newArgs.add(arg);
	}


	if(filePatternNames.size()>0) {
	    List<IO.Path> tmpFiles = new ArrayList<IO.Path>();
	    if(files.size()>0) {
		for(IO.Path arg: files) {
		    tmpFiles.add(arg);
		}
	    } else  {
		for (int i = newArgs.size()-1; i>=0;i--) {
		    String arg = newArgs.get(i);
		    File  f = new File(arg);
		    if(f.exists()) {
			tmpFiles.add(new IO.Path(f.getName()));
			break;
		    }
		}
	    }
	    for(IO.Path file: tmpFiles) {
		for(int i=0;i<filePatterns.size();i++) {
		    String value = StringUtil.findPattern(file.getPath(), ".*"+filePatterns.get(i));
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
	//	debugArgs = true;


	for (int i = 0; i < args.size(); i++) {
	    String arg = args.get(i).trim();
	    if (debugArgs) {
		System.err.println("\targ[" + i + "]=" + arg);
	    }
	    currentArg = arg;
	    try {
		if (doArgs) {
		    //Don't include maxrows or print commands
		    if(arg.equals("-print")) {
			continue;
		    }
		    if(arg.equals("-maxrows")) {
			i++;
			continue;
		    }
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
		    if ( !arg.equals(CMD_TABLE)) {
			arg = arg.replaceAll("\"", "\\\\\"");
			pw.print("\"" + arg + "\",");
		    }
		    continue;
		}

		if(extCommands!=null) {
		    boolean gotOne = false;
		    for(ExtCommand command:extCommands) {
			if(command.canHandle(this,arg)) {
			    ExtCommand other = command.cloneMe();
			    i = other.processArgs(this,args,i);
			    ctx.addProcessor(other);
			    gotOne = true;
			    if(other.isSink()) {
				hasSink = true;
			    }
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


		if (arg.equals(CMD_ARGS)) {
		    doArgs = true;
		    continue;
		}
		if (arg.equals("-args2")) {
		    doArgs2 = true;
		    continue;
		}
		if (arg.equals(CMD_SCRIPT)) {
		    outputScript(args, ctx);
		    return false;
		}		
		if (arg.equals("-debuginput")) {
		    ctx.setDebugInput(true);
		    continue;
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
		    System.err.println("Seesv args:" + this.args);
		    continue;
		}

		if (arg.startsWith("-") ||arg.length() == 0) {
		    throw new IllegalArgumentException("Unknown argument: args[" + i+"]=" +
						       arg+"  args:" + Utils.wrap(args,"a[${index}]=",", "));
		}


		if (addFiles) {
		    if (debugFiles) {
			System.err.println("adding file:" + arg);
		    }
		    files.add(new IO.Path(arg));
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
	pw.println("#the SEESV environment variable needs to point to the directory holding the seesv.sh script in RAMADDA's SeeSV  release");
	pw.println("#");
	pw.print("sh ${SEESV}/seesv.sh ");	
	boolean seenPrint = false;
	for (String arg: args) {
	    if(arg.equals(CMD_SCRIPT)) continue;
	    if(arg.equals(CMD_PRINT)) seenPrint = true;
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
    private void handleFilter(TextReader ctx,
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
    public  Dictionary<String, String> parseProps(String s) {
	if(s.startsWith(PREFIX_FILE)) {
	    File file = new File(s.substring(PREFIX_FILE.length()));
	    checkOkToRead(file.toString());
	    Properties tmp = new Properties();
	    try {
		String contents =  IO.readContents(file);
		String parent = file.getParent();
		if(parent!=null)
		    contents = contents.replace("${directory}",file.getParent());
		InputStream fis= new ByteArrayInputStream(contents.getBytes());
		tmp.load(new InputStreamReader(fis, Charset.forName("UTF-8")));
		fis.close();
	    } catch(Exception exc) {
		throw new RuntimeException(exc);
	    }		
	    Dictionary dict = tmp;
	    return (Dictionary<String,String>)dict;
	}

	//Remove comment lines
	s=s.replaceAll("(?m)^ *//.*$","");
	s = s.replaceAll("_quote_", "\"");
	s = s.replaceAll("\n", " ");


	List<String> toks = Utils.parseCommandLine(s);
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
						   "Error: Odd number of arguments:\n" + err +" string:" + s);
	    }
	    //	    System.err.println(toks.get(j)+"="+ toks.get(j + 1));
	    props.put(toks.get(j), toks.get(j + 1));
	    //	    System.err.println(toks.get(j) +" " + toks.get(j+1));

	}
	return props;
    }


    public static int parseInt(String s,int...dflt) {
	s = s.trim();
	try {
	    return Integer.parseInt(s.trim());
	} catch(NumberFormatException nfe) {
	    if(dflt.length>0) return dflt[0];
	    throw nfe;
	}
    }

    public static double parseDouble(String s, double ...dflt) {
	try {
	    s = s.trim();
	    if(s.length()==0) return Double.NaN;
	    if(s.equals("null")) return Double.NaN;	    
	    if(s.startsWith("random:")) {
		List<String> toks = Utils.split(s,":",true,true);
		if(toks.size()==1) {
		    return Math.random();
		}
		double min = Double.parseDouble(toks.get(1));
		double max = toks.size()==2?min+1:Double.parseDouble(toks.get(2));		
		double v = Math.random();
		v = min+v*(max-min);
		return v;
	    }
	    return Double.parseDouble(s.trim());
	} catch(NumberFormatException nfe) {
	    if(dflt.length>0)
		return dflt[0];
	    throw new RuntimeException("Error parsing:" +s);
	}
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



    public static List<List<String>> tokenizeCommands(String commandString, boolean keepLineSeparation) {
	List<StringBuilder> toks =
	    Seesv.tokenizeCommands(commandString);

	//      System.err.println("TOKS:" + toks);
	List<List<String>> llines  = new ArrayList<List<String>>();
	List<String>       current = null;
	for (StringBuilder sb : toks) {
	    String s = sb.toString();
	    if (s.equals(Utils.MULTILINE_END)) {
		if (keepLineSeparation) {
		    current = null;
		}
		continue;
	    }
	    if (current == null) {
		current = new ArrayList<String>();
		llines.add(current);
	    }
	    current.add(s);
	}
	if (llines.size() == 0) {
	    List<String> l = new ArrayList<String>();
	    llines.add(l);
	}
	return llines;
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
	if(args.length>0 && args[0].equals("-s3")) {
	    S3File.main(args);
	    return;
	}
	Seesv.setTmpCacheDir(new File("."));
	IO.setCacheDir(new File("."));
	try {
	    Seesv seesv = new Seesv(args);
	    seesv.commandLine  = true;
	    seesv.setSeesvContext(new SeesvContext() {
		    public List<Class> getClasses() {
			String _name=null;
			ArrayList<Class> classes = new ArrayList<Class>();
			String prop = System.getenv("SEESV_CLASSES");
			if(prop!=null) {
			    for(String name: Utils.split(prop,":",true,true)) {
				_name = name;
				try {
				    Class c  = Class.forName(name);
				    classes.add(c);
				} catch(Exception exc) {
				    if(_name!=null) {
					//				    System.err.println("Error reading class:" + _name +" error:" + exc);
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

	    seesv.run(null);
	} catch(SeesvException cexc) {
	    System.err.println(cexc.getFullMessage());
	    System.exit(1);
	} catch(Exception exc) {
	    Throwable inner = LogUtil.getInnerException(exc);
	    System.err.println(exc.getMessage());
	    inner.printStackTrace();
	    System.exit(1);
	}
	Utils.exitTest(0);
    }

    public static class Dater {
	private boolean hadError = false;
	private static String DFLT_DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final SimpleDateFormat sdf =  Utils.makeDateFormat(DFLT_DATEFORMAT);
	private String sdfString = DFLT_DATEFORMAT;
	private List<SimpleDateFormat> sdfs = new ArrayList<SimpleDateFormat>();
	private String timezone="UTC";

	public Dater() {
	}

	public Dater(String fmt, String tz) {
	    setFormat(fmt,tz);
	}


	public void setFormat(String fmt, String timezone) {
	    if(!Utils.stringDefined(fmt)) fmt = DFLT_DATEFORMAT;
	    sdfString = fmt;
	    if(Utils.stringDefined(timezone)) {
		this.timezone = timezone;
	    }
	    sdfs = new ArrayList<SimpleDateFormat>();
	    for(String format: Utils.split(fmt,";",true,true)){
		SimpleDateFormat sdf = Utils.makeDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone(this.timezone));
		sdfs.add(sdf);
	    }

	}
	
	public Date parseDate(String d) {
	    if(!Utils.stringDefined(d)) return null;
	    if(sdfs.size()==0) {
		return Utils.parseDate(d);
	    }

	    Exception lastException=null;
	    for(SimpleDateFormat sdf:sdfs) {
		try {
		    return sdf.parse(d);
		} catch (Exception exc) {
		    lastException = exc;
		}
            } 
	    if(!hadError) {
		SimpleDateFormat sdf = Utils.findDateFormat(d);
		hadError=true;
		try {
		    Date date = sdf.parse(d);
		    sdfs.add(0,sdf);
		    return date;
		} catch (Exception exc) {
		    lastException = exc;
		}
	    }
	    throw new SeesvException("Could not parse date:" + d + " with format:"
				     + sdfString);
	}

	public String formatDate(Date d) {
	    if(sdfs.size()==0) {
		synchronized(sdf) {
		    return sdf.format(d);
		}
	    }
	    return sdfs.get(0).format(d);
	}
	
	public TimeZone getTimeZone() {
	    return TimeZone.getTimeZone(timezone);
	}

	public String toString() {
	    return "fmt:" + sdfString;
	}
    }

}
